package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
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

	private String gridconId;
	private String bmsAId;
	private String bmsBId;
	private String bmsCId;
	private float offsetCurrent;

	protected io.openems.edge.ess.mr.gridcon.StateObject mainStateObject = null;
	protected io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconStateObject gridconStateObject = null;

	protected abstract ComponentManager getComponentManager();

	private final Logger log = LoggerFactory.getLogger(EssGridcon.class);

	protected final StateController stateController = new StateController();

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

	@Activate
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String gridconId,
			String bmsA, String bmsB, String bmsC, float offsetCurrent) throws OpenemsNamedException {

		super.activate(context, id, alias, enabled);

		this.gridconId = gridconId;
		this.bmsAId = bmsA;
		this.bmsBId = bmsB;
		this.bmsCId = bmsC;
		this.offsetCurrent = offsetCurrent;

		this.initializeStateController(gridconId, bmsA, bmsB, bmsC);
		this.mainStateObject = this.getFirstGeneralStateObjectUndefined();
		this.gridconStateObject = this.getFirstGridconStateObjectUndefined();
	}

	private GridconStateObject getFirstGridconStateObjectUndefined() {
		return this.stateController.getGridconStateObject(GridconState.UNDEFINED);
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

		if (this.getGridconPcs() == null) {
			this.log.error("Gridcon Component with ID [" + this.gridconId + "] is not found!");
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateActiveAndReactivePower();
			this.calculateMaxApparentPower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// prepare calculated Channels
				this.calculateSoc();
				this.calculateCapacity();
				this.calculateGridMode();
				this.calculateAllowedPower();
				this.calculateBatteryValues();

				// Execute state machine for general handling
				IState nextMainState = this.mainStateObject.getNextState();
				StateObject nextMainStateObject = this.stateController.getGeneralStateObject(nextMainState);

				System.out.println("  ----- CURRENT STATE:" + this.mainStateObject.getState().getName());
				System.out.println("  ----- NEXT STATE:" + nextMainStateObject.getState().getName());
				System.out.println("Conditional: ");
				this.stateController.printCondition();

				this.mainStateObject = nextMainStateObject;

				this.mainStateObject.act();

				// Execute state machine for gridcon handling, parameters for the grid settings
				// coming from the state machine object
				GridconSettings gridconSettings = this.mainStateObject.getGridconSettings();

				IState nextGridconState = this.gridconStateObject.getNextState();
				GridconStateObject nextGridconStateObject = this.stateController
						.getGridconStateObject(nextGridconState);
				this.gridconStateObject = nextGridconStateObject;
				this.gridconStateObject.act(gridconSettings);

				this.writeStateMachineToChannel();

			} catch (IllegalArgumentException | OpenemsNamedException e) {
				this.logError(this.log, "Error: " + e.getMessage());
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

		if (this.getBattery1() != null && !Helper.isUndefined(this.getBattery1())) {
			minCellVoltage = Math.min(minCellVoltage, this.getBattery1().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, this.getBattery1().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, this.getBattery1().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, this.getBattery1().getMaxCellTemperature().get());
		}

		if (this.getBattery2() != null && !Helper.isUndefined(this.getBattery2())) {
			minCellVoltage = Math.min(minCellVoltage, this.getBattery2().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, this.getBattery2().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, this.getBattery2().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, this.getBattery2().getMaxCellTemperature().get());
		}

		if (this.getBattery3() != null && !Helper.isUndefined(this.getBattery3())) {
			minCellVoltage = Math.min(minCellVoltage, this.getBattery3().getMinCellVoltage().get());
			maxCellVoltage = Math.max(maxCellVoltage, this.getBattery3().getMaxCellVoltage().get());
			minCellTemperature = Math.min(minCellTemperature, this.getBattery3().getMinCellTemperature().get());
			maxCellTemperature = Math.max(maxCellTemperature, this.getBattery3().getMaxCellTemperature().get());
		}

		int minCellVoltageMilliVolt = (int) (minCellVoltage * 1000);
		int maxCellVoltageMilliVolt = (int) (maxCellVoltage * 1000);

		_setMinCellVoltage(minCellVoltageMilliVolt);
		_setMaxCellVoltage(maxCellVoltageMilliVolt);
		_setMinCellTemperature((int) minCellTemperature);
		_setMaxCellTemperature((int) maxCellTemperature);

	}

	private void calculateMaxApparentPower() {
		int maxPower = (int) this.getGridconPcs().getMaxApparentPower();
		_setMaxApparentPower(maxPower);
	}

	protected void calculateActiveAndReactivePower() {
		float activePower = this.getGridconPcs().getActivePower();
		_setActivePower((int) activePower);

		float reactivePower = this.getGridconPcs().getReactivePower();
		_setReactivePower((int) reactivePower);
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (this.getGridconPcs() == null || !this.getGridconPcs().isRunning()) {
			return new Constraint[] {
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.getGridconPcs().setPower(activePower, reactivePower);
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

		for (Battery battery : this.getBatteries()) {
			Integer maxChargeCurrent = Math.min(MAX_CURRENT_PER_STRING, battery.getChargeMaxCurrent().get());
			maxChargeCurrent = maxChargeCurrent - offset; // Reduce the max power by the value for the offset current
			maxChargeCurrent = Math.max(maxChargeCurrent, 0);
			allowedCharge += battery.getVoltage().get() * maxChargeCurrent * -1;

			Integer maxDischargeCurrent = Math.min(MAX_CURRENT_PER_STRING, battery.getDischargeMaxCurrent().get());
			maxDischargeCurrent = maxDischargeCurrent - offset;
			// Reduce the max power by the value for the offset current
			maxDischargeCurrent = Math.max(maxDischargeCurrent, 0);
			allowedDischarge += battery.getVoltage().get() * maxDischargeCurrent;
		}

		allowedCharge = (allowedCharge * (1 + this.getGridconPcs().getEfficiencyLossChargeFactor()));
		allowedDischarge = (allowedDischarge * (1 - this.getGridconPcs().getEfficiencyLossDischargeFactor()));

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
		for (Battery b : this.getBatteries()) {
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

		for (Battery b : this.getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().asOptional();
			float totalCapacity = totalCapacityOpt.orElse(0);
			sumTotalCapacity += totalCapacity;
		}

		_setCapacity((int) sumTotalCapacity);
	}

	@Override
	public String debugLog() {
		return "StateObject: " + this.mainStateObject.getState().getName() + "| Next StateObject: "
				+ this.mainStateObject.getNextState().getName();
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	protected Collection<Battery> getBatteries() {
		Collection<Battery> batteries = new ArrayList<>();
		if (this.getBattery1() != null && !Helper.isUndefined(this.getBattery1())) {
			batteries.add(this.getBattery1());
		}

		if (this.getBattery2() != null && !Helper.isUndefined(this.getBattery2())) {
			batteries.add(this.getBattery2());
		}

		if (this.getBattery3() != null && !Helper.isUndefined(this.getBattery3())) {
			batteries.add(this.getBattery3());
		}
		return batteries;
	}

	GridconPcs getGridconPcs() {
		return this.getComponent(this.gridconId);
	}

	Battery getBattery1() {
		return this.getComponent(this.bmsAId);
	}

	Battery getBattery2() {
		return this.getComponent(this.bmsBId);
	}

	Battery getBattery3() {
		return this.getComponent(this.bmsCId);
	}

	<T> T getComponent(String id) {
		T component = null;
		try {
			component = this.getComponentManager().getComponent(id);
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return component;
	}
}
