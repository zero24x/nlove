package com.nlove.message;

public class NloveSearchMessage implements NloveMessageInterface {

	private String term;
	static String TYPE = "SEARCH";

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.SEARCH;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

}
