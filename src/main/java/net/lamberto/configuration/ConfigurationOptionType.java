package net.lamberto.configuration;

import org.apache.commons.configuration.Configuration;

public interface ConfigurationOptionType<T> {
	Class<T> getConfigurationType();

	T getValueFor(final String name, final Configuration configuration);
}
