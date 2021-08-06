package io.openems.edge.core.user;

import java.util.Base64;

import io.openems.common.session.Role;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.user.ManagedUser;

public class UserServiceUtils {

	private static final String ALLOWED_CHARS = "A-NP-Z1-9";
	private static final int PASSWORD_LENGTH = 16;

	private static final byte[] GENERATE_PASSWORD_SALT = Base64.getDecoder().decode(
			"Q21DSFlscVFXbWsxbW5xdndMMlV3S3FDMEQ3aW9qZTExTEVvSHE0b0hPd2hCcTBtcGhIalQyZDlCNlBDWTBLZUhwMUZnWlhaeUx0Q3JabVFhSEtzOGZtb2g1ekloMTRlUmc3YQ==");

	private static final byte[] GENERATE_SALT_SALT = Base64.getDecoder().decode(
			"TDBoc1BYejVlZWcwc2ptemNYamowd3FjcEtyRzd3dG9LVjk2MllFbkkzWU00cmNNcENiQjZsWDlLZWpPZERxdHRzc3EzOWZ3bkVic2ZxUlo4ZHNUS2xFdGs0S1JnR29FWkgxYw==");

	protected static String generatePassword(Host host, String username, Role role) {
		String pw = getRolePrefix(role) //
				+ Base64.getEncoder().encodeToString(//
						generate(host, username, GENERATE_PASSWORD_SALT)) //
						.toUpperCase() //
						.replaceAll("[^" + ALLOWED_CHARS + "]", "");
		while (pw.length() < PASSWORD_LENGTH) {
			pw += pw;
		}
		return pw.substring(0, PASSWORD_LENGTH);
	}

	protected static byte[] generateSalt(Host host, String username) {
		return generate(host, username, GENERATE_SALT_SALT);
	}

	private static byte[] generate(Host host, String username, byte[] salt) {
		String hostname = host.getHostname().orElse(host.getHostnameChannel().getNextValue().orElse("UNKNOWN"));

		return ManagedUser.hashPassword((hostname + username).toCharArray(), salt, ManagedUser.ITERATIONS,
				ManagedUser.KEY_LENGTH);
	}

	private static String getRolePrefix(Role role) {
		switch (role) {
		case ADMIN:
			return "A";
		case GUEST:
		case INSTALLER:
		case OWNER:
			return "";
		}
		return "";
	}
}
