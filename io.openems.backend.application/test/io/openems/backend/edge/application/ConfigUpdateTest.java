package io.openems.backend.edge.application;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.osgi.service.cm.Configuration;

import io.openems.backend.application.BackendApp;
import io.openems.common.test.DummyConfigurationAdmin;

public class ConfigUpdateTest {
	
	@Test
	public void testWithEmptyConfiguration() {
		final Configuration config = new DummyConfigurationAdmin.DummyConfiguration();
		
		final var update = BackendApp.getConfigUpdate(config);
		assertTrue(update.isPresent());
	}
	
	@Test
	public void testWithValidConfiguation() {
		final Configuration config = new DummyConfigurationAdmin.DummyConfiguration() //
				.addProperty("log4j2.rootLogger.level", "INFO");
		
		final var update = BackendApp.getConfigUpdate(config);
		assertTrue(update.isEmpty());
	}
	
	@Test
	public void testWithInvalidConfiguation() {
		final Configuration config = new DummyConfigurationAdmin.DummyConfiguration() //
				.addProperty("Something.random", "Hi");
		
		final var update = BackendApp.getConfigUpdate(config);
		assertTrue(update.isPresent());
	}
	
	@Test
	public void testWithNullConfiguation() {
		assertThrows(NullPointerException.class, () -> BackendApp.getConfigUpdate(null));
	}

}
