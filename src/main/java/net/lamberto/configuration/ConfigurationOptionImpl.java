package net.lamberto.configuration;

import java.io.Serializable;
import java.lang.annotation.Annotation;

// NOTE: quick way for suppressing "The annotation type ConfigurationOption should not be used as a superinterface for ConfigurationOptionImpl" warning
@SuppressWarnings("all")
class   ConfigurationOptionImpl implements ConfigurationOption, Serializable {
	private static final long serialVersionUID = 1L;

	private final Class<? extends ConfigurationOptionType> value;

	public ConfigurationOptionImpl(final Class<? extends ConfigurationOptionType> value) {
		this.value = value;
	}

	@Override
	public Class<? extends ConfigurationOptionType> value() {
		return value;
	}

	@Override
	public int hashCode() {
		// This is specified in java.lang.Annotation.
		return (127 * "value".hashCode()) ^ value.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ConfigurationOption)) {
			return false;
		}

		final ConfigurationOption other = (ConfigurationOption) o;
		return value.equals(other.value());
	}

	@Override
	public String toString() {
		return "@" + ConfigurationOption.class.getName() + "(value=" + value + ")";
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConfigurationOption.class;
	}
}