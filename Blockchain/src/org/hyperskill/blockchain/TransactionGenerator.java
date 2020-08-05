package org.hyperskill.blockchain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class TransactionGenerator {
    private final Blockchain blockchain;
    private final List<CryptoCurrencyHolder> cryptoCurrencyHolders;
    private final Random random;

    public TransactionGenerator(Blockchain blockchain, List<CryptoCurrencyHolder> cryptoCurrencyHolders) {
        this.blockchain = blockchain;
        this.cryptoCurrencyHolders = Collections.unmodifiableList(cryptoCurrencyHolders);
        this.random = new Random();
    }

    Stream<Transaction> generate() {
        var n = cryptoCurrencyHolders.size();

        return Stream.generate(() -> {
            var amount = new BigDecimal(String.valueOf(random.nextInt(9999) / 100.0));
            CryptoCurrencyHolder payer = cryptoCurrencyHolders.get(random.nextInt(n));
            CryptoCurrencyHolder payee = cryptoCurrencyHolders.get(random.nextInt(n));

            long transactionId = blockchain.getNextTransactionId();
            Transaction transaction = new Transaction(transactionId, payer, payee, amount);
            return transaction.setSignData(
                    payer.sign(transaction.getCryptoBlock()),
                    payer.getPublicKey());
        });
    }
}
