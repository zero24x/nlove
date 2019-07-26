package com.nlove.cli;

import static jsmith.nknsdk.examples.LogUtils.setupLogging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

	public static void main(String[] args) throws NKNClientException, WalletException, InterruptedException, IOException, ParseException {

		String version = Main.class.getPackage().getImplementationVersion();
		if (version != null) {
			LOG.info("Starting nlove version: " + version);
		}
		LOG.info("Loading, please wait ...");

		final String helpText = String
				.format("Welcome to nlove! To find file providers, join channel #nlove on D-Chat! (See https://gitlab.com/losnappas/d-chat) \r\nType one of this commands: \r\n\r\n"
						+ "help --> View this command help\r\n" + "list providers --> Shows names of all providers\r\n"
						+ "search <searchterm> --> Search for providers offering files with  \"kitties\" in the name\r\n"
						+ "connect <providerName> --> Connect to provider with name <providerName>\r\n" + "\r\nprovider enable --> Start becoming a file sharing provider"
						+ "\r\nprovider disable --> Stop providing file sharing services", version);

		Options options = new Options();
		options.addOption("debug", false, "display debug information");
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = parser.parse(options, args);

		boolean isDebug = cmd.hasOption("debug") || java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

		setupLogging(isDebug ? TPLogger.DEBUG : TPLogger.INFO);

		NloveConfigManager.INSTANCE.loadOrCreate();
		NloveConfig config = NloveConfigManager.INSTANCE.getConfig();

		LOG.info(String.format("Your username: %s", config.getUsername()));
		LOG.info(String.format("Estimated active global nlove instances: %d", NKNExplorer.getSubscribers(ClientCommandHandler.lobbyTopic, 0).length));
		LOG.info(String.format("Provider status: %s", config.getProviderEnabled() ? "ENABLED" : "DISABLED"));

		ClientCommandHandler cch = new ClientCommandHandler();
		cch.start();

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(in);

		if (config.getProviderEnabled()) {
			new ProviderManager().start();
		}

		ReverseProxyClientCommandHandler rch = new ReverseProxyClientCommandHandler();
		rch.start();

		LOG.info("Loading done!");
		LOG.info(helpText);

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
					LOG.info("Provider already enabled!");
				} else {
					config.setProviderEnabled(true);
					NloveConfigManager.INSTANCE.saveConfig();
					LOG.info(NloveConfigManager.RESTART_NEEDED);
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
				LOG.info(helpText);
			} else {
				LOG.info("Unknown command, please read help: " + helpText);
			}
		}
	}

}