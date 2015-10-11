package net.lamberto.configuration;

public class ConfigurationOptionsClassLevel {
	public static class NAME extends ConfigurationOptionTypes.StringOption {}
	public static class OPTIONS extends ConfigurationOptionTypes.StringArrayOption {}
	public static class TARGET_URL extends ConfigurationOptionTypes.URLOption {}
	public static class DISABLE_CONNECT extends ConfigurationOptionTypes.BooleanOption {}
	public static class MAX_FILE_SIZE extends ConfigurationOptionTypes.LongOption {}
}
