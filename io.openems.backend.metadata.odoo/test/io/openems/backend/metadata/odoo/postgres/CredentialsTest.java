package io.openems.backend.metadata.odoo.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.backend.metadata.odoo.odoo.Credentials;
import io.openems.backend.metadata.odoo.odoo.MyConfig;
import io.openems.backend.metadata.odoo.odoo.Protocol;
import io.openems.common.types.DebugMode;

public class CredentialsTest {

	public static final Credentials DUMMY_ODOO_CREDENTIALS = new Credentials("http://127.0.0.1:8069", 1, "admin",
			"admin", "db");

	@Test
	void testFrom() {
		final var config = new MyConfig(Protocol.HTTP, "127.0.0.1", 8069, 1, "admin", "admin", "127.0.0.1", 8068,
				"pgUser", "pgPassword", "db", 10, 10, 10, DebugMode.OFF, false, "");

		assertEquals(DUMMY_ODOO_CREDENTIALS, Credentials.fromConfig(config));
	}

}