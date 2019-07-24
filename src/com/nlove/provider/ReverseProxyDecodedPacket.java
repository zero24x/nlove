package com.nlove.provider;

import com.nlove.message.DecodedNloveMessage;

public class ReverseProxyDecodedPacket {

	private String connectionKey;
	private String fromIdentifier;
	private DecodedNloveMessage message;

	public ReverseProxyDecodedPacket(String connectionKey, String fromIdentifier, DecodedNloveMessage message) {
		this.connectionKey = connectionKey;
		this.fromIdentifier = fromIdentifier;
		this.message = message;
	}

	public String getConnectionKey() {
		return connectionKey;
	}

	public void setConnectionKey(String connectionKey) {
		this.connectionKey = connectionKey;
	}

	public String getFromIdentifier() {
		return fromIdentifier;
	}

	public void setFromIdentifier(String fromIdentifier) {
		this.fromIdentifier = fromIdentifier;
	}

	public DecodedNloveMessage getMessage() {
		return message;
	}

	public void setMessage(DecodedNloveMessage message) {
		this.message = message;
	}

}
