package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.system.fenecon.industrial.s.SystemFeneconIndustrialSImpl;

public class Context extends AbstractContext<SystemFeneconIndustrialSImpl> {

	protected final SymmetricEss ess;

	public Context(SystemFeneconIndustrialSImpl parent, SymmetricEss ess) {
		super(parent);
		this.ess = ess;
	}
}
