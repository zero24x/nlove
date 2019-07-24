package com.nlove.cli;

import static jsmith.nknsdk.examples.LogUtils.setupLogging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.darkyen.tproll.TPLogger;
import com.nlove.config.NloveConfig;
import com.nlove.config.NloveConfigManager;
import com.nlove.handler.ClientCommandHandler;
import com.nlove.handler.ReverseProxyClientCommandHandler;
import com.nlove.provider.ProviderManager;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.wallet.WalletException;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws NKNClientException, WalletException, InterruptedException, IOException {

		System.out.println("Loading, please wait ...");

		final String helpText = "Welcome to nlove! Type one of this commands: \r\n\r\n" + "help --> View this command help\r\n"
				+ "list providers --> Shows names of all providers\r\n" + "search <searchterm> --> Search for providers offering files with  \"kitties\" in the name\r\n"
				+ "connect <providerName> --> Connect to provider with name <providerName>\r\n" + "\r\nprovider enable --> Start becoming a file sharing provider"
				+ "\r\nprovider disable --> Stop providing file sharing services";

		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

		setupLogging(isDebug ? TPLogger.DEBUG : TPLogger.INFO);

		NloveConfigManager.INSTANCE.loadOrCreate();
		NloveConfig config = NloveConfigManager.INSTANCE.getConfig();

		System.out.println(String.format("Your username: %s", config.getUsername()));
		System.out.println(String.format("Estimated active global nlove instances: %d", NKNExplorer.getSubscribers(ClientCommandHandler.lobbyTopic, 0).length));
		System.out.println(String.format("Provider status: %s", config.getProviderEnabled() ? "ENABLED" : "DISABLED"));

		ClientCommandHandler cch = new ClientCommandHandler();
		cch.start();

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(in);

		if (config.getProviderEnabled()) {
			new ProviderManager().start();
		}

		ReverseProxyClientCommandHandler rch = new ReverseProxyClientCommandHandler();
		rch.start();

		System.out.println("Loading done!");
		System.out.println(helpText);

		while (true) {
			String line = br.readLine();
			String[] splitted = line.split("[\\s]+|\"([^\"]*)\"");

			if (splitted[0].equals("search")) {
				cch.search(splitted[1]);
				LOG.info("CLIENT: Sent command: search " + splitted[1]);

			} else if (splitted[0].equals("connect")) {
				LOG.info("CLIENT: Got command: connect " + splitted[1]);
				rch.connectToServiceProvider(splitted[1]);
			} else if (splitted[0].equals("provider") && splitted[1].equals("enable")) {
				if (config.getProviderEnabled()) {
					LOG.warn("Provider already enabled!");
				} else {
					config.setProviderEnabled(true);
					NloveConfigManager.INSTANCE.saveConfig();
					LOG.warn(NloveConfigManager.RESTART_NEEDED);
					Runtime.getRuntime().halt(0);
				}
			} else if (splitted[0].equals("provider") && splitted[1].equals("disable")) {

				if (!config.getProviderEnabled()) {
					LOG.warn("Provider already disabled!");
				} else {
					config.setProviderPort(null);
					config.setProviderEnabled(false);
					NloveConfigManager.INSTANCE.saveConfig();
					LOG.warn(NloveConfigManager.RESTART_NEEDED);
					Runtime.getRuntime().halt(0);
				}

			} else if (line.equals("help")) {
				System.out.println(helpText);
			} else {
				System.out.println("Unknown command, please read help: " + helpText);
			}
		}
	}

}