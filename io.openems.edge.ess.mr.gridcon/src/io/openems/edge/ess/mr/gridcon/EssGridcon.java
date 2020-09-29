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
import io.openems.edge.battery.api.Battery;
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
import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState;
import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconStateObject;
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

	protected io.openems.edge.ess.mr.gridcon.StateObject mainStateObject = null;
	protected io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconStateObject gridconStateObject = null;

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
		mainStateObject = getFirstGeneralStateObjectUndefined();
		gridconStateObject = getFirstGridconStateObjectUndefined();
	}

	private GridconStateObject getFirstGridconStateObjectUndefined() {
		return StateController.getGridconStateObject(GridconState.UNDEFINED);
	}

	protected abstract StateObject getFirstGeneralStateObjectUndefined();

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

				
				// Execute state machine for general handling				
				IState nextMainState = this.mainStateObject.getNextState();
				StateObject nextMainStateObject = StateController.getGeneralStateObject(nextMainState);

				System.out.println("  ----- CURRENT STATE:" + this.mainStateObject.getState().getName());
				System.out.println("  ----- NEXT STATE:" + nextMainStateObject.getState().getName());
				System.out.println("Conditional: ");
				StateController.printCondition();

				this.mainStateObject = nextMainStateObject;

				this.mainStateObject.act();
				
				// Execute state machine for gridcon handling, parameters for the grid settings coming from the state machine object
				GridconSettings gridconSettings = this.mainStateObject.getGridconSettings();
				
				
				IState nextGridconState = this.gridconStateObject.getNextState();
				GridconStateObject nextGridconStateObject = StateController.getGridconStateObject(nextGridconState);				
				this.gridconStateObject = nextGridconStateObject;
				this.gridconStateObject.act(gridconSettings);
				
				
				
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

		if (getBattery1() != null && !Helper.isUndefined(getBattery1())) {
			minCellVoltage = Math.min(minCellVoltage, getBattery1().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery1().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery1().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery1().getMaxCellTemperature().get());
		}

		if (getBattery2() != null && !Helper.isUndefined(getBattery2())) {
			minCellVoltage = Math.min(minCellVoltage, getBattery2().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery2().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery2().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery2().getMaxCellTemperature().get());
		}

		if (getBattery3() != null && !Helper.isUndefined(getBattery3())) {
			minCellVoltage = Math.min(minCellVoltage, getBattery3().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, getBattery3().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, getBattery3().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, getBattery3().getMaxCellTemperature().get());
		}

		int minCellVoltageMilliVolt = (int) (minCellVoltage * 1000);
		int maxCellVoltageMilliVolt = (int) (maxCellVoltage * 1000);

		_setMinCellVoltage(minCellVoltageMilliVolt);
		_setMaxCellVoltage(maxCellVoltageMilliVolt);
		_setMinCellTemperature((int) minCellTemperature);
		_setMaxCellTemperature((int) maxCellTemperature);

	}

	private void calculateMaxApparentPower() {
		int maxPower = (int) getGridconPcs().getMaxApparentPower();
		_setMaxApparentPower(maxPower);
	}

	protected void calculateActiveAndReactivePower() {
		float activePower = getGridconPcs().getActivePower();
		_setActivePower((int) activePower);

		float reactivePower = getGridconPcs().getReactivePower();
		_setReactivePower((int) reactivePower);
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

		for (Battery battery : getBatteries()) {
			Integer maxChargeCurrent = Math.min(MAX_CURRENT_PER_STRING, battery.getChargeMaxCurrent().get());
			maxChargeCurrent = maxChargeCurrent - offset; // Reduce the max power by the value for the offset current
			maxChargeCurrent = Math.max(maxChargeCurrent, 0);
			allowedCharge += battery.getVoltage().get() * maxChargeCurrent * -1;

			Integer maxDischargeCurrent = Math.min(MAX_CURRENT_PER_STRING,
					battery.getDischargeMaxCurrent().get());
			maxDischargeCurrent = maxDischargeCurrent - offset; // Reduce the max power by the value for the offset current
			maxDischargeCurrent = Math.max(maxDischargeCurrent, 0);
			allowedDischarge += battery.getVoltage().get() * maxDischargeCurrent;
		}

		allowedCharge = (allowedCharge * (1 + getGridconPcs().getEfficiencyLossChargeFactor()));
		allowedDischarge = (allowedDischarge * (1 - getGridconPcs().getEfficiencyLossDischargeFactor()));

		_setAllowedChargePower((int) allowedCharge);
		_setAllowedDischargePower((int) allowedDischarge);
	}

	protected abstract void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException;

	/**
	 * Calculates the StateObject-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	protected void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (Battery b : getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().asOptional();
			Optional<Integer> socOpt = b.getSoc().asOptional();
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				_setSoc(null);
				return;
			}
			float totalCapacity = totalCapacityOpt.get();
			float soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		
		_setSoc(soc);
	}

	protected void calculateCapacity() {
		float sumTotalCapacity = 0;

		for (Battery b : getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().asOptional();
			float totalCapacity = totalCapacityOpt.orElse(0);
			sumTotalCapacity += totalCapacity;
		}

		_setCapacity((int) sumTotalCapacity);
	}

	@Override
	public String debugLog() {
		return "StateObject: " + mainStateObject.getState().getName() + "| Next StateObject: "
				+ mainStateObject.getNextState().getName();
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	protected Collection<Battery> getBatteries() {
		Collection<Battery> batteries = new ArrayList<>();
		if (getBattery1() != null && !Helper.isUndefined(getBattery1())) {
			batteries.add(getBattery1());
		}

		if (getBattery2() != null && !Helper.isUndefined(getBattery2())) {
			batteries.add(getBattery2());
		}

		if (getBattery3() != null && !Helper.isUndefined(getBattery3())) {
			batteries.add(getBattery3());
		}
		return batteries;
	}

	GridconPcs getGridconPcs() {
		return getComponent(gridconId);
	}

	Battery getBattery1() {
		return getComponent(bmsAId);
	}

	Battery getBattery2() {
		return getComponent(bmsBId);
	}

	Battery getBattery3() {
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
