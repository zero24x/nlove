package com.nlove.iota.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IotaNodeHelper {

    private ObjectMapper mapper = new ObjectMapper();
    private final File iotaNodeConfigFile = Paths.get(System.getProperty("user.dir"), "config", File.separator.toString(), "iota-node-config.json").toFile();
    private IotaNodeConfig config;
    private static final Logger LOG = LoggerFactory.getLogger(IotaNodeHelper.class);

    public IotaNodeHelper() {
        try {
            this.setConfig(mapper.readValue(iotaNodeConfigFile, IotaNodeConfig.class));
        } catch (IOException e) {
            throw new RuntimeException("Could not parse iota node config file", e);
        }
    }

    public IotaNodeConfig getConfig() {
        return config;
    }

    public void setConfig(IotaNodeConfig config) {
        this.config = config;
    }

}
