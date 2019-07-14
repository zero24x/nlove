package com.nlove.message;

public enum MessageTypeEnum {
	CHAT("CHAT"), DOWNLOAD_DATA("DOWNLOAD_DAT"), DOWNLOAD_REQUEST("DOWNLOAD_REQUEST"), SEARCH("SEARCH");

	private String name;

	MessageTypeEnum(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
