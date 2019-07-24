package com.nlove.message;

import java.nio.ByteBuffer;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.protobuf.ByteString;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class NloveMessageConverter {

	private String name;
	private ObjectMapper mapper;

	public NloveMessageConverter(String name) {
		this.name = name;
		this.mapper = new ObjectMapper().registerModule(new JsonOrgModule());
	}

	public NloveMessageInterface parseMsg(ReceivedMessage msg) {

		if (!msg.isText) {
			return null;
		}

		JSONObject jsonMsg = new JSONObject(msg.textData);

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
			String msgString = mapper.writeValueAsString(c);

			return msgString;
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
			byte[] headerStringBytes = this.mapper.writeValueAsString(header).getBytes();
			short headerLen = (short) headerStringBytes.length;

			ByteBuffer headerBuf = ByteBuffer.allocate(headerStringBytes.length + Short.BYTES);
			headerBuf.putShort(headerLen);
			headerBuf.put(headerStringBytes);

			return headerBuf.array();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public DecodedNloveMessage decodeNloveMessage(ByteString data) {
		DecodedNloveMessage res = new DecodedNloveMessage();

		ByteBuffer buf = data.asReadOnlyByteBuffer();

		short headerLen = buf.getShort();
		byte[] header = new byte[headerLen];

		buf.get(header, 0, headerLen);
		String headerString = new String(header);

		JSONObject headerJson = new JSONObject(headerString);

		NloveReverseProxyMessageHeader decodedHeader = mapper.convertValue(headerJson, NloveReverseProxyMessageHeader.class);

		byte[] payload = new byte[buf.remaining()];
		buf.get(payload);

		res.setHeader(decodedHeader);
		res.setPayload(payload);

		return res;
	}
}
