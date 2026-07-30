package io.openems.core.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Dictionary;

import org.junit.jupiter.api.Test;
import org.osgi.service.cm.Configuration;

import io.openems.common.test.DummyConfigurationAdmin.DummyConfiguration;
import io.openems.common.utils.DictionaryUtils;

class LoggerConfiguratorTest {

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
	void currentConfigDefault() throws Exception {
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
	void currentConfigDefaultNoOverride() throws Exception {
		Configuration config = new DummyConfiguration() //
				.addProperty("log4j2.rootLogger.level", "DEBUG");

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig(""));
		assertTrue(result.isEmpty());
	}

	@Test
	void currentConfigFile() throws Exception {
		Configuration config = new DummyConfiguration();

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig("/path/to/log4j2.xml"));
		assertTrue(result.isPresent());
		final var conf = result.get();
		assertEquals("/path/to/log4j2.xml",
				DictionaryUtils.getAsString(conf, "org.ops4j.pax.logging.log4j2.config.file"));
		assertNull(DictionaryUtils.getAsString(conf, "log4j2.appender.console.type"));
	}

	@Test
	void currentConfigFileNoOverride() throws Exception {
		Configuration config = new DummyConfiguration() //
				.addProperty("org.ops4j.pax.logging.log4j2.config.file", "/path/to/log4j2.xml");

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig("/path/to/log4j2.xml"));
		assertTrue(result.isEmpty());
	}

	@Test
	void currentConfigFileWithNullProperties() {
		Configuration config = new DummyConfiguration() {
			@Override
			public Dictionary<String, Object> getProperties() {
				return null;
			}
		};

		var result = LoggerConfigurator.getCurrentConfiguration(config, testConfig("/path/to/log4j2.xml"));

		assertTrue(result.isPresent());
		assertEquals("/path/to/log4j2.xml",
				DictionaryUtils.getAsString(result.get(), "org.ops4j.pax.logging.log4j2.config.file"));
	}

}