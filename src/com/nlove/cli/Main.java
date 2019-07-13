package com.nlove.cli;

import com.darkyen.tproll.TPLogger;
import com.nlove.command.NloveCommand;
import com.nlove.handler.ProviderCommandHandler;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.network.HttpApi;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;
import jsmith.nknsdk.wallet.WalletUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import static jsmith.nknsdk.examples.LogUtils.setupLogging;

public class Main {
	
	private static final Logger LOG = LoggerFactory.getLogger(NKNClient.class);

	public static void main(String[] args) throws NKNClientException, WalletException, InterruptedException, IOException {

		setupLogging(TPLogger.DEBUG);

		final File walletFile = new File("pubsub.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "pwd");

		final Wallet pubsubWallet = Wallet.load(walletFile, "pwd");
		final String lobbyTopic = "nlove-lobby";

		final NKNExplorer.Subscriber[] subscribers = NKNExplorer.getSubscribers(lobbyTopic, 0);

		LOG.info("Subscribers of '" + lobbyTopic + "':");
		for (NKNExplorer.Subscriber s : subscribers) {
			LOG.info("  " + s.fullClientIdentifier + (s.meta.isEmpty() ? "" : ": " + s.meta));
		}
		LOG.info("Total: " + subscribers.length + " subs");

		final String identifier = "clientA";

		System.out.println("Subscribing to '" + lobbyTopic + "' using " + identifier
				+ (identifier == null || identifier.isEmpty() ? "" : ".")
				+ Hex.toHexString(pubsubWallet.getPublicKey()));
		final String txID = pubsubWallet.tx().subscribe(lobbyTopic, 0, 50, identifier, (String) null);

		if (txID == null) {
			LOG.error("Subscribe transaction failed");
			System.exit(1);
		} else {
			LOG.info("Subscribe transaction successful: " + txID);
		}

		 InputStreamReader in = new InputStreamReader(System.in);
         BufferedReader br = new BufferedReader(in);
         
         System.out.println("Please type one of these commands: \n"
         		+ "search <kitties> --> Search for providers offering files with  \"kitties\" in the name"
        		+ "startProvider --> Start providing all files in the subfolder \"share\" for others to search & download"
        		 );
         
         Identity clientIdentity = new Identity(identifier, pubsubWallet);
        NKNClient clientClient =  new NKNClient(new Identity(null, Wallet.createNew())).start();
         
         while (true) {
        	 String line = br.readLine();
        	 String[] splitted = line.split("\\s+");
        	 
        	 if (splitted[0] == "search") {
        		 clientClient.publishTextMessageAsync(lobbyTopic, 0, new NloveCommand().search(splitted[1]));
        		 
        	 } else if (splitted[0] == "startProvider" ) {

        			ProviderCommandHandler pch = new ProviderCommandHandler();
        			Identity pchIdentity = new Identity(identifier, pubsubWallet);
        			new NKNClient(pchIdentity).onNewMessage(msg -> {
        				pch.handle(msg);
        			}).start();
        			LOG.info("Provider started");
        	 }
         }
        
	
	}

}
