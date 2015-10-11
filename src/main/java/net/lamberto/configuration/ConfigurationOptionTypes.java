package net.lamberto.configuration;

public interface ConfigurationOptionTypes {
	public static class StringOption implements ConfigurationOptionType {}
	public static class StringArrayOption implements ConfigurationOptionType {}
	public static class IntegerOption implements ConfigurationOptionType {}
	public static class BooleanOption implements ConfigurationOptionType {}
	public static class URLOption implements ConfigurationOptionType {}
}
