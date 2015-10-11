guice-commons-configuration
====

**Continuous Integration:** [![Build Status](https://api.travis-ci.org/marcolamberto/guice-commons-configuration.png?branch=master)](https://travis-ci.org/marcolamberto/guice-commons-configuration) <br/>

A simple way for injecting typed configuration properties.

## What is guice-commons-configuration?

guice-commons-configuration is a Guice module using Apache Commons Configuration for mapping named properties by using a type-based injection.

## Basic usage

Configuration options can be placed in classes or interfaces.
A configuration option could simply extend an existing type from `ConfigurationOptionTypes`.

```java
public static final class HOSTNAME extends ConfigurationOptionTypes.StringOption {}
public static final class PORT extends ConfigurationOptionTypes.IntegerOption {}
```

You could easily create your own typed option by implementing the `ConfigurationOptionType` interface.

```java
public class StructuredOptions {
	// ...
	
	public static StructuredOptions valueOf(String data) {
		// ... impl goes here
	}
	
	// ...
}

public static final class CUSTOM_OPTION implements ConfigurationOptionType<StructuredOptions> {
	@Override
	public Class<StructuredOptions> getConfigurationType() {
		return StructuredOptions.class;
	}

	@Override
	public StructuredOptions getValueFor(final String name, final Configuration configuration) {
		// ... building custom class instance
		return StructuredOptions.valueOf(configuration.getProperty(name));
	}
}
```

The Guice module requires a `Map<String, String>` with the configuration values and a variable list of configuration options or classes and interfaces holding them. 
Each key in the Map is named accordingly to the configuration option class. 

Every injected configuration option **must be present** in the configuration Map.

You could install as many `ConfigurationOptionsModule` as you like but you have to pay attention to the configuration option classes because Guice bindings have to be unique.

```java
// ... within a Module
// optionsMap = { HOSTNAME="github.com", PORT="80", CUSTOM_OPTION="{x:1,y:2,z:3}" }
install(new ConfigurationOptionsModule(
	optionsMap,
	HOSTNAME.class,
	PORT.class,
	AnotherClassOrInterfaceHoldingOptions.class
);
//... 
```

Injection requires the `ConfigurationOption` annotation with the configuration option class as value.


```java
public MyClass {
	@Inject @ConfigurationOption(HOSTNAME.class)
	private String hostName;

	@Inject @ConfigurationOption(PORT.class)
	private Integer port;
	
	@Inject @ConfigurationOption(CUSTOM_OPTION.class)
	private StructuredOptions options;
}
```

