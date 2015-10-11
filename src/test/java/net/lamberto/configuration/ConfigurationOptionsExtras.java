package net.lamberto.configuration;

import org.apache.commons.configuration.Configuration;

public interface ConfigurationOptionsExtras {
	public static class MISSING extends ConfigurationOptionTypes.StringOption {}

	public static class CUSTOM_OPTION implements ConfigurationOptionType<Object> {
		@Override
		public Class<Object> getConfigurationType() {
			return Object.class;
		}

		@Override
		public Object getValueFor(final String name, final Configuration configuration) {
			return configuration.getString(name);
		}
	}
}
