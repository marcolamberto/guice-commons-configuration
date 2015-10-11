package net.lamberto.configuration;

import static java.util.Arrays.asList;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;

import lombok.extern.slf4j.Slf4j;
import net.lamberto.configuration.ConfigurationOptionTypes.BooleanOption;
import net.lamberto.configuration.ConfigurationOptionTypes.StringOption;

@Slf4j
public class ConfigurationOptionsModule extends PrivateModule {
	private final Configuration configuration;
	private final Map<Class<? extends ConfigurationOptionType<?>>, ConfigurationOptionType<?>> strategies;


	public ConfigurationOptionsModule(final Map<String, String> configuration, final Class<?> ... optionsHolderClasses) {
		log.debug("Configuration options values {}", configuration);
		this.configuration = new MapConfiguration(configuration);

		this.strategies = Maps.newHashMap();
		for (final Class<?> optionsHolderClass : optionsHolderClasses) {
			discoverOptions(optionsHolderClass);
		}
	}

	@SuppressWarnings("unchecked")
	private void discoverOptions(final Class<?> optionsHolder) {
		if (isOption(optionsHolder)) {
			registerStrategy((Class<? extends ConfigurationOptionType<?>>) optionsHolder);
			return;
		}

		for (final Class<?> option : optionsHolder.getClasses()) {
			//log.debug("* '{}'", option.getSimpleName());
			if (isOption(option)) {
				registerStrategy((Class<? extends ConfigurationOptionType<?>>) option);
			}
		}

		for (final Class<?> ifs : optionsHolder.getInterfaces()) {
			discoverOptions(ifs);
		}
	}

	private void registerStrategy(final Class<? extends ConfigurationOptionType<?>> type) {
		log.debug("Registering configuration option '{}'", type.getSimpleName());
		try {
			strategies.put(type, type.newInstance());
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = String.format("Cannot create configuration option '%s'", type.getSimpleName());
			log.warn(message);
			throw new ConfigurationException(message);
		}
	}

	private boolean isOption(final Class<?> option) {
		return option != null && (Iterables.contains(asList(option.getInterfaces()), ConfigurationOptionType.class) || isOption(option.getSuperclass()));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		bind(Configuration.class).toInstance(configuration);

		for (final Entry<Class<? extends ConfigurationOptionType<?>>,ConfigurationOptionType<?>> entry : strategies.entrySet()) {
			final Class<? extends ConfigurationOptionType<?>> configurationOption = entry.getKey();
			final ConfigurationOptionType<?> strategy = entry.getValue();

			final Class<?> fieldType = strategy.getConfigurationType();
			log.debug("Binding configuration named '{}' to field type '{}'",
				configurationOption.getSimpleName(),
				fieldType.getSimpleName()
			);

			bind(fieldType).annotatedWith(ConfigurationOptions.configuration(configurationOption)).toProvider(getProvider(strategy));

			expose(fieldType).annotatedWith(ConfigurationOptions.configuration(configurationOption));
		}
	}

	public Object getValueFor(final ConfigurationOptionType<?> strategy, final Configuration configuration) {
		final String name = strategy.getClass().getSimpleName();
		if (configuration.containsKey(name)) {
			return strategy.getValueFor(name, configuration);
		}

		final String message = String.format("No configuration property found for '%s'", name);
		log.warn(message);
		throw new ConfigurationException(message);
	}

	@SuppressWarnings("rawtypes")
	public Provider getProvider(final ConfigurationOptionType<?> strategy) {
		return new Provider<Object>() {
			@Inject
			private Configuration configuration;

			@Override
			public Object get() {
				return getValueFor(strategy, configuration);
			}
		};
	}



	public String getString(final Class<? extends ConfigurationOptionType<?>> key) {
		return new StringOption().getValueFor(key.getSimpleName(), configuration);
	}

	public Boolean getBoolean(final Class<? extends ConfigurationOptionType<?>> key) {
		return new BooleanOption().getValueFor(key.getSimpleName(), configuration);
	}
}
