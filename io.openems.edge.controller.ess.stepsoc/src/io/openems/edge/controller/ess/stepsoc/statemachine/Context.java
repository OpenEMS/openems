package io.openems.edge.controller.ess.stepsoc.statemachine;

import java.time.Clock;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.stepsoc.ControllerEssStepSocImpl;
import io.openems.edge.controller.ess.stepsoc.enums.Direction;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Context extends AbstractContext<ControllerEssStepSocImpl> {

	protected final ManagedSymmetricEss ess;
	protected final Direction direction;
	protected final int power;
	protected final int socStep;
	protected final int standbyTime;
	protected final Clock clock;

	public Context(ControllerEssStepSocImpl parent, //
			ManagedSymmetricEss ess, //
			Direction direction, //
			int power, //
			int socStep, //
			int standbyTime, //
			Clock clock//
	) {
		super(parent);
		this.ess = ess;
		this.direction = direction;
		this.power = power;
		this.socStep = socStep;
		this.standbyTime = standbyTime;
		this.clock = clock;
	}
}
