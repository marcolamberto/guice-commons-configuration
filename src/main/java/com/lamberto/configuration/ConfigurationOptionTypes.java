package com.lamberto.configuration;

import java.net.URL;


public class ConfigurationOptionTypes {
	public static class StringOption implements ConfigurationOptionType {
		@Override
		public Class<? extends Object> confType() {
			return String.class;
		}
	}

	public static class StringArrayOption implements ConfigurationOptionType {
		@Override
		public Class<? extends Object> confType() {
			return String[].class;
		}
	}

	public static class IntegerOption implements ConfigurationOptionType {
		@Override
		public Class<? extends Object> confType() {
			return Integer.class;
		}
	}

	public static class BooleanOption implements ConfigurationOptionType {
		@Override
		public Class<? extends Object> confType() {
			return Boolean.class;
		}
	}

	public static class URLOption implements ConfigurationOptionType {
		@Override
		public Class<? extends Object> confType() {
			return URL.class;
		}
	}
}
