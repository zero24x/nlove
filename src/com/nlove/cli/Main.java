package com.nlove.cli;

import static jsmith.nknsdk.examples.LogUtils.setupLogging;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.darkyen.tproll.TPLogger;
import com.nlove.handler.ClientCommandHandler;
import com.nlove.handler.ProviderCommandHandler;
import com.nlove.handler.ReverseProxyClientCommandHandler;
import com.nlove.handler.ReverseProxyProviderCommandHandler;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.wallet.WalletException;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws NKNClientException, WalletException, InterruptedException, IOException {

		final String helpText = "Welcome to nlove! Important commands: \n" + "help --> View this command help"
				+ "search <searchterm> --> Search for providers offering files with  \"kitties\" in the name\n"
				+ "download <filepath> --> Download a certain file provided by any provider\n" + "chat <message> --> Write a public message in the lobby chat\n"
				+ "Note: To start providing files, simply put them in the \"share\" subfolder.";
		System.out.println(helpText);

		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

		setupLogging(isDebug ? TPLogger.DEBUG : TPLogger.INFO);

		final String lobbyTopic = "nlove-lobby";

		/*
		 * final NKNExplorer.Subscriber[] subscribers =
		 * NKNExplorer.getSubscribers(lobbyTopic, 0);
		 * 
		 * LOG.info("Subscribers of '" + lobbyTopic + "':"); for (NKNExplorer.Subscriber
		 * s : subscribers) { LOG.info("  " + s.fullClientIdentifier + (s.meta.isEmpty()
		 * ? "" : ": " + s.meta)); } LOG.info("Total: " + subscribers.length + " subs");
		 */

		ClientCommandHandler cch = new ClientCommandHandler();
		cch.start();
		Thread.sleep(500);

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(in);

		File shareFolder = new File(System.getProperty("user.dir"));

		Boolean startProvider = shareFolder.exists() && shareFolder.isDirectory() && shareFolder.list().length > 0;

		ReverseProxyClientCommandHandler rch = new ReverseProxyClientCommandHandler();
		rch.start();

		if (startProvider) {
			ProviderCommandHandler pch = new ProviderCommandHandler();
			pch.start();

			ReverseProxyProviderCommandHandler rpch = new ReverseProxyProviderCommandHandler();
			rpch.start();

		} else {
			LOG.info("PROVIDER: Not starting, \"shared\" directory missing or empty.");
		}

		while (true) {
			String line = br.readLine();
			String[] splitted = line.split("\\s+");

			if (splitted[0].equals("search")) {
				cch.search(splitted[1]);
				LOG.info("CLIENT: Sent command: search " + splitted[1]);

			} else if (splitted[0].equals("connect")) {
				LOG.info("CLIENT: Got command: connect " + splitted[1]);
				rch.connectToServiceProvider(splitted[1]);
			} else if (splitted[0].equals("chat")) {
				cch.chat(splitted[1]);
				LOG.info("CLIENT: Sent command: chat " + splitted[1]);
			} else if (line.equals("help")) {
				System.out.println(helpText);
			} else {
				System.out.println("Unknown command, please read help: " + helpText);
			}
		}
	}
}