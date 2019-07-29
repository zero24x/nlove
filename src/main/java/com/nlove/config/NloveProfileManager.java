package com.nlove.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class NloveProfileManager {

    private static final Logger LOG = LoggerFactory.getLogger(NloveProfileManager.class);

    private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final File dataDir = Paths.get(System.getProperty("user.dir"), "data").toFile();
    private final File profileFile = Paths.get(System.getProperty("user.dir"), "data", File.separator.toString(), "profile.json").toFile();
    public static NloveProfileManager INSTANCE = new NloveProfileManager();

    private volatile NloveProfile profile = null;

    public NloveProfileManager() {
        try {
            if (this.profileFile.exists()) {
                this.profile = mapper.readValue(profileFile, NloveProfile.class);
            }
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public synchronized Boolean profileIsEmpty() {
        return !profileFile.exists() || profileFile.length() == 0;
    }

    public NloveProfile getProfile() {
        return this.profile;
    }

    public void setProfile(NloveProfile profile) {
        this.profile = profile;
    }

    public synchronized void saveProfile() {
        try {
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(profileFile, this.profile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LOG.info("Profile saved!");
        return;
    }

}
