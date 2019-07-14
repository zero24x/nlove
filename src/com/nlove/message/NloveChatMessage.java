package com.nlove.message;

public class NloveChatMessage implements NloveMessageInterface {

	private String text;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.CHAT;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
