package io.openems.common.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

class RoleTest {

	@Test
	void testAssertRoleUndefined() {
		assertThrows(OpenemsNamedException.class, () -> {
			Role.assertRoleIsAtLeast("userId", null, Role.GUEST, "resource");
		});
	}

	@Test
	void testAssertRoleLower() {
		assertThrows(OpenemsNamedException.class, () -> {
			Role.assertRoleIsAtLeast("userId", Role.OWNER, Role.INSTALLER, "resource");
		});
	}

	@Test
	void testAssertRoleSuccess() {
		assertDoesNotThrow(() -> {
			Role.assertRoleIsAtLeast("userId", Role.OWNER, Role.GUEST, "resource");
		});
	}

	@Test
	void testAssertRoleIsEqualSuccess() {
		assertDoesNotThrow(() -> {
			Role.assertRoleIsEqual("userId", Role.OWNER, Role.OWNER, "resource");
		});
	}

	@Test
	void testAssertRoleIsEqualHigher() {
		assertThrows(OpenemsNamedException.class, () -> {
			Role.assertRoleIsEqual("userId", Role.INSTALLER, Role.OWNER, "resource");
		});
	}

	@Test
	void testAssertRoleIsEqualLower() {
		assertThrows(OpenemsNamedException.class, () -> {
			Role.assertRoleIsEqual("userId", Role.GUEST, Role.OWNER, "resource");
		});
	}

}
