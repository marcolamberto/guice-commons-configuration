package net.lamberto.configuration;

import java.net.URL;

import com.google.inject.Inject;

import net.lamberto.configuration.ConfigurationOptionsClassLevel.DISABLE_CONNECT;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.MAX_FILE_SIZE;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.NAME;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.OPTIONS;
import net.lamberto.configuration.ConfigurationOptionsClassLevel.TARGET_URL;
import net.lamberto.configuration.ConfigurationOptionsInterfaceLevel.HOSTNAME;
import net.lamberto.configuration.ConfigurationOptionsInterfaceLevel.PORT;

public class SampleBean {
	@Inject @ConfigurationOption(NAME.class)
	public String name;

	@Inject @ConfigurationOption(OPTIONS.class)
	public String[] options;

	@Inject @ConfigurationOption(DISABLE_CONNECT.class)
	public Boolean disableConnect;

	@Inject @ConfigurationOption(HOSTNAME.class)
	public String hostName;

	@Inject @ConfigurationOption(PORT.class)
	public Integer port;

	@Inject @ConfigurationOption(TARGET_URL.class)
	public URL targetUrl;

	@Inject @ConfigurationOption(MAX_FILE_SIZE.class)
	public Long maxFileSize;
}
