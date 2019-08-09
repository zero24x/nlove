package com.nlove.config;

public class NloveConfig {
    private byte[] rsaPublicKeyBytes;
    private byte[] rsaPrivateKeyBytes;

    public byte[] getRsaPrivateKeyBytes() {
        return rsaPrivateKeyBytes;
    }

    public void setRsaPrivateKeyBytes(byte[] rsaPrivateKeyBytes) {
        this.rsaPrivateKeyBytes = rsaPrivateKeyBytes;
    }

    public byte[] getRsaPublicKeyBytes() {
        return rsaPublicKeyBytes;
    }

    public void setRsaPublicKeyBytes(byte[] rsaPublicKeyBytes) {
        this.rsaPublicKeyBytes = rsaPublicKeyBytes;
    }

}
