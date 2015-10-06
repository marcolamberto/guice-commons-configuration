package com.lamberto.configuration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import net.lamberto.junit.GuiceJUnitRunner;
import net.lamberto.junit.GuiceJUnitRunner.GuiceModules;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules(ConfigurationOptionsModuleTest.Module.class)
public class ConfigurationOptionsModuleTest {
	private static final String K1_VALUE = "V1";
	private static final Collection<String> K2_VALUE = ImmutableList.of("V1", "V2");
	private static final Boolean K3_VALUE = Boolean.TRUE;
	private static final String I1_VALUE = "IF!";

	private static final ImmutableMap<String, String> CONFIGURATION_SOURCE = ImmutableMap.of(
		"K1", K1_VALUE,
		"K2", Joiner.on(',').join(K2_VALUE),
		"K3", K3_VALUE.toString(),
		"I1", I1_VALUE
	);


	// class-level configuration properties
	public static class K1 extends ConfigurationOptionTypes.StringOption {}
	public static class K2 extends ConfigurationOptionTypes.StringArrayOption {}

	// interface-level configuration properties
	public static interface II1 {
		class I1 extends ConfigurationOptionTypes.StringOption {};
	}

	// inner-class-level configuration properties
	public static class OptionsHolder implements II1 {
		public static class K3 extends ConfigurationOptionTypes.BooleanOption {}
	}


	public static class Module extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				ConfigurationOptionsModuleTest.class,
				ConfigurationOptionsModuleTest.OptionsHolder.class
			));
		}
	}

	public static class MultipleInstancesModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				ConfigurationOptionsModuleTest.K1.class,
				ConfigurationOptionsModuleTest.K2.class
			));
			install(new ConfigurationOptionsModule(
				CONFIGURATION_SOURCE,
				ConfigurationOptionsModuleTest.OptionsHolder.class
			));
		}
	}


	public static class SampleBean {
		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K1.class)
		public String k1;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K2.class)
		public String[] k2;

		/*
		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K2.class)
		public Collection<String> k2collection;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K2.class)
		public List<String> k2list;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.K2.class)
		public Set<String> k2set;
		*/

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.OptionsHolder.K3.class)
		public Boolean k3;

		@Inject @ConfigurationOption(ConfigurationOptionsModuleTest.OptionsHolder.I1.class)
		public String i1;
	}


	@Inject
	private SampleBean sampleBean;


	@Test
	public void basicUsage() {
		assertThat(sampleBean.k1, is(K1_VALUE));
		assertThat(sampleBean.k2.length, is(K2_VALUE.size()));
		assertThat(sampleBean.k3, is(K3_VALUE));
		assertThat(sampleBean.i1, is(I1_VALUE));
	}

	@Test
	public void commonsConfigurationEnvExpansion() {
		final Configuration conf = new MapConfiguration(ImmutableMap.of("envBased", "${env:PATH}"));

		assertThat(conf.getString("envBased"), is(not("${env:PATH}")));
	}

	@Test
	@GuiceModules(ConfigurationOptionsModuleTest.MultipleInstancesModule.class)
	public void itShouldSupportMultipleConfigurations() {
		assertThat(sampleBean.k1, is(K1_VALUE));
		assertThat(sampleBean.k2.length, is(K2_VALUE.size()));
		assertThat(sampleBean.k3, is(K3_VALUE));
		assertThat(sampleBean.i1, is(I1_VALUE));
	}
}
