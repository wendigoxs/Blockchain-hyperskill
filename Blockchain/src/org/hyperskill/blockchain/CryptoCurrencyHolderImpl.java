package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.CryptoHelper;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class CryptoCurrencyHolderImpl implements CryptoCurrencyHolder {

    private final String name;
    private final Blockchain blockchain;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public CryptoCurrencyHolderImpl(String name, Blockchain blockchain) {
        this.name = name;
        this.blockchain = blockchain;
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BigDecimal getAmount() {
        return blockchain.getAmount(this);
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public byte[] sign(byte[] data) {
        return CryptoHelper.createSign(data, privateKey);
    }

    @Override
    public String toString() {
        String hex = String.format("%1$8s", Integer.toHexString(publicKey.hashCode())).replace(" ", "0");
        return String.format("%s_pubK:%s", name, hex);
    }
}
