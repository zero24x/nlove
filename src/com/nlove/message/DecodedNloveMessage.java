package com.nlove.message;

public class DecodedNloveMessage {

	private NloveReverseProxyMessageHeader header;
	private byte[] payload;

	public NloveReverseProxyMessageHeader getHeader() {
		return header;
	}

	public void setHeader(NloveReverseProxyMessageHeader header) {
		this.header = header;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

}
