package org.hyperskill.blockchain;

import java.math.BigDecimal;
import java.security.PublicKey;

public interface CryptoCurrencyHolder {
    String getName();

    BigDecimal getAmount();

    PublicKey getPublicKey();

    byte[] sign(byte[] data);
}
