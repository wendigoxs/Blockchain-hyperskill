package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public interface RejectTransactionStrategy {

    /**
     * Remove transactions that overflows payer balance
     *
     * @param transactions   list of Transactions
     * @param payersBalances payers balances map
     */
    void reject(List<Transaction> transactions, Map<CryptoCurrencyHolder, BigDecimal> payersBalances);

    static RejectTransactionStrategy rejectLast() {
        return (list, payersBalances) -> rejectGeneral(list, payersBalances, Comparator.comparing(Transaction::getId));
    }

    static RejectTransactionStrategy rejectSmallest() {
        return (list, payersBalances) -> rejectGeneral(list, payersBalances, Comparator.comparing(Transaction::getAmount).reversed());
    }

    static RejectTransactionStrategy rejectLargest() {
        return (list, payersBalances) -> rejectGeneral(list, payersBalances, Comparator.comparing(Transaction::getAmount));
    }

    private static void rejectGeneral(List<Transaction> list, Map<CryptoCurrencyHolder, BigDecimal> payerBalances, Comparator<Transaction> comparator) {
        Map<CryptoCurrencyHolder, List<Transaction>> grouped = list.stream()
                .collect(Collectors.groupingBy(Transaction::getPayer, Collectors.toList()));

        grouped.entrySet().stream()
                .map(it -> {
                    BigDecimal payerBalance = payerBalances.getOrDefault(it.getKey(), BigDecimal.ZERO);
                    return getOverflowed(it.getValue(), comparator, payerBalance);
                })
                .flatMap(Collection::stream)
                .forEach(it -> removeItem(list, it));
    }

    private static List<Transaction> getOverflowed(List<Transaction> list, Comparator<Transaction> comparator, BigDecimal threshold) {
        list.sort(comparator);
        List<Transaction> overflowed = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction transaction : list) {
            BigDecimal sumWithCurrent = sum.add(transaction.getAmount());
            if (sumWithCurrent.compareTo(threshold) <= 0) {
                sum = sumWithCurrent;
            } else {
                overflowed.add(transaction);
            }
        }
        return overflowed;
    }

    private static void removeItem(List<Transaction> list, Transaction transaction) {
        list.remove(transaction);
        Logger.getDebugLogger().printf(">>reject transaction: %s \n", transaction);
    }
}
