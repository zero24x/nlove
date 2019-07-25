package com.nlove.provider;

import com.google.protobuf.ByteString;

public class ReverseProxyReplyPacket {

	private ByteString data;
	private String destination;

	public ReverseProxyReplyPacket(String destination, ByteString data) {
		this.destination = destination;
		this.data = data;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public ByteString getData() {
		return data;
	}

	public void setData(ByteString data) {
		this.data = data;
	}

}
