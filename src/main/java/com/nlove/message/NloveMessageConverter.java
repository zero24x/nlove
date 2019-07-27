package com.nlove.message;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class NloveMessageConverter {

	private String name;
	private ObjectMapper mapper;

	public NloveMessageConverter(String name) {
		this.name = name;
		this.mapper = new ObjectMapper().registerModule(new JsonOrgModule()).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}

	public NloveMessageInterface parseMsg(ReceivedMessage msg) {

		if (!msg.isText) {
			return null;
		}

		JSONObject jsonMsg = new JSONObject(msg.textData);

		String type = jsonMsg.getString("type");
		JSONObject payload = jsonMsg.getJSONObject("payload");

		if (type.equals(MessageTypeEnum.REQUEST_USER_PROFILE_REQUEST.toString())) {
			return mapper.convertValue(payload, NloveRequestUserProfileRequestMessage.class);
		} else if (type.equals(MessageTypeEnum.REQUEST_USER_PROFILE_REPLY.toString())) {
			return mapper.convertValue(payload, NloveRequestUserProfileReplyMessage.class);
		}

		return null;

	}

	public String toMsgString(NloveMessageInterface m) {

		NloveMessageContainer c = new NloveMessageContainer();
		c.setPayload(m);
		c.setType(m.getMessageType());

		try {
			String msgString = mapper.writeValueAsString(c);

			return msgString;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
