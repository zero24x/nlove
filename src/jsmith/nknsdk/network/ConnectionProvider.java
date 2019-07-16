package jsmith.nknsdk.network;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsmith.nknsdk.utils.ThrowingLambda;

/**
 *
 */
public class ConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectionProvider.class);

	private static final Object lock = new Object();

	private static InetSocketAddress[] bootstrapNodes = {

			new InetSocketAddress("mainnet-seed-0001.nkn.org", 30003),
			new InetSocketAddress("mainnet-seed-0002.nkn.org", 30003),
			new InetSocketAddress("mainnet-seed-0003.nkn.org", 30003),
			new InetSocketAddress("mainnet-seed-0004.nkn.org", 30003),
			new InetSocketAddress("mainnet-seed-0005.nkn.org", 30003),

	};

	private static int maxRetries = 3;
	private static int rpcCallTimeoutMS = 10000;
	private static int messageAckTimeoutMS = 10000;

	public static int maxRetries() {
		synchronized (lock) {
			return maxRetries;
		}
	}

	public static void maxRetries(int maxRetries) {
		if (maxRetries < 0)
			throw new IllegalArgumentException("Max retries must be non-negative number");
		synchronized (lock) {
			ConnectionProvider.maxRetries = maxRetries;
		}
	}

	public static int rpcCallTimeoutMS() {
		synchronized (lock) {
			return rpcCallTimeoutMS;
		}
	}

	public static void rpcCallTimeoutMS(int rpcCallTimeoutMS) {
		if (rpcCallTimeoutMS < 0)
			throw new IllegalArgumentException("Timeout must be non-negative number");
		synchronized (lock) {
			ConnectionProvider.rpcCallTimeoutMS = rpcCallTimeoutMS;
		}
	}

	public static int messageAckTimeoutMS() {
		synchronized (lock) {
			return messageAckTimeoutMS;
		}
	}

	public static void messageAckTimeoutMS(int messageAckTimeoutMS) {
		if (messageAckTimeoutMS < 0)
			throw new IllegalArgumentException("Timeout must be non-negative number");
		synchronized (lock) {
			ConnectionProvider.messageAckTimeoutMS = messageAckTimeoutMS;
		}
	}

	public static void setBootstrapNodes(InetSocketAddress[] nodes) {
		synchronized (lock) {
			bootstrapNodes = nodes;
		}
	}

	public static <T> T attempt(ThrowingLambda<InetSocketAddress, T> action) throws Exception {
		final int retries = maxRetries();
		Exception error = null;

		final InetSocketAddress[] nodes;
		synchronized (lock) {
			nodes = bootstrapNodes;
		}

		int nextNodeI = (int) (Math.random() * nodes.length);

		for (int i = 0; i <= retries; i++) {
			try {
				return action.apply(nodes[nextNodeI]);
			} catch (Exception t) {
				if (t instanceof NknHttpApiException) {
					return null;
				}

				error = t;
				LOG.warn("Attempt {} failed", i);
				LOG.debug("Caused by:", t);
			}
			nextNodeI++;
			if (nextNodeI >= nodes.length)
				nextNodeI -= nodes.length;
		}
		assert error != null;
		throw error;
	}

}
