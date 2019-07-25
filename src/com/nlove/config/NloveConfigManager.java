package com.nlove.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class NloveConfigManager {

	private static final Logger LOG = LoggerFactory.getLogger(NloveConfigManager.class);

	private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private final File configFile = Paths.get(System.getProperty("user.dir"), "config", File.separator.toString(), "config.json").toFile();
	public static NloveConfigManager INSTANCE = new NloveConfigManager();
	public static String RESTART_NEEDED = "This configuration change needs an restart, please restart the application now!";

	private volatile NloveConfig config = null;

	public synchronized void loadOrCreate() throws IOException {

		if (!configFile.exists() || configFile.length() == 0) {
			new File(System.getProperty("user.dir"), "config").mkdir();

			System.out.println("Welcome to Nlove! Please type the username you want to use and the return key to confirm:");

			InputStreamReader in = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(in);
			String line = br.readLine();
			this.config = new NloveConfig();
			config.setUsername(line);
			mapper.writeValue(configFile, this.config);
		}

		else {
			this.config = mapper.readValue(configFile, NloveConfig.class);
		}

	}

	public NloveConfig getConfig() {
		return this.config;
	}

	public synchronized void saveConfig() {
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, this.config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

}
