package org.hyperskill.blockchain;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.stream.LongStream;

public class Miner implements CryptoCurrencyHolder {
    private final String minerId;
    private final Blockchain blockchain;
    private final CryptoCurrencyHolder cryptoCurrencyHolder;

    public Miner(String minerId, Blockchain blockchain) {
        this.blockchain = blockchain;
        this.minerId = minerId;
        this.cryptoCurrencyHolder = new CryptoCurrencyHolderImpl(minerId, blockchain);
    }

    public String getMinerId() {
        return minerId;
    }

    public Block mineBlock() {
        return blockchain.generateBlock(this);
    }

    public void mine(long n) {
        LongStream.range(0, n).forEach(it -> mineBlock());
    }

    @Override
    public String getName() {
        return cryptoCurrencyHolder.getName();
    }

    @Override
    public BigDecimal getAmount() {
        return cryptoCurrencyHolder.getAmount();
    }

    @Override
    public PublicKey getPublicKey() {
        return cryptoCurrencyHolder.getPublicKey();
    }

    @Override
    public byte[] sign(byte[] data) {
        return cryptoCurrencyHolder.sign(data);
    }

    @Override
    public String toString() {
        return String.format("MINER$%s", cryptoCurrencyHolder.toString());
    }
}
