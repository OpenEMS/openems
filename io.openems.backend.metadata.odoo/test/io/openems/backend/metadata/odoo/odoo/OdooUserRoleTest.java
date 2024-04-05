package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;

public class OdooUserRoleTest {

	@Test
	public void testGetter() {
		var role = OdooUserRole.INSTALLER;
		assertEquals("installer", role.getOdooRole());
		assertNotNull("OdooGroups should not be null", role.getOdooGroups());
		assertNotNull("OdooIds should not be null", role.toOdooIds());
	}

	@Test
	public void testGetRoleFromString() throws OpenemsException {
		assertEquals(OdooUserRole.ADMIN, OdooUserRole.getRole("admin"));
		assertEquals(OdooUserRole.ADMIN, OdooUserRole.getRole("Admin"));

		assertEquals(OdooUserRole.INSTALLER, OdooUserRole.getRole("installer"));
		assertEquals(OdooUserRole.INSTALLER, OdooUserRole.getRole("Installer"));

		assertEquals(OdooUserRole.OWNER, OdooUserRole.getRole("owner"));
		assertEquals(OdooUserRole.OWNER, OdooUserRole.getRole("Owner"));

		assertEquals(OdooUserRole.GUEST, OdooUserRole.getRole("guest"));
		assertEquals(OdooUserRole.GUEST, OdooUserRole.getRole("Guest"));

		assertThrows(OpenemsException.class, () -> OdooUserRole.getRole("Lorem ipsum"));
		assertThrows(OpenemsException.class, () -> OdooUserRole.getRole((String) null));
	}

	@Test
	public void testGetRoleFromRole() throws OpenemsException {
		assertEquals(OdooUserRole.ADMIN, OdooUserRole.getRole(Role.ADMIN));
		assertEquals(OdooUserRole.INSTALLER, OdooUserRole.getRole(Role.INSTALLER));
		assertEquals(OdooUserRole.OWNER, OdooUserRole.getRole(Role.OWNER));
		assertEquals(OdooUserRole.GUEST, OdooUserRole.getRole(Role.GUEST));

		assertThrows(OpenemsException.class, () -> OdooUserRole.getRole((Role) null));
	}

}
