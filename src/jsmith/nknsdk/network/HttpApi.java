package jsmith.nknsdk.network;

import java.math.BigDecimal;
import java.net.InetSocketAddress;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.darkyen.dave.Response;
import com.darkyen.dave.ResponseTranslator;
import com.darkyen.dave.Webb;

import jsmith.nknsdk.client.NKNExplorer;

/**
 *
 */
public class HttpApi {

	private static final Logger LOG = LoggerFactory.getLogger(HttpApi.class);

	private static final Webb webb = new Webb(null);

	public static JSONObject rpcCallJson(InetSocketAddress to, String method, JSONObject parameters) {

		final JSONObject requestBody = new JSONObject();
		requestBody.put("jsonrpc", "2.0");
		requestBody.put("method", method);
		requestBody.put("params", parameters);

		Response<String> response = webb.post("http://" + to.getHostString() + ":" + to.getPort()).ensureSuccess().bodyJson(requestBody.toString())
				.connectTimeout(ConnectionProvider.rpcCallTimeoutMS()).readTimeout(ConnectionProvider.rpcCallTimeoutMS()).execute(ResponseTranslator.STRING_TRANSLATOR);

		return new JSONObject(response.getBody());
	}

	public static Integer getFirstAvailableTopicBucket(InetSocketAddress server, String topic) {
		final JSONObject params = new JSONObject();
		params.put("topic", topic);

		final JSONObject response = rpcCallJson(server, "getfirstavailabletopicbucket", params);
		return response.getInt("result");
	}

	public static Integer getTopicBucketsCount(InetSocketAddress server, String topic) {
		final JSONObject params = new JSONObject();
		params.put("topic", topic);

		final JSONObject response = rpcCallJson(server, "gettopicbucketscount", params);

		return response.getInt("result");
	}

	public static NKNExplorer.Subscriber[] getSubscribers(InetSocketAddress server, String topic, int bucket) {
		final JSONObject params = new JSONObject();
		params.put("topic", topic);
		params.put("bucket", bucket);

		final JSONObject response = rpcCallJson(server, "getsubscribers", params);
		final JSONObject result = response.getJSONObject("result");

		final NKNExplorer.Subscriber[] subscribers = new NKNExplorer.Subscriber[result.length()];

		int i = 0;
		for (String id : result.keySet()) {
			subscribers[i++] = new NKNExplorer.Subscriber(id, result.getString(id));
		}

		return subscribers;
	}

	public static BigDecimal getBalance(InetSocketAddress server, String nknAddress) {
		final JSONObject params = new JSONObject();
		params.put("address", nknAddress);
//        params.put("assetid", asset.ID); // TODO: Is it possible to set assetid in devnet?

		final JSONObject response = rpcCallJson(server, "getbalancebyaddr", params);

		return response.getJSONObject("result").getBigDecimal("amount");
	}

	public static long getNonce(InetSocketAddress server, String nknAddress) {
		final JSONObject params = new JSONObject();
		params.put("address", nknAddress);

		final JSONObject response = rpcCallJson(server, "getnoncebyaddr", params);

		long nonce = response.getJSONObject("result").getLong("nonce");
		if (response.getJSONObject("result").has("nonceInTxPool")) {
			nonce = Math.max(nonce, response.getJSONObject("result").getLong("nonceInTxPool"));
		}
		return nonce;
	}

	public static void sendRawTransaction(InetSocketAddress server, byte[] tx) throws JSONException, NknHttpApiException {
		sendRawTransaction(server, Hex.toHexString(tx));
	}

	public static String sendRawTransaction(InetSocketAddress server, String tx) throws JSONException, NknHttpApiException {
		final JSONObject params = new JSONObject();
		params.put("tx", tx);

		final JSONObject response = rpcCallJson(server, "sendrawtransaction", params);

		if (response.has("error")) {
			throw new NknHttpApiException(response.getJSONObject("error").getInt("code"), response.getJSONObject("error").getString("data"));
		}

		return response.getString("result");
	}

	public static String resolveName(InetSocketAddress server, String name) {
		final JSONObject params = new JSONObject();
		params.put("name", name);

		final JSONObject response = rpcCallJson(server, "getaddressbyname", params);

		return response.has("error") ? null : response.getString("result");
	}

}
