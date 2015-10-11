package net.lamberto.configuration;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Joiner;

import lombok.extern.slf4j.Slf4j;


public interface ConfigurationOptionTypes {
	public static class StringOption implements ConfigurationOptionType<String> {
		@Override
		public Class<String> getConfigurationType() {
			return String.class;
		}

		@Override
		public String getValueFor(final String name, final Configuration configuration) {
			return Joiner.on(',').join(configuration.getStringArray(name));
		}
	}

	public static class StringArrayOption implements ConfigurationOptionType<String[]> {
		@Override
		public Class<String[]> getConfigurationType() {
			return String[].class;
		}

		@Override
		public String[] getValueFor(final String name, final Configuration configuration) {
			return configuration.getStringArray(name);
		}
	}

	public static class IntegerOption implements ConfigurationOptionType<Integer> {
		@Override
		public Class<Integer> getConfigurationType() {
			return Integer.class;
		}

		@Override
		public Integer getValueFor(final String name, final Configuration configuration) {
			return configuration.getInteger(name, -1);
		}
	}

	public static class LongOption implements ConfigurationOptionType<Long> {
		@Override
		public Class<Long> getConfigurationType() {
			return Long.class;
		}

		@Override
		public Long getValueFor(final String name, final Configuration configuration) {
			return configuration.getLong(name);
		}
	}

	public static class BooleanOption implements ConfigurationOptionType<Boolean> {
		@Override
		public Class<Boolean> getConfigurationType() {
			return Boolean.class;
		}

		@Override
		public Boolean getValueFor(final String name, final Configuration configuration) {
			return configuration.getBoolean(name);
		}
	}

	@Slf4j
	public static class URLOption implements ConfigurationOptionType<URL> {
		@Override
		public Class<URL> getConfigurationType() {
			return URL.class;
		}

		@Override
		public URL getValueFor(final String name, final Configuration configuration) {
			final String url = configuration.getString(name);

            try {
				return new URL(url);
            } catch (final MalformedURLException e) {
            	final String message = String.format("Invalid URL '%s'", url);
				log.warn(message, e);
                throw new ConfigurationException(message);
            }
		}
	}
}
