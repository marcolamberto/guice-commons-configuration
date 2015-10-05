package com.lamberto.configuration;


public final class ConfigurationOptions {
	private ConfigurationOptions() {
		// utility class
	}

	public static ConfigurationOption configuration(final Class<? extends ConfigurationOptionType> option) {
		return new ConfigurationOptionImpl(option);
	}
}
