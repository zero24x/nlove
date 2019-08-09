package com.nlove.hangout.message;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NloveHangoutMessage {

    private byte[] publicKeyBytes;
    private String signature;
    private String msg;
    private String username;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    public void setPublicKeyBytes(byte[] publicKeyBytes) {
        this.publicKeyBytes = publicKeyBytes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getSigningString() {
        JSONObject res = new JSONObject();
        res.put("msg", msg);
        res.put("publicKeyBytes", publicKeyBytes);
        res.put("username", username);

        return res.toString();
    }

}
