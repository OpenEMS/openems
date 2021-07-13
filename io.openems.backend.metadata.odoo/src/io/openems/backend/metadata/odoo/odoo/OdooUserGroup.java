package io.openems.backend.metadata.odoo.odoo;

public enum OdooUserGroup {

	CUSTOMER(107);

	private final int groupId;

	private OdooUserGroup(int groupId) {
		this.groupId = groupId;
	}

	public int getGroupId() {
		return this.groupId;
	}

}
