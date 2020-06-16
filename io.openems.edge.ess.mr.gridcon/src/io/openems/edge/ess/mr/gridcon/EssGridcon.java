package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public abstract class EssGridcon extends AbstractOpenemsComponent
		implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusSlave, EventHandler {

	public static final int MAX_CURRENT_PER_STRING = 80;

	String gridconId;
	String bmsAId;
	String bmsBId;
	String bmsCId;
	private float offsetCurrent;

	protected io.openems.edge.ess.mr.gridcon.StateObject stateObject = null;

	protected abstract ComponentManager getComponentManager();

	private final Logger log = LoggerFactory.getLogger(EssGridcon.class);

	public EssGridcon(io.openems.edge.common.channel.ChannelId[] otherChannelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ErrorCodeChannelId0.values(), //
				ErrorCodeChannelId1.values(), //
				otherChannelIds //
		);
	}

	public void activate(ComponentContext context, String id, String alias, boolean enabled, String gridconId,
			String bmsA, String bmsB, String bmsC, float offsetCurrent) throws OpenemsNamedException {

		super.activate(context, id, alias, enabled);

		this.gridconId = gridconId;
		this.bmsAId = bmsA;
		this.bmsBId = bmsB;
		this.bmsCId = bmsC;
		this.offsetCurrent = offsetCurrent;

		initializeStateController(gridconId, bmsA, bmsB, bmsC);
		stateObject = getFirstStateObjectUndefined();
	}

	protected abstract StateObject getFirstStateObjectUndefined();

	protected abstract void initializeStateController(String gridconPcs, String b1, String b2, String b3);

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// prepare calculated Channels
				calculateSoc();
				calculateCapacity();
				calculateGridMode();
				calculateActiveAndReactivePower();
				calculateMaxApparentPower();
				calculateAllowedPower();
				calculateBatteryValues();

				IState nextState = this.stateObject.getNextState();
				StateObject nextStateObject = StateController.getStateObject(nextState);

				// do not set the state undefined as state before
				// state before is only necessary (at the moment) to decide what the next
				// state is coming from undefined
				if (!this.stateObject.getState().toString().toUpperCase().contains("UNDEFINED")) {
					nextStateObject.setStateBefore(this.stateObject.getState());
				}

				System.out.println("  ----- CURRENT STATE:" + this.stateObject.getState().getName());
				System.out.println("  ----- NEXT STATE:" + nextStateObject.getState().getName());
				System.out.println("Conditional: ");
				StateController.printCondition();

				this.stateObject = nextStateObject;

				this.stateObject.act();
				this.writeStateMachineToChannel();
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				logError(log, "Error: " + e.getMessage());
			}
			break;
		}
	}

	protected abstract void writeStateMachineToChannel();

	/**
	 * calculates min/max cell voltage and temperature.
	 */
	private void calculateBatteryValues() {

		float minCellVoltage = Float.MAX_VALUE;
		float maxCellVoltage = Float.MIN_VALUE;

		float minCellTemperature = Float.MAX_VALUE;
		float maxCellTemperature = Float.MIN_VALUE;

		if (getBattery1() != null && !getBattery1().isUndefined()) {
			minCellVoltage = Math.min(minCellVoltage, getBattery1().getMinCellVoltage().value().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery1().getMaxCellVoltage().value().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery1().getMinCellTemperature().value().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery1().getMaxCellTemperature().value().get());
		}

		if (getBattery2() != null && !getBattery2().isUndefined()) {
			minCellVoltage = Math.min(minCellVoltage, getBattery2().getMinCellVoltage().value().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery2().getMaxCellVoltage().value().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery2().getMinCellTemperature().value().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery2().getMaxCellTemperature().value().get());
		}

		if (getBattery3() != null && !getBattery3().isUndefined()) {
			minCellVoltage = Math.min(minCellVoltage, getBattery3().getMinCellVoltage().value().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery3().getMaxCellVoltage().value().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery3().getMinCellTemperature().value().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery3().getMaxCellTemperature().value().get());
		}

		int minCellVoltageMilliVolt = (int) (minCellVoltage * 1000);
		int maxCellVoltageMilliVolt = (int) (maxCellVoltage * 1000);

		getMinCellVoltage().setNextValue(minCellVoltageMilliVolt);
		getMaxCellVoltage().setNextValue(maxCellVoltageMilliVolt);
		getMinCellTemperature().setNextValue(minCellTemperature);
		getMaxCellTemperature().setNextValue(maxCellTemperature);

	}

	private void calculateMaxApparentPower() {
		int maxPower = (int) getGridconPcs().getMaxApparentPower();
		getMaxApparentPower().setNextValue(maxPower);
	}

	protected void calculateActiveAndReactivePower() {
		float activePower = getGridconPcs().getActivePower();
		getActivePower().setNextValue(activePower);

		float reactivePower = getGridconPcs().getReactivePower();
		getReactivePower().setNextValue(reactivePower);
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (!getGridconPcs().isRunning()) {
			log.warn("CCU State not running!!");

			return new Constraint[] {
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		getGridconPcs().setPower(activePower, reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return GridconPcs.POWER_PRECISION_WATT;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GridconPcsImpl.class, accessMode, 300) //
						.build());
	}

	/**
	 * Handles Battery data, i.e. setting allowed charge/discharge power.
	 */
	protected void calculateAllowedPower() {
		double allowedCharge = 0;
		double allowedDischarge = 0;

		int offset = (int) Math.ceil(Math.abs(this.offsetCurrent));

		for (SoltaroBattery battery : getBatteries()) {
			Integer maxChargeCurrent = Math.min(MAX_CURRENT_PER_STRING, battery.getChargeMaxCurrent().value().get());
			maxChargeCurrent = maxChargeCurrent - offset; // Reduce the max power by the value for the offset current
			maxChargeCurrent = Math.max(maxChargeCurrent, 0);
			allowedCharge += battery.getVoltage().value().get() * maxChargeCurrent * -1;

			Integer maxDischargeCurrent = Math.min(MAX_CURRENT_PER_STRING,
					battery.getDischargeMaxCurrent().value().get());
			maxDischargeCurrent = maxDischargeCurrent - offset; // Reduce the max power by the value for the offset current
			maxDischargeCurrent = Math.max(maxDischargeCurrent, 0);
			allowedDischarge += battery.getVoltage().value().get() * maxDischargeCurrent;
		}

		allowedCharge = (allowedCharge * (1 + getGridconPcs().getEfficiencyLossChargeFactor()));
		allowedDischarge = (allowedDischarge * (1 - getGridconPcs().getEfficiencyLossDischargeFactor()));

		getAllowedCharge().setNextValue(allowedCharge);
		getAllowedDischarge().setNextValue(allowedDischarge);
	}

	protected abstract void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException;

	/**
	 * Calculates the StateObject-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	protected void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (SoltaroBattery b : getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().value().asOptional();
			Optional<Integer> socOpt = b.getSoc().value().asOptional();
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				getSoc().setNextValue(null);
				return;
			}
			float totalCapacity = totalCapacityOpt.get();
			float soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		getSoc().setNextValue(soc);
	}

	protected void calculateCapacity() {
		float sumTotalCapacity = 0;

		for (SoltaroBattery b : getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().value().asOptional();
			float totalCapacity = totalCapacityOpt.orElse(0);
			sumTotalCapacity += totalCapacity;
		}

		getCapacity().setNextValue(sumTotalCapacity);
	}

	@Override
	public String debugLog() {
		return "StateObject: " + stateObject.getState().getName() + "| Next StateObject: "
				+ stateObject.getNextState().getName();
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	protected Collection<SoltaroBattery> getBatteries() {
		Collection<SoltaroBattery> batteries = new ArrayList<>();
		if (getBattery1() != null && !getBattery1().isUndefined()) {
			batteries.add(getBattery1());
		}

		if (getBattery2() != null && !getBattery2().isUndefined()) {
			batteries.add(getBattery2());
		}

		if (getBattery3() != null && !getBattery3().isUndefined()) {
			batteries.add(getBattery3());
		}
		return batteries;
	}

	GridconPcs getGridconPcs() {
		return getComponent(gridconId);
	}

	SoltaroBattery getBattery1() {
		return getComponent(bmsAId);
	}

	SoltaroBattery getBattery2() {
		return getComponent(bmsBId);
	}

	SoltaroBattery getBattery3() {
		return getComponent(bmsCId);
	}

	<T> T getComponent(String id) {
		T component = null;
		try {
			component = getComponentManager().getComponent(id);
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return component;
	}
}
