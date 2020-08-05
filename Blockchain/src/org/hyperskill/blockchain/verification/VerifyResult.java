package org.hyperskill.blockchain.verification;

public enum VerifyResult {
    OK,
    FAIL_ZEROS,
    FAIL_PREV_HASH,
    FAIL_TRANSACTION_ID,
    FAIL_TRANSACTION_SIGN,
    FAIL_TRANSACTION_AMOUNT,
    FAIL_MINER_REWARD
}

