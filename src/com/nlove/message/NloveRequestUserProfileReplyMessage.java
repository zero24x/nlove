package com.nlove.message;

import com.nlove.config.NloveProfile;

public class NloveRequestUserProfileReplyMessage implements NloveMessageInterface {
	private NloveProfile profile;

	@Override
	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.REQUEST_USER_PROFILE_REPLY;
	}

	public NloveProfile getProfile() {
		return profile;
	}

	public void setProfile(NloveProfile profile) {
		this.profile = profile;
	}

}
