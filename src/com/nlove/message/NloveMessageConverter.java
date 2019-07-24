package com.nlove.message;

import org.bouncycastle.util.Arrays;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class NloveMessageConverter {

	public static String MAGIC_IDENTIFIER = "<<NLOV>>";
	private String name;
	private ObjectMapper mapper;
	private byte[] IDENTIFIER_BYTES = NloveMessageConverter.MAGIC_IDENTIFIER.toString().getBytes();

	public NloveMessageConverter(String name) {
		this.name = name;
		this.mapper = new ObjectMapper().registerModule(new JsonOrgModule());
	}

	public NloveMessageInterface parseMsg(ReceivedMessage msg) {

		if (!msg.isText) {
			return null;
		}

		JSONObject jsonMsg = new JSONObject(msg.textData.substring(MAGIC_IDENTIFIER.length()));

		String type = jsonMsg.getString("type");
		JSONObject payload = jsonMsg.getJSONObject("payload");

		if (type.equals(MessageTypeEnum.SEARCH_REQUEST.toString())) {
			return mapper.convertValue(payload, NloveSearchRequestMessage.class);
		} else if (type.equals(MessageTypeEnum.SEARCH_REQUEST_REPLY.toString())) {
			return mapper.convertValue(payload, NloveSearchRequestReplyMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_REQUEST.toString())) {
			return mapper.convertValue(payload, NloveDownloadRequestMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_REQUEST_REPLY.toString())) {
			return mapper.convertValue(payload, NloveDownloadRequestReplyMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_DATA.toString())) {
			return mapper.convertValue(payload, NloveDownloadDataMessage.class);
		} else if (type.equals(MessageTypeEnum.REVERSE_PROXY_CONNECT.toString())) {
			return mapper.convertValue(payload, NloveReverseProxyConnectMessage.class);
		}

		return null;
	}

	public String toMsgString(NloveMessageInterface m) {

		NloveMessageContainer c = new NloveMessageContainer();
		c.setPayload(m);
		c.setType(m.getMessageType());

		JSONObject msg = new JSONObject();

		try {
			return String.format("%s%s", MAGIC_IDENTIFIER, mapper.writeValueAsString(c));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public byte[] makeHeaderBytes(boolean isAck, int clientPort, boolean socketClosed, int ackNum, int seqNum) {
		NloveReverseProxyMessageHeader header = new NloveReverseProxyMessageHeader();
		header.setIsAck(isAck);
		header.setClientPort(clientPort);
		header.setSocketClosed(socketClosed);
		header.setAckNum(ackNum);
		header.setSeqNum(seqNum);

		try {
			byte[] res = (this.mapper.writeValueAsString(header) + NloveMessageConverter.MAGIC_IDENTIFIER).getBytes();
			return res;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public DecodedNloveMessage decodeNloveMessage(ByteString data) {
		DecodedNloveMessage res = new DecodedNloveMessage();

		byte[] fullData = data.toByteArray();

		int headerEndPos = Bytes.indexOf(fullData, IDENTIFIER_BYTES);

		byte[] header = Arrays.copyOf(fullData, headerEndPos);

		String headerString = new String(header);
		JSONObject headerJson = new JSONObject(headerString);

		NloveReverseProxyMessageHeader decodedHeader = mapper.convertValue(headerJson, NloveReverseProxyMessageHeader.class);

		res.setHeader(decodedHeader);
		res.setPayload(Arrays.copyOfRange(fullData, headerEndPos + IDENTIFIER_BYTES.length, fullData.length));

		return res;
	}

}
