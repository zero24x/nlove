package com.nlove.message;

public class NloveSearchRequestReplyMessage implements NloveMessageInterface {

	private String result;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.SEARCH_REQUEST_REPLY;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}
