package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.Logger;
import org.hyperskill.blockchain.utils.SoutLogging;
import org.hyperskill.blockchain.utils.StringUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Block implements Serializable {
    private static final SoutLogging DEBUG_LOGGER = Logger.getDebugLogger();

    final static Block ZERO_BLOCK = new Block(null, Collections.emptyList()) {
        @Override
        public Long getId() {
            return 0L;
        }

        @Override
        public String getHash() {
            return "0";
        }
    };
    private static final String NO_TRANSACTIONS = "No transactions";

    final String minerId;
    private long timestamp;
    final private List<Transaction> transactions;
    private String prevHash;

    private Long id;
    private Long magicNumber;

    //info fields
    long generatingTime;
    String infoText;

    private Block(String minerId, List<Transaction> transactions) {
        this.minerId = minerId;
        this.timestamp = new Date().getTime();
        this.transactions = Collections.unmodifiableList(transactions);
    }

    public static Block createBlock(Blockchain blockchain, Miner miner, List<Transaction> data) {
        long startTime = System.currentTimeMillis();
        data.add(new Transaction(null, Blockchain.COINBASE, miner, blockchain.getMinerReward()));
        Block block = new Block(miner.getMinerId(), data);
        Random random = new Random();
        int counter = 0;
        do {
            Block lastBlock = blockchain.getLastBlock();
            block.prevHash = lastBlock.getHash();
            block.id = lastBlock.getId() + 1;
            block.magicNumber = random.nextLong();
            block.timestamp = new Date().getTime(); //timestamp in blocks should be increased

            counter = (counter + 1) % 10000;
            if (counter == 0) {
                DEBUG_LOGGER.printf(".");
            }
        } while (!block.isHashStartsWithZeroes(blockchain.getZerosCount()));
        long endTime = System.currentTimeMillis();
        block.generatingTime = (endTime - startTime);
        return block;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Long getMaxTransactionId() {
        OptionalLong max = transactions.stream()
                .filter(Predicate.not(Blockchain::isCoinBaseTransaction))
                .mapToLong(Transaction::getId).max();
        return max.isPresent() ? max.getAsLong() : null;
    }

    public Long getMinTransactionId() {
        OptionalLong min = transactions.stream()
                .filter(Predicate.not(Blockchain::isCoinBaseTransaction))
                .mapToLong(Transaction::getId).min();
        return min.isPresent() ? min.getAsLong() : null;
    }

    public boolean isHashStartsWithZeroes(int n) {
        final String zeroesPrefix = IntStream.range(0, n).mapToObj((it) -> "0").collect(Collectors.joining());
        return getHash().startsWith(zeroesPrefix);
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }

    private String getDataAsText() {
        if (transactions.isEmpty()) {
            return "\n" + NO_TRANSACTIONS;
        }
        return transactions.stream()
                .map(Transaction::toString)
                .collect(Collectors.joining("\n", "\n", ""));
    }

    public BigDecimal getMinerReward() {
        return getTransactions().stream()
                .filter(it -> Blockchain.COINBASE.equals(it.getPayer()))
                .map(Transaction::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    public String getMinerRewardString() {
        return new DecimalFormat("#.##").format(getMinerReward());
    }

    public String getHash() {
        String input = String.format("%d%s%s%d", timestamp, prevHash, getDataAsText(), magicNumber);
        return StringUtil.applySha256(input);
    }

    @Override
    public String toString() {
        return String.format("\nBlock:\n" +
                        "Created by: %s\n" +
                        "%1$s gets %s VC\n" +
                        "Id: %d\n" +
                        "Timestamp: %d\n" +
                        "Magic number: %d\n" +
                        "Hash of the previous block:\n%s\n" +
                        "Hash of the block:\n%s\n" +
                        "Block data: %s\n" +
                        "Block was generating for %d seconds\n" +
                        "%s\n",
                minerId, getMinerRewardString(), id, timestamp, magicNumber, prevHash, getHash(), getDataAsText(), generatingTime / 1000, infoText);
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
