package org.hyperskill.blockchain.verification;

import org.hyperskill.blockchain.Block;
import org.hyperskill.blockchain.Blockchain;
import org.hyperskill.blockchain.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Verificator {

    Blockchain blockchain;

    public Verificator(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public VerifyResult verifyBlock(Block block) {
        if (!block.isHashStartsWithZeroes(blockchain.getZerosCount())) {
            return VerifyResult.FAIL_ZEROS;
        }
        if (!block.getPrevHash().equals(blockchain.getLastBlock().getHash())) {
            return VerifyResult.FAIL_PREV_HASH;
        }
        if (!verifyTransactionId(block)) {
            return VerifyResult.FAIL_TRANSACTION_ID;
        }
        if (!verifyTransactionsSign(block)) {
            return VerifyResult.FAIL_TRANSACTION_SIGN;
        }
        if (!verifyTransactionsAmount(block)) {
            return VerifyResult.FAIL_TRANSACTION_AMOUNT;
        }
        if (!verifyMinerReward(block)) {
            return VerifyResult.FAIL_MINER_REWARD;
        }
        return VerifyResult.OK;
    }

    private boolean verifyTransactionId(Block block) {
        Long prevMaxMessageId = blockchain.getPrevBlockMaxTransactionId(block);
        Long minMessageId = block.getMinTransactionId();
        return minMessageId == null || minMessageId > prevMaxMessageId;
    }



    private boolean verifyTransactionsSign(Block block) {
        return block.getTransactions().stream()
                .filter(Predicate.not(Blockchain::isCoinBaseTransaction))
                .allMatch(Transaction::validate);
    }

    private boolean verifyTransactionsAmount(Block block) {
        return blockchain.getTransactionsWithOverflow(block.getTransactions()).isEmpty();
    }

    private boolean verifyMinerReward(Block block) {
        List<BigDecimal> list = block.getTransactions().stream()
                .filter(Blockchain::isCoinBaseTransaction)
                .limit(2)
                .map(Transaction::getAmount)
                .collect(Collectors.toList());
        return list.isEmpty() || (list.size() == 1 && list.get(0).compareTo(blockchain.getMinerReward(block.getId())) <= 0);
    }

}
