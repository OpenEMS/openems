package io.openems.edge.controller.ess.timeofusetariff.v1;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Deprecated
public class EnergyScheduleHandlerV1 {

	public static record ContextV1(List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves,
			List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssLimiter14a> ctrlLimiter14as, ManagedSymmetricEss ess, ControlMode controlMode,
			int maxChargePowerFromGrid) {
	}

	private final Supplier<StateMachine[]> availableStates;
	private final Supplier<ContextV1> context;

	private ImmutableSortedMap<ZonedDateTime, Period<StateMachine>> schedule = ImmutableSortedMap.of();

	public EnergyScheduleHandlerV1(Supplier<StateMachine[]> availableStates, Supplier<ContextV1> context) {
		this.availableStates = availableStates;
		this.context = context;
	}

	/**
	 * Gets the available States.
	 * 
	 * @return an Array of States
	 */
	public StateMachine[] getAvailableStates() {
		return this.availableStates.get();
	}

	/**
	 * Gets the Context.
	 * 
	 * @return the Context
	 */
	public ContextV1 getContext() {
		return this.context.get();
	}

	public static record Period<STATE>(STATE state, Integer essChargeInChargeGrid) {
	}

	/**
	 * Sets the Schedule. Called by Optimizer.
	 * 
	 * @param schedule the Schedule
	 */
	public synchronized void setSchedule(ImmutableSortedMap<ZonedDateTime, Period<StateMachine>> schedule) {
		this.schedule = schedule;
	}

	/**
	 * Gets the current State or null.
	 * 
	 * @return the State or null
	 */
	public synchronized StateMachine getCurrentState() {
		return Optional.ofNullable(this.schedule.get(roundDownToQuarter(ZonedDateTime.now()))) //
				.map(Period::state) //
				.orElse(null);
	}

	/**
	 * Gets the current essChargeInChargeGrid or null.
	 * 
	 * @return the essChargeInChargeGrid or null
	 */
	public synchronized Integer getCurrentEssChargeInChargeGrid() {
		return Optional.ofNullable(this.schedule.get(roundDownToQuarter(ZonedDateTime.now()))) //
				.map(Period::essChargeInChargeGrid) //
				.orElse(null);
	}

}
