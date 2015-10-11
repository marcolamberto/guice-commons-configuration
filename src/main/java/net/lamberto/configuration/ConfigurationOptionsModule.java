package net.lamberto.configuration;

import static java.util.Arrays.asList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lamberto.configuration.ConfigurationOptionTypes.BooleanOption;
import net.lamberto.configuration.ConfigurationOptionTypes.IntegerOption;
import net.lamberto.configuration.ConfigurationOptionTypes.StringArrayOption;
import net.lamberto.configuration.ConfigurationOptionTypes.StringOption;
import net.lamberto.configuration.ConfigurationOptionTypes.URLOption;

@Slf4j
public class ConfigurationOptionsModule extends PrivateModule {
	private final Configuration configuration;
	private final Collection<Class<? extends ConfigurationOptionType>> configurationOptions;


	@RequiredArgsConstructor
	private static enum ConversionStrategy {
		BOOLEAN(BooleanOption.class, Boolean.class) {
			@Override
			public Boolean getValueFor(final String name, final Configuration configuration) {
				return configuration.getBoolean(name);
			}
		},
		INTEGER(IntegerOption.class, Integer.class) {
			@Override
			public Integer getValueFor(final String name, final Configuration configuration) {
				return configuration.getInteger(name, -1);
			}
		},
		STRING(StringOption.class, String.class) {
			@Override
			public String getValueFor(final String name, final Configuration configuration) {
				return Joiner.on(',').join(configuration.getStringArray(name));
			}
		},
		STRING_ARRAY(StringArrayOption.class, String[].class) {
			@Override
			public String[] getValueFor(final String name, final Configuration configuration) {
				return configuration.getStringArray(name);
			}
		},
		URL(URLOption.class, URL.class) {
			@Override
			public URL getValueFor(final String name, final Configuration configuration) {
                try {
                    return new URL(configuration.getString(name));
                } catch (final MalformedURLException e) {
                    log.warn("Invalid URL", e);
                    return null;
                }
			}
		},


		;


		@Getter
		private final Class<? extends ConfigurationOptionType> configurationOptionType;

		@Getter
		private final Class<?> configurationType;


		private static final Map<Class<? extends ConfigurationOptionType>, ConversionStrategy> FOR_OPTION_TYPE;
		static {
			final Builder<Class<? extends ConfigurationOptionType>, ConversionStrategy> builder = ImmutableMap.<Class<? extends ConfigurationOptionType>, ConversionStrategy>builder();

			for (final ConversionStrategy strategy : values()) {
				builder.put(strategy.getConfigurationOptionType(), strategy);
			}

			FOR_OPTION_TYPE = builder.build();
		}


		public static ConversionStrategy forOptionType(final Class<? extends ConfigurationOptionType> type) {
			final Optional<ConversionStrategy> strategy = discoverOptionType(type);
			if (strategy.isPresent()) {
				return strategy.get();
			}

			final String message = String.format("No configuration type found for '%s'", type.getSimpleName());
			log.warn(message);
			throw new ConfigurationNotFoundException(message);
		}

		@SuppressWarnings("unchecked")
		private static Optional<ConversionStrategy> discoverOptionType(final Class<? extends ConfigurationOptionType> type) {
			if (FOR_OPTION_TYPE.containsKey(type)) {
				return Optional.of(FOR_OPTION_TYPE.get(type));
			}

			final Class<?> superclass = type.getSuperclass();
			if (superclass != null) {
				return discoverOptionType((Class<? extends ConfigurationOptionType>) superclass);
			}

			return Optional.absent();
		}

		public abstract Object getValueFor(final String name, final Configuration configuration);

		public Object getValueFor(final Class<? extends ConfigurationOptionType> key, final Configuration configuration) {
			final String name = key.getSimpleName();
			if (configuration.containsKey(name)) {
				return getValueFor(name, configuration);
			}

			final String message = String.format("No configuration property found for '%s'", name);
			log.warn(message);
			throw new ConfigurationNotFoundException(message);
		}

		@SuppressWarnings("rawtypes")
		public Provider getProvider(final Class<? extends ConfigurationOptionType> configurationOption) {
			return new Provider<Object>() {
				@Inject
				private Configuration configuration;

				@Override
				public Object get() {
					return getValueFor(configurationOption, configuration);
				}
			};
		}
	}


	public ConfigurationOptionsModule(final Map<String, String> configuration, final Class<?> ... optionsHolderClasses) {
		log.debug("Configuration options values {}", configuration);
		this.configuration = new MapConfiguration(configuration);

		this.configurationOptions = Lists.newArrayList();
		for (final Class<?> optionsHolderClass : optionsHolderClasses) {
			discoverOptions(optionsHolderClass);
		}
	}

	@SuppressWarnings("unchecked")
	private void discoverOptions(final Class<?> optionsHolder) {
		if (isOption(optionsHolder)) {
			configurationOptions.add((Class<? extends ConfigurationOptionType>) optionsHolder);
			return;
		}

		for (final Class<?> option : optionsHolder.getClasses()) {
			//log.debug("* '{}'", option.getSimpleName());
			if (isOption(option)) {
				log.debug("Registering configuration option '{}'", option.getSimpleName());
				configurationOptions.add((Class<? extends ConfigurationOptionType>) option);
			}
		}

		for (final Class<?> ifs : optionsHolder.getInterfaces()) {
			discoverOptions(ifs);
		}
	}

	private boolean isOption(final Class<?> option) {
		return option != null && (Iterables.contains(asList(option.getInterfaces()), ConfigurationOptionType.class) || isOption(option.getSuperclass()));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		bind(Configuration.class).toInstance(configuration);

		for (final Class<? extends ConfigurationOptionType> configurationOption : configurationOptions) {
			final ConversionStrategy strategy = ConversionStrategy.forOptionType(configurationOption);

			final Class<?> fieldType = strategy.getConfigurationType();
			log.debug("Binding configuration named '{}' to field type '{}' annotated with '{}'", configurationOption.getSimpleName(), fieldType.getSimpleName(), strategy.getConfigurationOptionType().getSimpleName());

			bind(fieldType).annotatedWith(ConfigurationOptions.configuration(configurationOption)).toProvider(strategy.getProvider(configurationOption));

			expose(fieldType).annotatedWith(ConfigurationOptions.configuration(configurationOption));
		}
	}


	public String getString(final Class<? extends ConfigurationOptionType> key) {
		return (String) ConversionStrategy.STRING.getValueFor(key, configuration);
	}

	public Boolean getBoolean(final Class<? extends ConfigurationOptionType> key) {
		return (Boolean) ConversionStrategy.BOOLEAN.getValueFor(key, configuration);
	}
}
