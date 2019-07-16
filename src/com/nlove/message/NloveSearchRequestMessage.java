package com.nlove.message;

public class NloveSearchRequestMessage implements NloveMessageInterface {

	private String term;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.SEARCH_REQUEST;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

}
