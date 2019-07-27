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
import com.nlove.config.NloveProfile.Gender;

public class NloveProfileManager {

	private static final Logger LOG = LoggerFactory.getLogger(NloveProfileManager.class);

	private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private final File profileFile = Paths.get(System.getProperty("user.dir"), "data", File.separator.toString(), "profile.json").toFile();
	public static NloveProfileManager INSTANCE = new NloveProfileManager();

	private volatile NloveProfile profile = null;

	public synchronized void loadOrCreate() throws IOException {

		if (!profileFile.exists() || profileFile.length() == 0) {
			new File(System.getProperty("user.dir"), "data").mkdir();
			this.profile = new NloveProfile();

			LOG.info("You do not have an user profile yet, please enter the following optional details and the RETURN key to confirm:");

			InputStreamReader in = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(in);

			String usernameQuestion = "Username:";
			LOG.info(usernameQuestion);
			String username = null;

			while (username == null) {
				String lineIn = br.readLine();
				if (lineIn.trim().length() > 0) {
					username = lineIn;
					profile.setUsername(username);
				} else {
					LOG.warn("Invalid input! Please type your " + usernameQuestion + ":");
				}
			}

			String genderQuestion = "Gender (Possible values: m, f, other):";
			LOG.info(genderQuestion);
			Gender gender = null;

			while (gender == null) {
				String lineIn = br.readLine();
				if (lineIn.equals("m") || lineIn.equals("f") || lineIn.equals("other")) {
					gender = Gender.valueOf(lineIn);
					profile.setGender(gender);

				} else {
					LOG.warn("Invalid input! Please type your " + genderQuestion + ":");
				}
			}

			String yobQuestion = "Year of birth:";
			LOG.info(yobQuestion);
			Integer yob = null;
			while (yob == null) {
				String lineIn = br.readLine();
				try {
					yob = Integer.parseInt(lineIn);
					profile.setYearOfBirth(yob);
				} catch (NumberFormatException e) {
					LOG.warn("Invalid input! Please type your " + yobQuestion + ":");
				}
			}

			String aboutQuestion = "About text (Example: I like trains):";
			LOG.info(aboutQuestion);
			String about = null;
			while (about == null) {
				String lineIn = br.readLine();
				if (lineIn.trim().length() > 0) {
					about = lineIn;
					profile.setAbout(about);
				} else {
					LOG.warn("Invalid input! Please type your " + aboutQuestion + ":");
				}
			}

			mapper.writeValue(profileFile, this.profile);
			LOG.info("User profile saved!");
		} else {
			this.profile = mapper.readValue(profileFile, NloveProfile.class);
		}

	}

	public void updateProfile() throws IOException {

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(in);

		LOG.info("Update your profile:");

		LOG.info("Username");
		LOG.info(" [Currently: [{}], Press RETURN key to keep :", profile.getUsername());
		String username = null;

		String lineIn = br.readLine();
		if (lineIn.length() > 0) {
			profile.setUsername(lineIn.trim());
		}

		String genderQuestion = "Gender (Possible values: m, f, other)";
		LOG.info(genderQuestion);
		LOG.info(" [Currently: [{}], Press RETURN key to keep :", profile.getGender());

		lineIn = br.readLine().trim();
		if (lineIn.length() > 0) {
			Gender gender = null;
			while (gender == null) {
				if (lineIn.equals("m") || lineIn.equals("f") || lineIn.equals("other")) {
					gender = Gender.valueOf(lineIn);
					profile.setGender(gender);
				} else {
					gender = null;
					LOG.warn("Invalid input! Please type your " + genderQuestion + ":");
					lineIn = br.readLine().trim();
				}
			}
		}

		String yobQuestion = "Year of birth:";
		LOG.info(yobQuestion);
		LOG.info(" [Currently: [{}] :", profile.getYearOfBirth());

		lineIn = br.readLine().trim();
		if (lineIn.length() > 0) {
			Integer yob = null;
			while (yob == null) {
				try {
					yob = Integer.parseInt(lineIn);
					profile.setYearOfBirth(yob);
				} catch (NumberFormatException e) {
					yob = null;
					LOG.warn("Invalid input! Please type your " + yobQuestion + ":");
					lineIn = br.readLine().trim();
				}
			}
		}

		String aboutQuestion = "About text (Example: I like trains):";
		LOG.info(aboutQuestion);
		LOG.info(" [Currently: [{}], Press RETURN key to keep :", profile.getAbout());

		String about = null;
		lineIn = br.readLine().trim();
		if (lineIn.length() > 0) {
			while (about == null) {
				if (lineIn.trim().length() > 0) {
					about = lineIn;
					profile.setAbout(about);
				} else {
					LOG.warn("Invalid input! Please type your " + aboutQuestion + ":");
					lineIn = br.readLine().trim();
				}
			}
		}
		this.profile = profile;
		this.saveProfile();
		LOG.info("User profile updated!");
	}

	public NloveProfile getProfile() {
		return this.profile;
	}

	public synchronized void saveProfile() {
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(profileFile, this.profile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

}
