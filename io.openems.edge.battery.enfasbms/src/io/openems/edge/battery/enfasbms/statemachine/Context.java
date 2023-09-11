package io.openems.edge.battery.enfasbms.statemachine;

import io.openems.edge.battery.enfasbms.EnfasBms;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<EnfasBms> {

	public Context(EnfasBms component) {
		super(component);
	}
}
