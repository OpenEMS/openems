package io.openems.backend.metadata.odoo.odoo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.openems.common.exceptions.OpenemsException;

public enum OdooUserRole {

	ADMIN("admin"), //
	INSTALLER("installer", OdooUserGroup.PORTAL), //
	OWNER("owner", OdooUserGroup.PORTAL), //
	GUEST("guest");

	private final String odooRole;
	private final OdooUserGroup[] odooGroups;

	OdooUserRole(String odooRole, OdooUserGroup... odooGroups) {
		this.odooRole = odooRole;
		this.odooGroups = odooGroups;
	}

	/**
	 * Get the Odoo role.
	 *
	 * @return Odoo role
	 */
	public String getOdooRole() {
		return this.odooRole;
	}

	/**
	 * Get the specified Odoo groups for the role.
	 *
	 * @return Groups for an Odoo role
	 */
	public OdooUserGroup[] getOdooGroups() {
		return this.odooGroups;
	}

	/**
	 * Transform the specified Odoo group objects to a list of IDs.
	 *
	 * @return The Odoo groups as a list of IDs.
	 */
	public List<Integer> toOdooIds() {
		return Arrays.stream(this.odooGroups) //
				.map(OdooUserGroup::getGroupId) //
				.collect(Collectors.toList());
	}

	/**
	 * Get the {@link OdooUserRole} for the given role as {@link String}.
	 *
	 * @param role as {@link String} to parse
	 * @return The Odoo role
	 * @throws OpenemsException if role does not exist
	 */
	public static OdooUserRole getRole(String role) throws OpenemsException {
		role = role.toLowerCase();

		if (role.equals("admin")) {
			return OdooUserRole.ADMIN;
		} else if (role.equals("installer")) {
			return OdooUserRole.INSTALLER;
		} else if (role.equals("owner")) {
			return OdooUserRole.OWNER;
		} else {
			throw new OpenemsException("Role [" + role + "] does not exist");
		}
	}

}
