package com.nlove.provider;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.cli.Main;
import com.nlove.config.NloveConfig;
import com.nlove.config.NloveConfigManager;
import com.nlove.handler.ProviderCommandHandler;
import com.nlove.handler.ReverseProxyProviderCommandHandler;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.wallet.WalletException;

public class ProviderManager {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public void start() throws NKNClientException, WalletException, IOException {

		File shareFolder = new File(System.getProperty("user.dir"));

		Boolean startProvider = shareFolder.exists() && shareFolder.isDirectory() && shareFolder.list().length > 0;
		if (!startProvider) {
			LOG.warn("PROVIDER: Could not start, \"shared\" directory missing or empty.");
			return;
		}

		NloveConfig config = NloveConfigManager.INSTANCE.getConfig();
		if (config.getProviderPort() == null) {
			Scanner in = new Scanner(System.in);
			System.out.println("Please enter your provider port e.g. 80 if you want to share your HTTP server for others to connect to.");
			int port = in.nextInt();
			config.setProviderPort(port);
			NloveConfigManager.INSTANCE.saveConfig();
		} else {
			LOG.info("Provider port: {} (Your share the server listening on this port with others!)", config.getProviderPort());
		}

		ProviderCommandHandler pch = new ProviderCommandHandler();
		pch.start();

		ReverseProxyProviderCommandHandler rpch = new ReverseProxyProviderCommandHandler();
		rpch.start();

	}

}
