package io.openems.edge.controller.ess.standby.statemachine;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.standby.ControllerEssStandby;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Context extends AbstractContext<ControllerEssStandby> {

	protected final ManagedSymmetricEss ess;
	protected final Sum sum;
	protected final LocalDate configuredStartDate;
	protected final LocalDate configuredEndDate;
	protected final DayOfWeek configuredDayOfWeek;

	/**
	 * The clock. Used to provide a mocked clock for unit tests.
	 */
	protected final Clock clock;

	public Context(ControllerEssStandby parent, ManagedSymmetricEss ess, Sum sum, LocalDate configuredStartDate,
			LocalDate configuredEndDate, DayOfWeek configuredDayOfWeek, Clock clock) {
		super(parent);
		this.ess = ess;
		this.sum = sum;
		this.configuredStartDate = configuredStartDate;
		this.configuredEndDate = configuredEndDate;
		this.configuredDayOfWeek = configuredDayOfWeek;
		this.clock = clock;
	}

}