package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.CryptoHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Optional;

public class Transaction implements Serializable {
    private final long serialVersionUID = 1;
    private static final String NO_ID = "COINBASE:";

    private final Long id;
    private final CryptoCurrencyHolder payer;
    private final CryptoCurrencyHolder payee;
    private final BigDecimal amount;
    private byte[] sign;
    private PublicKey publicKey;

    public Transaction(Long id, CryptoCurrencyHolder payer, CryptoCurrencyHolder payee, BigDecimal amount) {
        this.id = id;
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
    }

    public Transaction setSignData(byte[] sign, PublicKey publicKey) {
        this.sign = sign;
        this.publicKey = publicKey;
        return this;
    }

    public Long getId() {
        return id;
    }

    public CryptoCurrencyHolder getPayer() {
        return payer;
    }

    public CryptoCurrencyHolder getPayee() {
        return payee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public byte[] getSign() {
        return sign;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return format(id, payer, payee, amount);
    }

    public byte[] getCryptoBlock() {
        return toString().getBytes();
    }

    public static String format(Long id, CryptoCurrencyHolder payer, CryptoCurrencyHolder payee, BigDecimal amount) {
        return String.format("%s %s sent to %s %s VC", Optional.ofNullable(id).map(Object::toString).orElse(NO_ID), payer, payee, amount);
    }

    public boolean validate() {
        return CryptoHelper.verifySign(toString().getBytes(), sign, publicKey);
    }
}
