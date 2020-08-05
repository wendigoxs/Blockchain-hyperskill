package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.AutoClosableLock;
import org.hyperskill.blockchain.utils.Logger;
import org.hyperskill.blockchain.utils.SoutLogging;
import org.hyperskill.blockchain.verification.Verificator;
import org.hyperskill.blockchain.verification.VerifyResult;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Simple Blockchain implementation
 *
 * @author Andrey Lavrukhin
 */
public class Blockchain implements Serializable {

    public static final CryptoCurrencyHolder COINBASE = new CryptoCurrencyHolderImpl("COINBASE", null);

    private static final boolean LOG_TRANSACTIONS = false;
    private static final SoutLogging LOGGER = Logger.getInstance(Logger.LogType.ALL);
    private static final SoutLogging DEBUG_LOGGER = Logger.getDebugLogger();
    private static final SoutLogging TRANSACTIONS_LOGGER = LOG_TRANSACTIONS ? DEBUG_LOGGER : Logger.getInstance(Logger.LogType.NONE);
    private static final int MAX_ZEROS = 7;

    private volatile int zerosCount = 0;
    private final BlockchainConfiguration configuration;
    private final AtomicLong idSequence;
    private final AtomicLong transactionIdCounter;
    private final Deque<Block> chain;
    private final Deque<Transaction> dataQueue;
    private final Object CHAIN_LOCK = new Object();
    private final Object ZEROS_LOCK = new Object();
    private final ReentrantReadWriteLock dataLock;
    private final Deque<Long> blockGeneratingTimes;
    private final Verificator verificator;

    public Blockchain() {
        configuration = BlockchainConfigurationImpl.getInstance();
        idSequence = new AtomicLong(0);
        transactionIdCounter = new AtomicLong(1);
        chain = new ConcurrentLinkedDeque<>();
        dataQueue = new ConcurrentLinkedDeque<>();
        dataLock = new ReentrantReadWriteLock(true);
        blockGeneratingTimes = new ArrayDeque<>(configuration.getBlockGenAvgCount());
        verificator = new Verificator(this);

        chain.push(Block.ZERO_BLOCK);
    }

    public Block generateBlock(Miner miner) {
        while (true) {
            Block block = Block.createBlock(this, miner, getTransactionsForBlock());
            VerifyResult verifyResult = verify(block);
            if (verifyResult == VerifyResult.OK) {
                synchronized (CHAIN_LOCK) {
                    verifyResult = verify(block);
                    if (verifyResult == VerifyResult.OK) {
                        addBlock(block);
                        return block;
                    } else {
                        DEBUG_LOGGER.printf("\n>> not verified STAGE2 %s hash:%s zerosCount:%d timeSpent:%d ms \n",
                                verifyResult, block.getHash(), zerosCount, block.generatingTime);
                    }
                }
            } else {
                DEBUG_LOGGER.printf("\n>> not verified STAGE1 %s hash:%s zerosCount:%d timeSpent:%d ms \n",
                        verifyResult, block.getHash(), zerosCount, block.generatingTime);
            }
        }
    }

    private List<Transaction> getTransactionsForBlock() {
        List<Transaction> transactions;
        try (var lock = new AutoClosableLock(dataLock.readLock())) {
            transactions = dataQueue.stream()
                    .filter(this::isTransactionOk)
                    .limit(configuration.getMaxTransactionsInBlock())
                    .collect(Collectors.toList());
        }
        rejectOverflowTransactions(transactions);
        return transactions;
    }

    private boolean isTransactionOk(Transaction transaction) {
        final Block NEW_BLOCK = null;
        Long maxTransactionId = getPrevBlockMaxTransactionId(NEW_BLOCK);
        boolean isIdOk = maxTransactionId == null || transaction.getId() > maxTransactionId; // transactions with small ids will be skipped (due to validation rules)
        boolean isSumOk = transaction.getAmount().compareTo(getAmount(transaction.getPayer())) <= 0;
        return isIdOk && (isCoinBaseTransaction(transaction) || isSumOk);
    }

    public BigDecimal getAmount(CryptoCurrencyHolder cryptoCurrencyHolder) {
        Map<Boolean, BigDecimal> balanceMap = chain.stream().flatMap(block -> block.getTransactions().stream())
                .filter(transaction -> cryptoCurrencyHolder.equals(transaction.getPayer()) || cryptoCurrencyHolder.equals(transaction.getPayee()))
                .filter(transaction -> !transaction.getPayer().equals(transaction.getPayee()))
                .collect(Collectors.partitioningBy(transaction -> cryptoCurrencyHolder.equals(transaction.getPayer()),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        return balanceMap.entrySet().stream()
                .map(e -> e.getKey() ? e.getValue().negate() : e.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add, BigDecimal::add);
    }

    private void rejectOverflowTransactions(List<Transaction> transactions) {
        RejectTransactionStrategy rejectTransactionStrategy = RejectTransactionStrategy.rejectSmallest();
        do {
            List<Map.Entry<CryptoCurrencyHolder, BigDecimal>> overflow = getTransactionsWithOverflow(transactions);
            if (overflow.isEmpty()) {
                break;
            }
            Set<CryptoCurrencyHolder> overflowPayers = overflow.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
            rejectTransactionStrategy.reject(transactions, getCurrentAmounts(overflowPayers));
        } while (true);
    }

    public List<Map.Entry<CryptoCurrencyHolder, BigDecimal>> getTransactionsWithOverflow(List<Transaction> transactions) {
        return transactions.stream()
                .filter(Predicate.not(Blockchain::isCoinBaseTransaction))
                .collect(Collectors.groupingBy(Transaction::getPayer, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .filter(e -> e.getValue().compareTo(getAmount(e.getKey())) > 0)
                .collect(Collectors.toList());
    }

    public VerifyResult verify(Block block) {
        return verificator.verifyBlock(block);
    }

    private void addBlock(Block block) {
        block.setId(idSequence.incrementAndGet());
        int deltaZeros = adjustZeros(block.generatingTime);
        block.setInfoText(getDeltaText(deltaZeros));
        chain.push(block);

        try (var lock = new AutoClosableLock(dataLock.writeLock())) {
            dataQueue.removeAll(block.getTransactions());
        }
        LOGGER.println(block);
        DEBUG_LOGGER.println(new Object() {
            @Override
            public String toString() {
                return getCurrentAmounts(Collections.emptyList()).toString();
            }
        });
    }

    private int adjustZeros(long blockGeneratingTime) {
        while (blockGeneratingTimes.size() >= configuration.getBlockGenAvgCount()) {
            blockGeneratingTimes.removeFirst();
        }
        blockGeneratingTimes.addLast(blockGeneratingTime);

        if (zerosCount >= configuration.getMaxZeros()) {
            return 0;
        }

        long avgBlockGenTime = (long) blockGeneratingTimes.stream().mapToLong(it -> it).average().orElse(0);
        if (avgBlockGenTime < configuration.getBlockGenMsMin()) {
            int newZerosCount = zerosCount + 1;
            synchronized (ZEROS_LOCK) {
                if (zerosCount < newZerosCount) {
                    zerosCount = newZerosCount;
                    return +1;
                }
            }
        }
        if (avgBlockGenTime > configuration.getBlockGenMsMax()) {
            int newZerosCount = Math.max(zerosCount - 1, 0);
            synchronized (ZEROS_LOCK) {
                if (zerosCount > newZerosCount) {
                    zerosCount = newZerosCount;
                    return -1;
                }
            }
        }
        return 0;
    }

    private String getDeltaText(int deltaZeros) {
        if (deltaZeros == 0) {
            return String.format("N stays the same %d", zerosCount);
        }
        return String.format(deltaZeros > 0
                        ? "N was increased to %d"
                        : "N was decreased to %d"
                , zerosCount);
    }

    public Blockchain addTransaction(Transaction transaction) {
        try (var lock = new AutoClosableLock(dataLock.writeLock())) {
            dataQueue.addLast(transaction);
            TRANSACTIONS_LOGGER.printf(">> added transaction: %s \n", transaction);
        } catch (Exception ex) {
            ex.printStackTrace(DEBUG_LOGGER.getPrintStream());
            throw new RuntimeException(ex);
        }
        return this;
    }

    private Map<CryptoCurrencyHolder, BigDecimal> getCurrentAmounts(Collection<CryptoCurrencyHolder> payersFilter) {
        return chain.stream().flatMap(block -> block.getTransactions().stream())
                .filter(it -> !it.getPayer().equals(it.getPayee()))
                .filter(it -> payersFilter.isEmpty() || payersFilter.contains(it.getPayer()) || payersFilter.contains(it.getPayee()))
                .flatMap(tr -> Stream.of(
                        Map.entry(tr.getPayer(), tr.getAmount().negate()),
                        Map.entry(tr.getPayee(), tr.getAmount())))
                .filter(it -> payersFilter.isEmpty() || payersFilter.contains(it.getKey()))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.reducing(BigDecimal.ZERO, Map.Entry::getValue, BigDecimal::add)));
    }

    public Block getLastBlock() {
        return chain.peek();
    }

    public Iterator<Block> getBlocksIterator() {
        return chain.iterator();
    }

    public Long getPrevBlockMaxTransactionId(Block block) {
        Iterable<Block> iterable = this::getBlocksIterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .dropWhile(it -> block != null && !it.getHash().equals(block.getPrevHash()))
                .map(Block::getMaxTransactionId)
                .filter(Objects::nonNull)
                .findFirst().orElse(-1L);
    }

    public static boolean isCoinBaseTransaction(Transaction transaction) {
        return COINBASE.equals(transaction.getPayer());
    }

    public int getZerosCount() {
        return zerosCount;
    }

    public long getNextTransactionId() {
        return transactionIdCounter.getAndIncrement();
    }

    public BigDecimal getMinerReward() {
        return getMinerReward(null);
    }

    public BigDecimal getMinerReward(Long blockId) {
        long blockNumber = Optional.ofNullable(blockId).orElse(chain.size() + 1L);
        int n = (int) (Math.log(blockNumber) / Math.log(configuration.getMinerRewardLogBase()));
        return BigDecimal.valueOf(configuration.getMinerInitialReward()).multiply(BigDecimal.valueOf(Math.pow(0.5, n)));
    }

    @Override
    public String toString() {
        Iterable<Block> iterable = () -> chain.descendingIterator();
        return StreamSupport.stream(iterable.spliterator(), false)
                .skip(1) //skip #0 block
                .map(Block::toString).collect(Collectors.joining("\n"));
    }
}
