package org.hyperskill.blockchain.utils;

import java.security.*;

public class CryptoHelper {

    private static final String RSA = "RSA";
    private static final String SHA_256_WITH_RSA = "SHA256withRSA";

    public static KeyPair generateKeyPair() {
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(RSA);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        return keyPairGenerator.generateKeyPair();
    }

    private static Signature getSignatureInstance() {
        try {
            return Signature.getInstance(SHA_256_WITH_RSA);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] createSign(byte[] data, PrivateKey privateKey) {
        Signature signature = getSignatureInstance();
        try {
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean verifySign(byte[] data, byte[] sign, PublicKey publicKey) {
        Signature signature = getSignatureInstance();
        try {
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(sign);
        } catch (SignatureException | InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    }


}
