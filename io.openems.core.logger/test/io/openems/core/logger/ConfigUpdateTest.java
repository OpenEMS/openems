package io.openems.core.logger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.osgi.service.cm.Configuration;

import io.openems.common.test.DummyConfigurationAdmin.DummyConfiguration;
import io.openems.common.utils.DictionaryUtils;

public class ConfigUpdateTest {

	private static Config testConfig(String path) {
		return new Config() {
			@Override
			public String path() {
				return path;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}
		};
	}

	@Test
	public void currentConfigDefault() throws Exception {
		Configuration config = new DummyConfiguration();

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig(""));
		assertTrue(result.isPresent());
		final var conf = result.get();
		assertEquals("Console", DictionaryUtils.getAsString(conf, "log4j2.appender.console.type"));
		assertEquals("PaxOsgi", DictionaryUtils.getAsString(conf, "log4j2.appender.paxosgi.type"));
		assertEquals("INFO", DictionaryUtils.getAsString(conf, "log4j2.rootLogger.level"));
		assertNull(DictionaryUtils.getAsString(conf, "org.ops4j.pax.logging.log4j2.config.file"));
	}

	@Test
	public void currentConfigDefaultNoOverride() throws Exception {
		Configuration config = new DummyConfiguration() //
				.addProperty("log4j2.rootLogger.level", "DEBUG");

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig(""));
		assertTrue(result.isEmpty());
	}

	@Test
	public void currentConfigFile() throws Exception {
		Configuration config = new DummyConfiguration();

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig("/path/to/log4j2.xml"));
		assertTrue(result.isPresent());
		final var conf = result.get();
		assertEquals("/path/to/log4j2.xml",
				DictionaryUtils.getAsString(conf, "org.ops4j.pax.logging.log4j2.config.file"));
		assertNull(DictionaryUtils.getAsString(conf, "log4j2.appender.console.type"));
	}

	@Test
	public void currentConfigFileNoOverride() throws Exception {
		Configuration config = new DummyConfiguration() //
				.addProperty("org.ops4j.pax.logging.log4j2.config.file", "/path/to/log4j2.xml");

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig("/path/to/log4j2.xml"));
		assertTrue(result.isEmpty());
	}

}