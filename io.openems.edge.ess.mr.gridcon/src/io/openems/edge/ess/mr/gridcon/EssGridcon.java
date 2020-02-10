package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

//@Designate(ocd = EssGridconConfig.class, factory = true)
//@Component(//
//		name = "ESS.Gridcon", //
//		immediate = true, //
//		configurationPolicy = ConfigurationPolicy.REQUIRE, //
//		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
//		}) //
public abstract class EssGridcon extends AbstractOpenemsComponent
		implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusSlave {

	public static final String KEY_POWER_IS_ZERO = "zero";
	public static final String KEY_POWER_CHARGING = "charging";
	public static final String KEY_POWER_DISCHARGING = "discharging";

	protected GridconPCS gridconPCS;
	protected Battery batteryA;
	protected Battery batteryB;
	protected Battery batteryC;

	protected boolean enableIPU1 = false;
	protected boolean enableIPU2 = false;
	protected boolean enableIPU3 = false;

	protected ParameterSet parameterSet;

//	public EssGridconConfig config;

	StateMachine stateMachine;

protected abstract ComponentManager getComponentManager();

	protected Map<String, Map<GridConChannelId, Float>> weightingMap = initializeMap();
	protected int stringControlMode = 1;

	private final Logger log = LoggerFactory.getLogger(EssGridcon.class);


	public EssGridcon() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ErrorCodeChannelId0.values(), //
				ErrorCodeChannelId1.values()// , //
//				ChannelId.values(), //
//				otherChannelIds.values()
		);
	}

//	@Activate
	public void activate(ComponentContext context, String id, String alias, boolean enabled, boolean enableIPU1,
			boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet, String gridcon, String bmsA, String bmsB,
			String bmsC) throws OpenemsNamedException {
		super.activate(context, id, alias, enabled);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;

		calculateMaxApparentPower();
		gridconPCS = getComponentManager().getComponent(gridcon);

		try {
			batteryA = getComponentManager().getComponent(bmsA);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string a
		}

		try {
			batteryB = getComponentManager().getComponent(bmsB);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string b
		}

		try {
			batteryC = getComponentManager().getComponent(bmsC);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string c
		}
//		stateMachine = new StateMachine(gridconPCS, this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void calculateMaxApparentPower() {

		int maxPower = 0;
		if (enableIPU1) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (enableIPU2) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (enableIPU3) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}

		getMaxApparentPower().setNextValue(maxPower);
	}

	protected void calculateActivePower() {
		// Calculate Total Active Power

		float activePowerIpu1 = gridconPCS.getActivePowerInverter1();
		float activePowerIpu2 = gridconPCS.getActivePowerInverter2();
		float activePowerIpu3 = gridconPCS.getActivePowerInverter3();
		float activePower = activePowerIpu1 + activePowerIpu2 + activePowerIpu3;
		getActivePower().setNextValue(activePower);
	}

	protected int getStringControlMode() {
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

//	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
//		STATE_MACHINE(Doc.of(IState.values()) //
//				.text("Current State of State-Machine")), //
//
//		STATE_CYCLE_ERROR(Doc.of(Level.FAULT)),
//
//		; //
//
//		private final Doc doc;
//
//		private ChannelId(Doc doc) {
//			this.doc = doc;
//		}
//
//		@Override
//		public Doc doc() {
//			return doc;
//		}
//	}

	/**
	 * Sets the weights for battery strings A, B and C according to max allowed
	 * current.
	 * 
	 * @return
	 * @throws OpenemsNamedException
	 */
	protected Map<String, Map<GridConChannelId, Float>> getWeightingMap() {

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
	protected void calculateBatteryData() {
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
	protected void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		GridMode gridMode = GridMode.ON_GRID;
		getGridMode().setNextValue(gridMode);
	}

	/**
	 * Calculates the State-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	protected void calculateSoc() {
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

	public void start() {
		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(true);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(parameterSet);
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);

		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
	}

	public void runSystem() {
		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(false);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(parameterSet);
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);
		
		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
				
	}

	public void stopSystem() {
//TODO		
	}

	public boolean isRunning() {
		return gridconPCS.isRunning();
	}

	public boolean isError() {
//TODO
		return gridconPCS.isError();
	}

}
