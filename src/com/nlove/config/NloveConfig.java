package com.nlove.config;

public class NloveConfig {

	private boolean providerEnabled;
	private Integer providerPort;
	private String username;

	public Integer getProviderPort() {
		return providerPort;
	}

	public void setProviderPort(Integer providerPort) {
		this.providerPort = providerPort;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean getProviderEnabled() {
		return providerEnabled;
	}

	public void setProviderEnabled(boolean providerEnabled) {
		this.providerEnabled = providerEnabled;
	}

}
