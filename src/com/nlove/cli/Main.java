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
import com.nlove.config.NloveProfile;
import com.nlove.config.NloveProfileManager;
import com.nlove.handler.ClientCommandHandler;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.wallet.WalletException;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws NKNClientException, WalletException, InterruptedException, IOException, ParseException {

		String version = Main.class.getPackage().getImplementationVersion();
		if (version != null) {
			LOG.info("Starting nlove version {}, please wait... ", version);
		}

		final String helpText = String
				.format("Welcome to nlove! For discussion and support join #nlove on D-Chat! (See https://gitlab.com/losnappas/d-chat) \r\nType one of this commands: \r\n\r\n"
						+ "update-profile --> Update your profile\r\n" + "roll --> Find a random user profile\r\n" + "help --> View this command help\r\n", version);

		Options options = new Options();
		options.addOption("debug", false, "display debug information");
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = parser.parse(options, args);

		boolean isDebug = cmd.hasOption("debug") || java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

		setupLogging(isDebug ? TPLogger.DEBUG : TPLogger.INFO);

		NloveProfileManager.INSTANCE.loadOrCreate();
		NloveProfile profile = NloveProfileManager.INSTANCE.getProfile();

		LOG.info(String.format("Your username: %s", profile.getUsername()));
		LOG.info(String.format("Estimated active global nlove instances: %d", NKNExplorer.getSubscribers(ClientCommandHandler.LOBBY_TOPIC, 0).length));

		ClientCommandHandler cch = new ClientCommandHandler();
		cch.start();

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(in);

		LOG.info("Loading done!");
		LOG.info(helpText);

		while (true) {
			String line = br.readLine();
			try {
				String[] splitted = line.split("[\\s]+|\"([^\"]*)\"");

				LOG.info("Got command: {}", line);

				if (splitted[0].equals("update-profile")) {
					NloveProfileManager.INSTANCE.updateProfile();
				} else if (splitted[0].equals("roll")) {
					cch.roll();
				} else if (line.equals("help")) {
					LOG.info(helpText);
				} else {
					LOG.info("Unknown command, please read help: " + helpText);
				}
			} catch (NullPointerException e) {
				continue;
			}
		}
	}
}