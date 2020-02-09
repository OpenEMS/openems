package io.openems.edge.ess.mr.gridcon.ongrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.GridconPCSImpl;
import io.openems.edge.ess.mr.gridcon.StateMachine;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = EssGridconOnGridConfig.class, factory = true)
@Component(//
		name = "ESS.Gridcon.OnGrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
		}) //
public class EssGridconOngrid extends AbstractOpenemsComponent
		implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusSlave, EventHandler {

	public static final String KEY_POWER_IS_ZERO = "zero";
	public static final String KEY_POWER_CHARGING = "charging";
	public static final String KEY_POWER_DISCHARGING = "discharging";

	private GridconPCS gridconPCS;
	private Battery batteryA;
	private Battery batteryB;
	private Battery batteryC;

	public EssGridconOnGridConfig config;

	StateMachine stateMachine;

	@Reference
	private Power power;

	private Map<String, Map<GridConChannelId, Float>> weightingMap = initializeMap();
	private int stringControlMode = 1;

	private final Logger log = LoggerFactory.getLogger(EssGridconOngrid.class);

	@Reference
	protected ComponentManager componentManager;

	public EssGridconOngrid() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ErrorCodeChannelId0.values(), //
				ErrorCodeChannelId1.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, EssGridconOnGridConfig config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		calculateMaxApparentPower();
		gridconPCS = componentManager.getComponent(config.gridcon_id());

		try {
			batteryA = componentManager.getComponent(config.bms_a_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string a
		}

		try {
			batteryB = componentManager.getComponent(config.bms_b_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string b
		}

		try {
			batteryC = componentManager.getComponent(config.bms_c_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string c
		}
		stateMachine = new StateMachine(gridconPCS, this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void calculateMaxApparentPower() {

		int maxPower = 0;
		if (config.enableIPU1()) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (config.enableIPU2()) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (config.enableIPU3()) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}

		getMaxApparentPower().setNextValue(maxPower);
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
				calculateGridMode();
				calculateBatteryData();
				calculateSoc();
				calculateActivePower();

				weightingMap = getWeightingMap();
				stringControlMode = getStringControlMode();

				// start state-machine handling
				stateMachine.run();

				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(false);
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(true);
				logError(log, "State-Cycle Error: " + e.getMessage());
			}
			break;
		}
	}

	private void calculateActivePower() {
		// Calculate Total Active Power

		float activePowerIpu1 = gridconPCS.getActivePowerInverter1();
		float activePowerIpu2 = gridconPCS.getActivePowerInverter2();
		float activePowerIpu3 = gridconPCS.getActivePowerInverter3();
		float activePower = activePowerIpu1 + activePowerIpu2 + activePowerIpu3;
		getActivePower().setNextValue(activePower);
	}

	private int getStringControlMode() {
		int weightingMode = 0; // Depends on number of battery strings!!!

		// --- // TODO if battery is not ready for work, remove it from the weighting
		// mode!!

		boolean useBatteryStringA = (batteryA != null && batteryA.getReadyForWorking().value().orElse(false));
		if (useBatteryStringA) {
			weightingMode += 1; // battA = 1 (2^0)
		}
		boolean useBatteryStringB = (batteryB != null && batteryB.getReadyForWorking().value().orElse(false));
		if (useBatteryStringB) {
			weightingMode += 8; // battB = 8 (2^3)
		}
		boolean useBatteryStringC = (batteryC != null && batteryC.getReadyForWorking().value().orElse(false));
		if (useBatteryStringC) {
			weightingMode += 64; // battC = 64 (2^6)
		}

		return weightingMode;
	}

	@Override
	public Power getPower() {
		return power;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (getGridMode().value().get() != GridMode.ON_GRID.getValue() && !gridconPCS.isRunning()) {

			log.info("Gridmode nicht on grid oder ccu state nicht run!!");

			return new Constraint[] {
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		// set the weights for battery strings A, B and C.
		Map<GridConChannelId, Float> weightings = null;
		if (activePower > 0) {
			weightings = weightingMap.get(KEY_POWER_DISCHARGING);
		} else if (activePower < 0) {
			weightings = weightingMap.get(KEY_POWER_CHARGING);
		} else {
			weightings = weightingMap.get(KEY_POWER_IS_ZERO);
		}

		Float weightA = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A);
		Float weightB = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B);
		Float weightC = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C);

		// no string used...no power possibile
		if (weightA == 0 && weightB == 0 && weightC == 0) {
			return;
		}

		// at least one string has to be set
		if (stringControlMode == 0) {
			return;
		}

		gridconPCS.setPower(activePower, reactivePower);

		gridconPCS.setWeightStringA(weightA);
		gridconPCS.setWeightStringB(weightB);
		gridconPCS.setWeightStringC(weightC);

		gridconPCS.setStringControlMode(stringControlMode);

	}

	@Override
	public int getPowerPrecision() {
		return 100; // TODO estimated value
	}

	private Map<String, Map<GridConChannelId, Float>> initializeMap() {
		Map<String, Map<GridConChannelId, Float>> ret = new HashMap<>();

		Map<GridConChannelId, Float> weightZero = new HashMap<>();
		Map<GridConChannelId, Float> weightCharge = new HashMap<>();
		Map<GridConChannelId, Float> weightDischarge = new HashMap<>();

		weightZero.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		weightZero.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		weightZero.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);

		weightCharge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		weightCharge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		weightCharge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);

		weightDischarge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		weightDischarge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		weightDischarge.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);

		ret.put(KEY_POWER_IS_ZERO, weightZero);
		ret.put(KEY_POWER_CHARGING, weightCharge);
		ret.put(KEY_POWER_DISCHARGING, weightDischarge);

		return ret;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GridconPCSImpl.class, accessMode, 300) //
						.build());
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		STATE_CYCLE_ERROR(Doc.of(Level.FAULT)),

		; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return doc;
		}
	}

	/**
	 * Sets the weights for battery strings A, B and C according to max allowed
	 * current.
	 * 
	 * @return
	 * @throws OpenemsNamedException
	 */
	private Map<String, Map<GridConChannelId, Float>> getWeightingMap() {

		Map<String, Map<GridConChannelId, Float>> ret = new HashMap<>();

		Map<GridConChannelId, Float> chargeMap = new HashMap<>();
		Map<GridConChannelId, Float> dischargeMap = new HashMap<>();
		Map<GridConChannelId, Float> zeroMap = new HashMap<>();

		ret.put(KEY_POWER_CHARGING, chargeMap);
		ret.put(KEY_POWER_DISCHARGING, dischargeMap);
		ret.put(KEY_POWER_IS_ZERO, zeroMap);

		// Discharge
		if (batteryA != null && isBatteryReady(batteryA)) {
			float currentA = batteryA.getDischargeMaxCurrent().value().asOptional().orElse(0);
			float voltageA = batteryA.getVoltage().value().asOptional().orElse(0);
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		}

		if (batteryB != null && isBatteryReady(batteryB)) {
			float currentB = batteryB.getDischargeMaxCurrent().value().asOptional().orElse(0);
			float voltageB = batteryB.getVoltage().value().asOptional().orElse(0);
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		}

		if (batteryC != null && isBatteryReady(batteryC)) {
			float currentC = batteryC.getDischargeMaxCurrent().value().asOptional().orElse(0);
			float voltageC = batteryC.getVoltage().value().asOptional().orElse(0);
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
		}

		// Charge
		if (batteryA != null && isBatteryReady(batteryA)) {
			float currentA = batteryA.getChargeMaxCurrent().value().asOptional().orElse(0);
			float voltageA = batteryA.getVoltage().value().asOptional().orElse(0);
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		}

		if (batteryB != null && isBatteryReady(batteryB)) {
			float currentB = batteryB.getChargeMaxCurrent().value().asOptional().orElse(0);
			float voltageB = batteryB.getVoltage().value().asOptional().orElse(0);
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		}

		if (batteryC != null && isBatteryReady(batteryC)) {
			float currentC = batteryC.getChargeMaxCurrent().value().asOptional().orElse(0);
			float voltageC = batteryC.getVoltage().value().asOptional().orElse(0);
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
		}

		// active power is zero

		float factor = 100;
		float weightA = 0;
		float weightB = 0;
		float weightC = 0;
		if (batteryA != null && batteryB != null && batteryC != null) { // ABC
			Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
			Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
			Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();

			// Racks die abgeschalten sind dürfen nicht berücksichtigt werden
			// --> Gewichtung auf 0

			if (vAopt.isPresent() && vBopt.isPresent() && vCopt.isPresent()) {
				float averageVoltageA = vAopt.get();
				float averageVoltageB = vBopt.get();
				float averageVoltageC = vCopt.get();

				float min = Math.min(averageVoltageA, Math.min(averageVoltageB, averageVoltageC));
				if (isBatteryReady(batteryA)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(batteryB)) {
					weightB = (int) ((averageVoltageB - min) * factor);
				}
				if (isBatteryReady(batteryC)) {
					weightC = (int) ((averageVoltageC - min) * factor);
				}
			}
		} else if (batteryA != null && batteryB != null && batteryC == null) { // AB
			Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
			Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
			if (vAopt.isPresent() && vBopt.isPresent()) {
				float averageVoltageA = vAopt.get();
				float averageVoltageB = vBopt.get();
				float min = Math.min(averageVoltageA, averageVoltageB);
				if (isBatteryReady(batteryA)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(batteryB)) {
					weightB = (averageVoltageB - min) * factor;
				}
			}
		} else if (batteryA != null && batteryB == null && batteryC != null) { // AC
			Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
			Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();
			if (vAopt.isPresent() && vCopt.isPresent()) {
				float averageVoltageA = vAopt.get();
				float averageVoltageC = vCopt.get();
				float min = Math.min(averageVoltageA, averageVoltageC);
				if (isBatteryReady(batteryA)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(batteryC)) {
					weightC = (averageVoltageC - min) * factor;
				}
			}
		} else if (batteryA == null && batteryB != null && batteryC != null) { // BC
			Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
			Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();
			if (vBopt.isPresent() && vCopt.isPresent()) {
				float averageVoltageB = vBopt.get();
				float averageVoltageC = vCopt.get();
				float min = Math.min(averageVoltageB, averageVoltageC);
				if (isBatteryReady(batteryB)) {
					weightB = (averageVoltageB - min) * factor;
				}
				if (isBatteryReady(batteryC)) {
					weightC = (averageVoltageC - min) * factor;
				}
			}
		}

		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, weightA);
		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, weightB);
		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, weightC);

		return ret;
	}

	private boolean isBatteryReady(Battery battery) {
		if (battery == null) {
			return false;
		}

		Optional<Boolean> readyOpt = battery.getReadyForWorking().getNextValue().asOptional();

		return (readyOpt.isPresent() && readyOpt.get());
	}

	/**
	 * Handles Battery data, i.e. setting allowed charge/discharge power.
	 */
	void calculateBatteryData() {
		int allowedCharge = 0;
		int allowedDischarge = 0;
		int capacity = 0;

		for (Battery battery : getBatteries()) {
			allowedCharge += battery.getVoltage().value().orElse(0) * battery.getChargeMaxCurrent().value().orElse(0)
					* -1;
			allowedDischarge += battery.getVoltage().value().orElse(0)
					* battery.getDischargeMaxCurrent().value().orElse(0);
			capacity += battery.getCapacity().value().orElse(0);
		}
		getAllowedCharge().setNextValue(allowedCharge);
		getAllowedDischarge().setNextValue(allowedDischarge);
		getCapacity().setNextValue(capacity);
	}

	public boolean isAtLeastOneBatteryReady() {
		for (Battery battery : getBatteries()) {
			Optional<Boolean> val = battery.getReadyForWorking().value().asOptional();

			if (val.isPresent() && val.get()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Evaluates the Grid-Mode and sets the GRID_MODE channel accordingly.
	 * 
	 * @return
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		GridMode gridMode = GridMode.ON_GRID;
		getGridMode().setNextValue(gridMode);
	}

	/**
	 * Calculates the State-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (Battery b : getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().value().asOptional();
			Optional<Integer> socOpt = b.getSoc().value().asOptional();
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				getSoc().setNextValue(null);
				return;
			}
			int totalCapacity = totalCapacityOpt.get();
			int soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		getSoc().setNextValue(soc);
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	private Collection<Battery> getBatteries() {

		Collection<Battery> batteries = new ArrayList<>();
		if (batteryA != null) {
			batteries.add(batteryA);
		}

		if (batteryB != null) {
			batteries.add(batteryB);
		}

		if (batteryC != null) {
			batteries.add(batteryC);
		}

		return batteries;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		RUNNING(0, "Running"), //
		;

		private final int value;
		private final String name;

		private State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public void startSystem() {

		boolean enableIPU1 = config.enableIPU1();
		boolean enableIPU2 = config.enableIPU2();
		boolean enableIPU3 = config.enableIPU3();

		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(true);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(config.parameterSet());
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);

		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (config.enableIPU1()) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (config.enableIPU2()) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (config.enableIPU3()) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
	}

	public void runSystem() {

		boolean enableIPU1 = config.enableIPU1();
		boolean enableIPU2 = config.enableIPU2();
		boolean enableIPU3 = config.enableIPU3();

		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(false);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(config.parameterSet());
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);

		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (config.enableIPU1()) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (config.enableIPU2()) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (config.enableIPU3()) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
	}
}
