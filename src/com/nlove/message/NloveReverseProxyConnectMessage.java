package com.nlove.message;

public class NloveReverseProxyConnectMessage implements NloveMessageInterface {

	private int clientPort;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.REVERSE_PROXY_CONNECT;
	}

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

}
