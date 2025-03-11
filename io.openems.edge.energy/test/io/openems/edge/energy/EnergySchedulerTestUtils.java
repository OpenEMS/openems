package io.openems.edge.energy;

import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import java.time.LocalTime;

import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.EnergyUtils;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChargeParams;

public class EnergySchedulerTestUtils {

	private EnergySchedulerTestUtils() {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of
	 * Controller.Ess.EmergencyCapacityReserve.
	 * 
	 * @param id         the Component-ID
	 * @param reserveSoc the configured Reserve-Soc
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode eshEmergencyCapacityReserve(String id, int reserveSoc) {
		return io.openems.edge.controller.ess.emergencycapacityreserve.EnergyScheduler //
				.buildEnergyScheduleHandler(new DummyController(id), //
						() -> reserveSoc);
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of
	 * Controller.Ess.EmergencyCapacityReserve.
	 * 
	 * @param id         the Component-ID
	 * @param reserveSoc the configured Reserve-Soc
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyEmergencyCapacityReserve(String id, int reserveSoc) {
		return new DummyEnergySchedulable(id, eshEmergencyCapacityReserve(id, reserveSoc)); //
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of
	 * Controller.Ess.EmergencyCapacityReserve with default settings.
	 * 
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyEmergencyCapacityReserve() {
		return dummyEmergencyCapacityReserve("ctrlEmergencyCapacityReserve0", 20);
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of
	 * Controller.Ess.LimitTotalDischarge.
	 * 
	 * @param id     the Component-ID
	 * @param minSoc the configured Min-Soc
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode eshLimitTotalDischarge(String id, int minSoc) {
		return io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler //
				.buildEnergyScheduleHandler(new DummyController(id), //
						() -> minSoc);
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of
	 * Controller.Ess.LimitTotalDischarge.
	 * 
	 * @param id     the Component-ID
	 * @param minSoc the configured Min-Soc
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyLimitTotalDischarge(String id, int minSoc) {
		return new DummyEnergySchedulable(id, eshLimitTotalDischarge(id, minSoc)); //
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.LimitTotalDischarge
	 * with default settings.
	 * 
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyLimitTotalDischarge() {
		return dummyLimitTotalDischarge("ctrlLimitTotalDischarge0", 20);
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of Controller.Ess.FixActivePower.
	 * 
	 * @param id           the Component-ID
	 * @param power        the configured power
	 * @param relationship the configured {@link Relationship}
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode eshFixActivePower(String id, int power,
			Relationship relationship) {
		return io.openems.edge.controller.ess.fixactivepower.EnergyScheduler //
				.buildEnergyScheduleHandler(new DummyController(id), //
						() -> new io.openems.edge.controller.ess.fixactivepower.EnergyScheduler.OptimizationContext(
								EnergyUtils.toEnergy(power), relationship));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.FixActivePower.
	 * 
	 * @param id           the Component-ID
	 * @param power        the configured power
	 * @param relationship the configured {@link Relationship}
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyFixActivePower(String id, int power, Relationship relationship) {
		return new DummyEnergySchedulable(id, eshFixActivePower(id, power, relationship)); //
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.FixActivePower with
	 * default settings.
	 * 
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyFixActivePower() {
		return dummyFixActivePower("ctrlFixActivePower0", -1000, EQUALS);
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of
	 * Controller.Ess.GridOptimizedCharge in MANUAL mode.
	 * 
	 * @param id        the Component-ID
	 * @param localTime the configured {@link LocalTime}
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode eshGridOptimizedChargeManual(String id, LocalTime localTime) {
		return io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler //
				.buildEnergyScheduleHandler(new DummyController(id), //
						() -> new io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.Config.Manual(
								localTime));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.GridOptimizedCharge
	 * in MANUAL mode.
	 * 
	 * @param id        the Component-ID
	 * @param localTime the configured {@link LocalTime}
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyGridOptimizedChargeManual(String id, LocalTime localTime) {
		return new DummyEnergySchedulable(id, eshGridOptimizedChargeManual(id, localTime)); //
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.GridOptimizedCharge
	 * in MANUAL mode with default settings.
	 * 
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyGridOptimizedChargeManual() {
		return dummyGridOptimizedChargeManual("ctrlGridOptimizedCharge0", LocalTime.of(10, 0));
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of
	 * Controller.Ess.Time-Of-Use-Tariff.
	 * 
	 * @param id          the Component-ID
	 * @param controlMode the configured {@link ControlMode}
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithDifferentModes eshTimeOfUseTariff(String id, ControlMode controlMode) {
		return io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler //
				.buildEnergyScheduleHandler(new DummyController(id), //
						() -> new io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.Config(controlMode));
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} of Evse.Controller.Single.
	 * 
	 * @param id                 the Component-ID
	 * @param mode               the actual Mode
	 * @param chargeParams       the {@link ChargeParams}
	 * @param sessionEnergyLimit the configured Session-Energy-Limit
	 * @return the {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode eshEvseSingleManual(String id,
			io.openems.edge.evse.api.chargepoint.Mode.Actual mode, ChargeParams chargeParams, int sessionEnergyLimit) {
		return io.openems.edge.controller.evse.single.EnergyScheduler //
				.buildManualEnergyScheduleHandler(new DummyController(id), //
						() -> new io.openems.edge.controller.evse.single.EnergyScheduler.ManualOptimizationContext(mode,
								true /* isReadyForCharging */, chargeParams, 0 /* sessionEnergy */,
								sessionEnergyLimit));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.Time-Of-Use-Tariff.
	 * 
	 * @param id          the Component-ID
	 * @param controlMode the configured {@link ControlMode}
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyTimeOfUseTariff(String id, ControlMode controlMode) {
		return new DummyEnergySchedulable(id, eshTimeOfUseTariff(id, controlMode)); //
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.Time-Of-Use-Tariff
	 * with default settings.
	 * 
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable dummyTimeOfUseTariff() {
		return dummyTimeOfUseTariff("ctrlEssTimeOfUseTariff0", ControlMode.CHARGE_CONSUMPTION);
	}
}
