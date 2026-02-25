package io.openems.common.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class RoleTest {

	@Test(expected = OpenemsNamedException.class)
	public void testAssertRoleUndefined() throws Exception {
		Role.assertRole("userId", null, Role.GUEST, "resource");
	}

	@Test(expected = OpenemsNamedException.class)
	public void testAssertRoleLower() throws Exception {
		Role.assertRole("userId", Role.OWNER, Role.INSTALLER, "resource");
	}

	@Test
	public void testAssertRoleSuccess() throws Exception {
		assertDoesNotThrow(() -> {
			Role.assertRole("userId", Role.OWNER, Role.GUEST, "resource");
		});
	}

}
