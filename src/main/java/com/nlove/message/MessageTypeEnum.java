package com.nlove.message;

public enum MessageTypeEnum {
	REQUEST_USER_PROFILE_REQUEST("REQUEST_USER_PROFILE_REQUEST"), REQUEST_USER_PROFILE_REPLY("REQUEST_USER_PROFILE_REPLY");

	private String name;

	MessageTypeEnum(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
