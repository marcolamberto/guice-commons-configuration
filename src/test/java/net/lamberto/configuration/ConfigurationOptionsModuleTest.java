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

import net.lamberto.configuration.ConfigurationOptionsModuleTest.InnerClass.BooleanCfg;
import net.lamberto.configuration.ConfigurationOptionsModuleTest.InnerInterface.StringCfg;
import net.lamberto.configuration.ConfigurationOptionsModuleTest.InvalidOptions.CUSTOM_OPTION;
import net.lamberto.configuration.ConfigurationOptionsModuleTest.InvalidOptions.MissingCfg;
import net.lamberto.junit.GuiceJUnitRunner;
import net.lamberto.junit.GuiceJUnitRunner.GuiceModules;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules(ConfigurationOptionsModuleTest.Module.class)
public class ConfigurationOptionsModuleTest {
	private static final String CLASS_LEVEL_STRING_CFG_VALUE = "V1";
	private static final Collection<String> CLASS_LEVEL_ARRAY_CFG_VALUE = ImmutableList.of("V1", "V2");
	private static final Integer CLASS_LEVEL_INTEGER_CFG_VALUE = 42;
	private static final URL CLASS_LEVEL_URL_CFG_VALUE = newURL("http://www.gimp.org");
	private static final Boolean INNER_CLASS_BOOLEAN_CFG_VALUE = Boolean.TRUE;
	private static final String INNER_INTERFACE_STRING_CFG_VALUE = "IF!";


	private static final ImmutableMap<String, String> CONFIGURATION_SOURCE = ImmutableMap.<String, String>builder()
		.put(ClassLevelStringCfg.class.getSimpleName(), CLASS_LEVEL_STRING_CFG_VALUE)
		.put(ClassLevelArrayCfg.class.getSimpleName(), Joiner.on(',').join(CLASS_LEVEL_ARRAY_CFG_VALUE))
		.put(ClassLevelIntegerCfg.class.getSimpleName(), CLASS_LEVEL_INTEGER_CFG_VALUE.toString())
		.put(ClassLevelURLCfg.class.getSimpleName(), CLASS_LEVEL_URL_CFG_VALUE.toString())
		.put(InnerClass.BooleanCfg.class.getSimpleName(), INNER_CLASS_BOOLEAN_CFG_VALUE.toString())
		.put(InnerInterface.StringCfg.class.getSimpleName(), INNER_INTERFACE_STRING_CFG_VALUE)
	.build();


	// class-level configuration properties
	public static class ClassLevelStringCfg extends ConfigurationOptionTypes.StringOption {}
	public static class ClassLevelArrayCfg extends ConfigurationOptionTypes.StringArrayOption {}
	public static class ClassLevelIntegerCfg extends ConfigurationOptionTypes.IntegerOption {}
	public static class ClassLevelURLCfg extends ConfigurationOptionTypes.URLOption {}

	// invalid configuration properties
	public static interface InvalidOptions {
		public static class MissingCfg extends ConfigurationOptionTypes.StringOption {}
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

	// interface-level configuration properties
	public static interface InnerInterface {
		class StringCfg extends ConfigurationOptionTypes.StringOption {};
	}

	// inner-class-level configuration properties
	public static class InnerClass implements InnerInterface {
		public static class BooleanCfg extends ConfigurationOptionTypes.BooleanOption {}
	}


	public static class Module extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				ConfigurationOptionsModuleTest.class,
				InnerClass.class
			));
		}
	}

	public static class MissingConfigurationModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new Module());
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				MissingCfg.class
			));
		}
	}

	public static class CustomOptionConfigurationModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new Module());
			install(new ConfigurationOptionsModule(
				ImmutableMap.of("CUSTOM_OPTION", "customized!"),
				CUSTOM_OPTION.class
			));
		}
	}

	public static class MultipleInstancesModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				ClassLevelStringCfg.class,
				ClassLevelArrayCfg.class,
				ClassLevelIntegerCfg.class,
				ClassLevelURLCfg.class
			));
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				InnerClass.class
			));
		}
	}


	public static class SampleBean {
		@Inject @ConfigurationOption(ClassLevelStringCfg.class)
		public String k1;

		@Inject @ConfigurationOption(ClassLevelArrayCfg.class)
		public String[] k2;

		@Inject @ConfigurationOption(BooleanCfg.class)
		public Boolean k3;

		@Inject @ConfigurationOption(StringCfg.class)
		public String i1;

		@Inject @ConfigurationOption(ClassLevelIntegerCfg.class)
		public Integer intCfg;

		@Inject @ConfigurationOption(ClassLevelURLCfg.class)
		public URL urlCfg;
	}


	@Inject
	private SampleBean sampleBean;

	@Inject
	private Injector injector;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void basicUsage() {
		assertThat(sampleBean.k1, is(CLASS_LEVEL_STRING_CFG_VALUE));
		assertThat(sampleBean.k2.length, is(CLASS_LEVEL_ARRAY_CFG_VALUE.size()));
		assertThat(sampleBean.k3, is(INNER_CLASS_BOOLEAN_CFG_VALUE));
		assertThat(sampleBean.i1, is(INNER_INTERFACE_STRING_CFG_VALUE));
		assertThat(sampleBean.intCfg, is(CLASS_LEVEL_INTEGER_CFG_VALUE));
		assertThat(sampleBean.urlCfg, is(CLASS_LEVEL_URL_CFG_VALUE));
	}

	@Test
	public void commonsConfigurationEnvExpansion() {
		final Configuration conf = new MapConfiguration(ImmutableMap.of("envBased", "${env:PATH}"));

		assertThat(conf.getString("envBased"), is(not("${env:PATH}")));
	}

	@Test
	@GuiceModules(MultipleInstancesModule.class)
	public void itShouldSupportMultipleConfigurations() {
		assertThat(sampleBean.k1, is(CLASS_LEVEL_STRING_CFG_VALUE));
		assertThat(sampleBean.k2.length, is(CLASS_LEVEL_ARRAY_CFG_VALUE.size()));
		assertThat(sampleBean.k3, is(INNER_CLASS_BOOLEAN_CFG_VALUE));
		assertThat(sampleBean.i1, is(INNER_INTERFACE_STRING_CFG_VALUE));
	}

	@Test
	@GuiceModules(MissingConfigurationModule.class)
	public void itShouldThrowAnExceptionForMissingConfigurationKeys() {
		thrown.expect(ProvisionException.class);
		thrown.expectCause(isA(ConfigurationException.class));
		thrown.expectMessage("No configuration property found for 'MissingCfg'");

		injector.injectMembers(new Object() {
			@Inject
			@ConfigurationOption(MissingCfg.class)
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
