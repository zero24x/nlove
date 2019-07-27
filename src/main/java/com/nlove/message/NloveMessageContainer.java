package com.nlove.message;

public class NloveMessageContainer {

	private Object payload;
	private MessageTypeEnum messageType;

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public Object getPayload() {
		return this.payload;
	}

	public MessageTypeEnum getType() {
		return messageType;
	}

	public void setType(MessageTypeEnum type) {
		this.messageType = type;
	}

}
