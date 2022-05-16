package io.openems.backend.metadata.odoo;

import java.time.ZonedDateTime;

import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.metadata.odoo.odoo.FieldValue;

public class MyEdgeUser extends EdgeUser {

	private final OdooMetadata parent;

	public MyEdgeUser(OdooMetadata parent, int id, String edgeId, String userId, int timeToWait,
			ZonedDateTime lastNotification) {
		super(id, edgeId, userId, timeToWait, lastNotification);
		this.parent = parent;
	}

	@Override
	public boolean setTimeToWait(int timeToWait) {
		boolean change = super.setTimeToWait(timeToWait);
		if (change) {
			/* Set in Odoo */
			this.parent.getOdooHandler().writeEdgeUser(this,
					new FieldValue<>(Field.EdgeDeviceUserRole.TIME_TO_WAIT, timeToWait));
		}
		return change;
	}

}
