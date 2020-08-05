package org.hyperskill.blockchain;

public interface BlockchainConfiguration {
    long getBlockGenMsMin();

    long getBlockGenMsMax();

    int getMaxTransactionsInBlock();

    double getMinerInitialReward();

    int getMinerRewardLogBase();

    int getBlockGenAvgCount();

    int getMaxZeros();
}
