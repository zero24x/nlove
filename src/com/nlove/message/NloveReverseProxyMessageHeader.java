package com.nlove.message;

public class NloveReverseProxyMessageHeader {

	private int clientPort;
	private Boolean socketClosed;

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public Boolean getSocketClosed() {
		return socketClosed;
	}

	public void setSocketClosed(Boolean socketClosed) {
		this.socketClosed = socketClosed;
	}

}
