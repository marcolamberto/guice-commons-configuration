package com.lamberto.configuration;

import static java.util.Arrays.asList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;

@Slf4j
public class ConfigurationOptionsModule extends PrivateModule {
	private final Configuration configuration;
	private final Collection<Class<? extends ConfigurationOptionType>> configurationOptions;


	private static enum ConversionStrategy {
		BOOLEAN() {
			@SuppressWarnings("unchecked")
			@Override
			public Boolean getValueFor(final String name, final Configuration configuration) {
				return configuration.getBoolean(name);
			}
		},
		INTEGER() {
			@SuppressWarnings("unchecked")
			@Override
			public Integer getValueFor(final String name, final Configuration configuration) {
				return configuration.getInteger(name, -1);
			}
		},
		STRING() {
			@SuppressWarnings("unchecked")
			@Override
			public String getValueFor(final String name, final Configuration configuration) {
				return Joiner.on(',').join(configuration.getStringArray(name));
			}
		},
		STRING_ARRAY() {
			@SuppressWarnings("unchecked")
			@Override
			public String[] getValueFor(final String name, final Configuration configuration) {
				return configuration.getStringArray(name);
			}
		},
		URL() {
			@SuppressWarnings("unchecked")
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

		public <T> T getValueFor(final Class<? extends ConfigurationOptionType> key, final Configuration configuration) {
			final String name = key.getSimpleName();
			if (configuration.containsKey(name)) {
				return getValueFor(name, configuration);
			}

			throw newConfigurationNotFoundException(name);
		}

		public abstract <T> T getValueFor(final String name, final Configuration configuration);
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

	@Override
	protected void configure() {
		bind(Configuration.class).toInstance(configuration);

		for (final Class<? extends ConfigurationOptionType> keyClass : configurationOptions) {
			final String name = keyClass.getSimpleName();
			final Class<? extends Object> confType = buildKey(keyClass).confType();
			log.debug("Binding {} => {}", name, confType);

			if (confType.equals(Integer.class)) {
				bind(Integer.class).annotatedWith(ConfigurationOptions.configuration(keyClass)).toProvider(new Provider<Integer>() {
					@Inject
					private Configuration configuration;

					@Override
					public Integer get() {
						return ConversionStrategy.INTEGER.getValueFor(keyClass, configuration);
					}
				});
			} else if (confType.equals(Boolean.class)) {
				bind(Boolean.class).annotatedWith(ConfigurationOptions.configuration(keyClass)).toProvider(new Provider<Boolean>() {
					@Inject
					private Configuration configuration;

					@Override
					public Boolean get() {
						return ConversionStrategy.BOOLEAN.getValueFor(keyClass, configuration);
					}
				});
			} else if (confType.equals(String.class)) {
				bind(String.class).annotatedWith(ConfigurationOptions.configuration(keyClass)).toProvider(new Provider<String>() {
					@Inject
					private Configuration configuration;

					@Override
					public String get() {
						return ConversionStrategy.STRING.getValueFor(keyClass, configuration);
					}
				});
			} else if (confType.equals(URL.class)) {
                bind(URL.class).annotatedWith(ConfigurationOptions.configuration(keyClass)).toProvider(new Provider<URL>() {
                    @Inject
                    private Configuration configuration;

                    @Override
                    public URL get() {
						return ConversionStrategy.URL.getValueFor(keyClass, configuration);
                    }
                });
            } else if (confType.equals(String[].class)) {
				bind(String[].class).annotatedWith(ConfigurationOptions.configuration(keyClass)).toProvider(new Provider<String[]>() {
					@Inject
					private Configuration configuration;

					@Override
					public String[] get() {
						return ConversionStrategy.STRING_ARRAY.getValueFor(keyClass, configuration);
					}
				});
			}

			expose(confType).annotatedWith(ConfigurationOptions.configuration(keyClass));
		}
	}

	private static ConfigurationNotFoundException newConfigurationNotFoundException(final String name) {
		final String message = "No configuration found for '" + name + "'";
		log.warn(message);
		return new ConfigurationNotFoundException(message);
	}

	private ConfigurationOptionType buildKey(final Class<? extends ConfigurationOptionType> keyClass) {
		try {
			return keyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
			throw new IllegalArgumentException("Cannot create config options instance for class " + keyClass);
		}
	}

	public String getString(final Class<? extends ConfigurationOptionType> key) {
		return ConversionStrategy.STRING.getValueFor(key, configuration);
	}

	public Boolean getBoolean(final Class<? extends ConfigurationOptionType> key) {
		return ConversionStrategy.BOOLEAN.getValueFor(key, configuration);
	}
}
