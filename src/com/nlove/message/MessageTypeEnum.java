package com.nlove.message;

public enum MessageTypeEnum {
	CHAT("CHAT"), SEARCH_REQUEST("SEARCH_REQUEST"), SEARCH_REQUEST_REPLY("SEARCH_REQUEST_REPLY"), REVERSE_PROXY_CONNECT("REVERSE_PROXY_CONNECT");

	private String name;

	MessageTypeEnum(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
