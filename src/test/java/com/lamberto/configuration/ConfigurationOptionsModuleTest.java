package com.lamberto.configuration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import net.lamberto.junit.GuiceJUnitRunner;
import net.lamberto.junit.GuiceJUnitRunner.GuiceModules;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules(ConfigurationOptionsModuleTest.Module.class)
public class ConfigurationOptionsModuleTest {

	public static class K1 extends ConfigurationOptionTypes.StringOption {}
	public static class K2 extends ConfigurationOptionTypes.StringArrayOption {}

	public static interface II1 {
		class I1 extends ConfigurationOptionTypes.StringOption {};
	}

	public static class OptionsHolder implements II1 {
		public static class K3 extends ConfigurationOptionTypes.BooleanOption {}
	}

	public static class Module extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				ImmutableMap.of(
					"K1", "V1",
					"K2", "V1,V2",
					"K3", "true",
					"I1", "IF!"
				),
				ConfigurationOptionsModuleTest.class,
				ConfigurationOptionsModuleTest.OptionsHolder.class
			));
		}
	}

	public static class MultipleInstancesModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				ImmutableMap.of(
					"K1", "V1",
					"K2", "V1,V2",
					"K3", "true",
					"I1", "IF!"
				),
				ConfigurationOptionsModuleTest.K1.class,
				ConfigurationOptionsModuleTest.K2.class
			));
			install(new ConfigurationOptionsModule(
				ImmutableMap.of(
					"K1", "V1",
					"K2", "V1,V2",
					"K3", "true",
					"I1", "IF!"
				),
				ConfigurationOptionsModuleTest.OptionsHolder.class
			));
		}
	}

	public static class SampleBean {
		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K1.class)
		public String k1;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K2.class)
		public String[] k2;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.OptionsHolder.K3.class)
		public Boolean k3;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.OptionsHolder.I1.class)
		public String i1;
	}

	@Inject
	private SampleBean sampleBean;

	@Test
	public void basicUsage() {
		assertThat(sampleBean.k1, is("V1"));
		assertThat(sampleBean.k2.length, is(2));
		assertThat(sampleBean.k3, is(true));
		assertThat(sampleBean.i1, is("IF!"));
	}

	@Test
	public void commonsConfigurationEnvExpansion() {
		final Configuration conf = new MapConfiguration(ImmutableMap.of("envBased", "${env:PATH}"));

		assertThat(conf.getString("envBased"), is(not("${env:PATH}")));
	}

	@Test
	@GuiceModules(ConfigurationOptionsModuleTest.MultipleInstancesModule.class)
	public void itShouldSupportMultipleConfigurations() {
		assertThat(sampleBean.k1, is("V1"));
		assertThat(sampleBean.k2.length, is(2));
		assertThat(sampleBean.k3, is(true));
		assertThat(sampleBean.i1, is("IF!"));
	}
}
