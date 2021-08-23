package io.openems.backend.metadata.odoo.odoo;

public enum OdooUserGroup {

	// XXX PORTAL id might differ between different Odoo databases
	PORTAL(65);

	private final int groupId;

	private OdooUserGroup(int groupId) {
		this.groupId = groupId;
	}

	public int getGroupId() {
		return this.groupId;
	}

}
