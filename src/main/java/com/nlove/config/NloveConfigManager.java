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

public class NloveConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(NloveConfigManager.class);

    private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final File configDir = Paths.get(System.getProperty("user.dir"), "config").toFile();
    private final File configFile = Paths.get(System.getProperty("user.dir"), "config", File.separator.toString(), "config.json").toFile();
    public static NloveConfigManager INSTANCE = new NloveConfigManager();

    private volatile NloveConfig config = null;

    public NloveConfigManager() {
        try {
            if (this.configFile.exists()) {
                this.config = mapper.readValue(configFile, NloveConfig.class);
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

    public synchronized Boolean isEmpty() {
        return !configFile.exists() || configFile.length() == 0;
    }

    public NloveConfig getConfig() {
        return config;
    }

    public void setConfig(NloveConfig config) {
        this.config = config;
    }

    public synchronized void save() {
        try {
            if (!configDir.exists()) {
                configDir.mkdir();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, this.config);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LOG.info("Config saved!");
        return;
    }

}
