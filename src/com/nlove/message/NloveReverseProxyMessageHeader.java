package com.nlove.message;

public class NloveReverseProxyMessageHeader {

	private int clientPort;
	private Boolean socketClosed;
	private long sequenceNum;

	public long getSequenceNum() {
		return sequenceNum;
	}

	public void setSequenceNum(long sequenceNum) {
		this.sequenceNum = sequenceNum;
	}

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
