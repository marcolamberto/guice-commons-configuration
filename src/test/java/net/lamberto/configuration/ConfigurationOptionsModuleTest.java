package net.lamberto.configuration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;

import net.lamberto.configuration.ConfigurationOptionsClassLevel.DISABLE_CONNECT;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.MAX_FILE_SIZE;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.NAME;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.OPTIONS;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.TARGET_URL;
import net.lamberto.configuration.ConfigurationOptionsExtras.CUSTOM_OPTION;
import net.lamberto.configuration.ConfigurationOptionsExtras.MISSING;
import net.lamberto.configuration.ConfigurationOptionsInterfaceLevel.HOSTNAME;
import net.lamberto.configuration.ConfigurationOptionsInterfaceLevel.PORT;
import net.lamberto.junit.GuiceJUnitRunner;
import net.lamberto.junit.GuiceJUnitRunner.GuiceModules;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules(ConfigurationOptionsModuleTest.DefaultModule.class)
public class ConfigurationOptionsModuleTest {
	private static final String NAME_VALUE = "V1";
	private static final Collection<String> OPTIONS_VALUE = ImmutableList.of("V1", "V2");
	private static final Integer PORT_VALUE = 42;
	private static final URL TARGET_URL_VALUE = newURL("http://www.gimp.org");
	private static final Boolean DISABLE_CONNECT_VALUE = Boolean.TRUE;
	private static final String HOSTNAME_VALUE = "IF!";
	private static final Long MAX_FILE_SIZE_VALUE = 1001L;


	private static final ImmutableMap<String, String> CONFIGURATION_PROPERTIES = ImmutableMap.<String, String>builder()
		.put(NAME.class.getSimpleName(), NAME_VALUE)
		.put(OPTIONS.class.getSimpleName(), Joiner.on(',').join(OPTIONS_VALUE))
		.put(PORT.class.getSimpleName(), PORT_VALUE.toString())
		.put(TARGET_URL.class.getSimpleName(), TARGET_URL_VALUE.toString())
		.put(DISABLE_CONNECT.class.getSimpleName(), DISABLE_CONNECT_VALUE.toString())
		.put(HOSTNAME.class.getSimpleName(), HOSTNAME_VALUE)
		.put(MAX_FILE_SIZE.class.getSimpleName(), MAX_FILE_SIZE_VALUE.toString())
	.build();


	public static class DefaultModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_PROPERTIES,
				ConfigurationOptionsClassLevel.class,
				ConfigurationOptionsInterfaceLevel.class
			));
		}
	}

	public static class MissingConfigurationModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new DefaultModule());
			install(new ConfigurationOptionsModule(
				CONFIGURATION_PROPERTIES,
				MISSING.class
			));
		}
	}

	public static class CustomOptionConfigurationModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new DefaultModule());
			install(new ConfigurationOptionsModule(
				ImmutableMap.of(CUSTOM_OPTION.class.getSimpleName(), "customized!"),
				CUSTOM_OPTION.class
			));
		}
	}

	public static class MultipleInstancesModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_PROPERTIES,
				NAME.class,
				OPTIONS.class,
				PORT.class,
				TARGET_URL.class
			));
			install(new ConfigurationOptionsModule(
				CONFIGURATION_PROPERTIES,
				HOSTNAME.class,
				DISABLE_CONNECT.class,
				MAX_FILE_SIZE.class
			));
		}
	}


	@Inject
	private SampleBean sampleBean;

	@Inject
	private Injector injector;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void basicUsage() {
		assertThat(sampleBean.name, is(NAME_VALUE));
		assertThat(sampleBean.options.length, is(OPTIONS_VALUE.size()));
		assertThat(sampleBean.disableConnect, is(DISABLE_CONNECT_VALUE));
		assertThat(sampleBean.hostName, is(HOSTNAME_VALUE));
		assertThat(sampleBean.port, is(PORT_VALUE));
		assertThat(sampleBean.targetUrl, is(TARGET_URL_VALUE));
		assertThat(sampleBean.maxFileSize, is(MAX_FILE_SIZE_VALUE));
	}

	@Test
	public void commonsConfigurationEnvExpansion() {
		final Configuration conf = new MapConfiguration(ImmutableMap.of("envBased", "${env:PATH}"));

		assertThat(conf.getString("envBased"), is(not("${env:PATH}")));
	}

	@Test
	@GuiceModules(MultipleInstancesModule.class)
	public void itShouldSupportMultipleConfigurations() {
		assertThat(sampleBean.name, is(NAME_VALUE));
		assertThat(sampleBean.options.length, is(OPTIONS_VALUE.size()));
		assertThat(sampleBean.disableConnect, is(DISABLE_CONNECT_VALUE));
		assertThat(sampleBean.hostName, is(HOSTNAME_VALUE));
	}

	@Test
	@GuiceModules(MissingConfigurationModule.class)
	public void itShouldThrowAnExceptionForMissingConfigurationKeys() {
		thrown.expect(ProvisionException.class);
		thrown.expectCause(isA(ConfigurationException.class));
		thrown.expectMessage("No configuration property found for 'MISSING'");

		injector.injectMembers(new Object() {
			@Inject
			@ConfigurationOption(MISSING.class)
			private String whoCares;
		});
	}

	@Test
	@GuiceModules(CustomOptionConfigurationModule.class)
	public void itShouldAllowCustomConfigurationTypes() throws Exception {
		final Object obj = new Object() {
			@Inject
			@ConfigurationOption(CUSTOM_OPTION.class)
			public Object custom;
		};

		injector.injectMembers(obj);

		assertThat(obj.getClass().getField("custom").get(obj).toString(), is("customized!"));
	}


	private static URL newURL(final String url) {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(url);
		}
	}
}
