package org.hyperskill.blockchain;

public class BlockchainConfigurationImpl implements BlockchainConfiguration {
    private static final long BLOCK_GEN_MS_MIN = 1000;
    private static final long BLOCK_GEN_MS_MAX = 5000;
    public static final int MAX_ZEROS = 7;
    private static final int MAX_TRANSACTIONS_IN_BLOCK = 5;
    private static final double MINER_INITIAL_REWARD = 100.0;
    private static final int MINER_REWARD_LOG_BASE = 32;
    private static final int BLOCK_GEN_AVG_COUNT = 3;

    private final static BlockchainConfigurationImpl instance = new BlockchainConfigurationImpl();

    public static BlockchainConfiguration getInstance() {
        return instance;
    }

    @Override
    public long getBlockGenMsMin() {
        return BLOCK_GEN_MS_MIN;
    }

    @Override
    public long getBlockGenMsMax() {
        return BLOCK_GEN_MS_MAX;
    }

    @Override
    public int getMaxTransactionsInBlock() {
        return MAX_TRANSACTIONS_IN_BLOCK;
    }

    @Override
    public double getMinerInitialReward() {
        return MINER_INITIAL_REWARD;
    }

    @Override
    public int getMinerRewardLogBase() {
        return MINER_REWARD_LOG_BASE;
    }

    @Override
    public int getBlockGenAvgCount() {
        return BLOCK_GEN_AVG_COUNT;
    }

    @Override
    public int getMaxZeros() {
        return MAX_ZEROS;
    }
}
