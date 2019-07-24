package com.nlove.provider;

public class ReverseProxyReplyPacket {

	private byte[] data;
	private String destination;

	public ReverseProxyReplyPacket(String destination, byte[] data) {
		this.destination = destination;
		this.data = data;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
