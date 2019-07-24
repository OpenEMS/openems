package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.writeutils.DcdcControl;
import io.openems.edge.ess.mr.gridcon.writeutils.IpuInverterControl;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class GridconPCS extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	public static final float DC_LINK_VOLTAGE_SETPOINT = 800f;
	public static final float DC_LINK_VOLTAGE_TOLERANCE_VOLT = 20;

	public static final int MAX_POWER_PER_INVERTER = 41_900; // experimentally measured
	
	public static final float ON_GRID_FREQUENCY_FACTOR = 1.035f;
	public static final float ON_GRID_VOLTAGE_FACTOR = 0.97f;

	protected static final float OFF_GRID_FREQUENCY_FACTOR = 1.012f;
	protected static final float OFF_GRID_VOLTAGE_FACTOR = 1.0f;
		
	private final Logger log = LoggerFactory.getLogger(GridconPCS.class);

	// State-Machines
	private final StateMachine stateMachine;

	protected Config config;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	public GridconPCS() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ErrorCodeChannelId0.values(), //
				ErrorCodeChannelId1.values(), //
				GridConChannelId.values() //
		);
		this.stateMachine = new StateMachine(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;

		// Calculate max apparent power from number of inverters
		this.getMaxApparentPower().setNextValue(config.inverterCount().getMaxApparentPower());

		// Call parent activate()
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
		
	}
	

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The main entry point for the Gridcon PCS implementation. Everything starts at
	 * the TOPIC_CYCLE_AFTER_PROCESS_IMAGE event.
	 * 
	 * <p>
	 * Prepares some default channels, than handles the central State-Machine.
	 */
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// prepare calculated Channels
				this.calculateGridMode();
				this.calculateBatteryData();
				this.calculateSoc();

				// start state-machine handling
				this.stateMachine.run();

				this.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(false);
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				this.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(true);
				this.logError(this.log, "State-Cycle Error: " + e.getMessage());
			}
			break;
		}
	}

	/**
	 * if parameter is true, the contactor will be closed if it's false, the
	 * contactor will be opened and Mr Gridcon is going to start
	 * 
	 * @param closed
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	public void setHardResetContactor(boolean closed) throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel channelHardReset = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.outputMRHardReset()));
		channelHardReset.setNextWriteValue(closed);
	}

	/**
	 * Evaluates the Grid-Mode and sets the GRID_MODE channel accordingly.
	 * 
	 * @return
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		GridMode gridMode = GridMode.UNDEFINED;
		try {
			BooleanReadChannel inputNAProtection1 = this.componentManager
					.getChannel(ChannelAddress.fromString(this.config.inputNAProtection1()));
			BooleanReadChannel inputNAProtection2 = this.componentManager
					.getChannel(ChannelAddress.fromString(this.config.inputNAProtection2()));

			Optional<Boolean> isInputNAProtection1 = inputNAProtection1.value().asOptional();
			Optional<Boolean> isInputNAProtection2 = inputNAProtection2.value().asOptional();

			if (!isInputNAProtection1.isPresent() || !isInputNAProtection2.isPresent()) {
				gridMode = GridMode.UNDEFINED;
			} else {
				if (isInputNAProtection1.get() && isInputNAProtection2.get()) {
					gridMode = GridMode.ON_GRID;
				} else {
					gridMode = GridMode.OFF_GRID;
				}
			}

		} finally {
			this.getGridMode().setNextValue(gridMode);
		}
	}

	/**
	 * Handles Battery data, i.e. setting allowed charge/discharge power.
	 */
	private void calculateBatteryData() {
		int allowedCharge = 0;
		int allowedDischarge = 0;
		int capacity = 0;
		for (Battery battery : this.getBatteries()) {
			allowedCharge += battery.getVoltage().value().orElse(0) * battery.getChargeMaxCurrent().value().orElse(0)
					* -1;
			allowedDischarge += battery.getVoltage().value().orElse(0)
					* battery.getDischargeMaxCurrent().value().orElse(0);
			capacity += battery.getCapacity().value().orElse(0);
		}
		this.getAllowedCharge().setNextValue(allowedCharge);
		this.getAllowedDischarge().setNextValue(allowedDischarge);
		this.getCapacity().setNextValue(capacity);
	}

	/**
	 * Calculates the State-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	private void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (Battery b : this.getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().value().asOptional();
			Optional<Integer> socOpt = b.getSoc().value().asOptional();
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				this.getSoc().setNextValue(null);
				return;
			}
			int totalCapacity = totalCapacityOpt.get();
			int soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		this.getSoc().setNextValue(soc);
	}

	/**
	 * Sets the IPU-settings for Inverter-Control and DCDC-Control.
	 * 
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	public void setIpuControlSettings() throws IllegalArgumentException, OpenemsNamedException {
		InverterCount inverterCount = this.config.inverterCount();
		switch (inverterCount) {
		case ONE:
			new IpuInverterControl() //
					.pMaxCharge(inverterCount.getMaxApparentPower()) //
					.pMaxDischarge(inverterCount.getMaxApparentPower() * -1) //
					.writeToChannels(this, IpuInverterControl.Inverter.ONE);
			break;

		case TWO:
			new IpuInverterControl() //
					.pMaxCharge(inverterCount.getMaxApparentPower()) //
					.pMaxDischarge(inverterCount.getMaxApparentPower() * -1) //
					.writeToChannels(this, IpuInverterControl.Inverter.ONE) //
					.writeToChannels(this, IpuInverterControl.Inverter.TWO); //
			break;

		case THREE:
			new IpuInverterControl() //
					.pMaxCharge(inverterCount.getMaxApparentPower()) //
					.pMaxDischarge(inverterCount.getMaxApparentPower() * -1) //
					.writeToChannels(this, IpuInverterControl.Inverter.ONE) //
					.writeToChannels(this, IpuInverterControl.Inverter.TWO) //
					.writeToChannels(this, IpuInverterControl.Inverter.THREE);
			break;
		}
		
	}
	
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (this.stateMachine.getState() != StateMachine.State.ONGRID) {
			return new Constraint[] {
					this.createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.stateMachine.getOngridHandler().getState() != OngridHandler.State.RUN) {
			// stop if not in On-Grid and Running -> Pref and Qref = 0 by
			// CommandControlRegisters
			return;
		}

		float maxApparentPower = this.config.inverterCount().getMaxApparentPower();
		/*
		 * !! signum, MR calculates negative values as discharge, positive as charge.
		 * Gridcon sets the (dis)charge according to a percentage of the
		 * MAX_APPARENT_POWER. So 0.1 => 10% of max power. Values should never take
		 * values lower than -1 or higher than 1.
		 */
		float activePowerFactor = -activePower / maxApparentPower;
		float reactivePowerFactor = -reactivePower / maxApparentPower;

		FloatWriteChannel pRefChannel = this.channel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF);
		FloatWriteChannel qRefChannel = this.channel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF);
		pRefChannel.setNextWriteValue(activePowerFactor);
		qRefChannel.setNextWriteValue(reactivePowerFactor);
		
		// calculate and set the weights for battery strings A, B and C.
		Map<GridConChannelId, Float> map = this.weightBatteryStrings(activePower);
		
		new DcdcControl()
		.dcVoltageSetpoint(GridconPCS.DC_LINK_VOLTAGE_SETPOINT)
		.weightStringA(map.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A))
		.weightStringB(map.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B))
		.weightStringC(map.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C))
		.iRefStringA(0)
		.iRefStringB(0)
		.iRefStringC(0)
		.stringControlMode(componentManager, config)
		.writeToChannels(this);
		
	}

	/**
	 * Sets the weights for battery strings A, B and C according to max allowed
	 * current.
	 * 
	 * @param activePower
	 * @return 
	 * @throws OpenemsNamedException
	 */
	private Map<GridConChannelId, Float> weightBatteryStrings(int activePower) throws OpenemsNamedException {
		int weightA = 0;
		int weightB = 0;
		int weightC = 0;

		Battery batteryStringA = null;
		Battery batteryStringB = null;
		Battery batteryStringC = null;

		// TODO if battery is not ready for work, set weight to '0'
		
		try {
			batteryStringA = this.componentManager.getComponent(this.config.batteryStringA_id());
		} catch (Exception e) {
			// Exception needs not to be handled because batteries are allowed to be null
		}
		try {
			batteryStringB = this.componentManager.getComponent(this.config.batteryStringB_id());
		} catch (Exception e) {
			// Exception needs not to be handled because batteries are allowed to be null
		}
		try {
			batteryStringC = this.componentManager.getComponent(this.config.batteryStringC_id());
		} catch (Exception e) {
			// Exception needs not to be handled because batteries are allowed to be null
		}
		if (activePower > 0) {
			/*
			 * Discharge
			 */
			if (batteryStringA != null) {
				weightA = batteryStringA.getDischargeMaxCurrent().value().asOptional().orElse(0);
				// if battery is not ready or if minSoc is reached, do not allow further discharging
				if (!this.isBatteryReady(batteryStringA) || batteryStringA.getSoc().value().asOptional().orElse(0) <= this.config.minSocBatteryA()) {
					weightA = 0;
				}
			}

			if (batteryStringB != null) {
				weightB = batteryStringB.getDischargeMaxCurrent().value().asOptional().orElse(0);
				// if battery is not ready or if minSoc is reached, do not allow further discharging
				if (!this.isBatteryReady(batteryStringB) || batteryStringB.getSoc().value().asOptional().orElse(0) <= this.config.minSocBatteryB()) {
					weightB = 0;
				}
			}

			if (batteryStringC != null) {
				weightC = batteryStringC.getDischargeMaxCurrent().value().asOptional().orElse(0);
				// if battery is not ready or if minSoc is reached, do not allow further discharging
				if (!this.isBatteryReady(batteryStringC) || batteryStringC.getSoc().value().asOptional().orElse(0) <= this.config.minSocBatteryC()) {
					weightC = 0;
				}
			}

		} else if (activePower < 0) {
			/*
			 * Charge
			 */
			if (batteryStringA != null && this.isBatteryReady(batteryStringA)) {
				weightA = batteryStringA.getChargeMaxCurrent().value().asOptional().orElse(0);
			}
			if (batteryStringB != null && this.isBatteryReady(batteryStringB)) {
				weightB = batteryStringB.getChargeMaxCurrent().value().asOptional().orElse(0);
			}
			if (batteryStringC != null && this.isBatteryReady(batteryStringC)) {
				weightC = batteryStringC.getChargeMaxCurrent().value().asOptional().orElse(0);
			}

		} else {
			/*
			 * active power is zero
			 */
			
			int factor = 100;
			if (batteryStringA != null && batteryStringB != null && batteryStringC != null) { // ABC
				Optional<Integer> vAopt = batteryStringA.getVoltage().value().asOptional();
				Optional<Integer> vBopt = batteryStringB.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryStringC.getVoltage().value().asOptional();
				
				
				// Racks die abgeschalten sind dürfen nicht berücksichtigt werden
				//--> Gewichtung auf 0
				
				
				if (vAopt.isPresent() && vBopt.isPresent() && vCopt.isPresent()) {
					double averageVoltageA = vAopt.get() / this.config.weightFactorBatteryA();
					double averageVoltageB = vBopt.get() / this.config.weightFactorBatteryB();
					double averageVoltageC = vCopt.get() / this.config.weightFactorBatteryC();
					
					double min = Math.min(averageVoltageA, Math.min(averageVoltageB, averageVoltageC));
					if (this.isBatteryReady(batteryStringA)) {
						weightA = (int) ((averageVoltageA - min) * factor);
					}
					if (this.isBatteryReady(batteryStringB)) {
						weightB = (int) ((averageVoltageB - min) * factor);
					}
					if (this.isBatteryReady(batteryStringC)) {
						weightC = (int) ((averageVoltageC - min) * factor);
					}
				}
			} else if (batteryStringA != null && batteryStringB != null && batteryStringC == null) { // AB
				Optional<Integer> vAopt = batteryStringA.getVoltage().value().asOptional();
				Optional<Integer> vBopt = batteryStringB.getVoltage().value().asOptional();
				if (vAopt.isPresent() && vBopt.isPresent()) {
					double averageVoltageA = vAopt.get() / this.config.weightFactorBatteryA();
					double averageVoltageB = vBopt.get() / this.config.weightFactorBatteryB();
					double min = Math.min(averageVoltageA, averageVoltageB);
					if (this.isBatteryReady(batteryStringA)) {
						weightA = (int) ((averageVoltageA - min) * factor);
					}
					if (this.isBatteryReady(batteryStringB)) {
						weightB = (int) ((averageVoltageB - min) * factor);
					}
				}
			} else if (batteryStringA != null && batteryStringB == null && batteryStringC != null) { // AC
				Optional<Integer> vAopt = batteryStringA.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryStringC.getVoltage().value().asOptional();
				if (vAopt.isPresent() && vCopt.isPresent()) {
					double averageVoltageA = vAopt.get() / this.config.weightFactorBatteryA();
					double averageVoltageC = vCopt.get() / this.config.weightFactorBatteryC();
					double min = Math.min(averageVoltageA, averageVoltageC);
					if (this.isBatteryReady(batteryStringA)) {
						weightA = (int) ((averageVoltageA - min) * factor);
					}
					if (this.isBatteryReady(batteryStringC)) {
						weightC = (int) ((averageVoltageC - min) * factor);
					}
				}
			} else if (batteryStringA == null && batteryStringB != null && batteryStringC != null) { // BC
				Optional<Integer> vBopt = batteryStringB.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryStringC.getVoltage().value().asOptional();
				if (vBopt.isPresent() && vCopt.isPresent()) {
					double averageVoltageB = vBopt.get() / this.config.weightFactorBatteryB();
					double averageVoltageC = vCopt.get() / this.config.weightFactorBatteryC();
					double min = Math.min(averageVoltageB, averageVoltageC);
					if (this.isBatteryReady(batteryStringB)) {
						weightB = (int) ((averageVoltageB - min) * factor);
					}
					if (this.isBatteryReady(batteryStringC)) {
						weightC = (int) ((averageVoltageC - min) * factor);
					}
				}
			}
		}
		
//		FloatWriteChannel weightAchannel = this.channel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A);
//		weightAchannel.setNextWriteValue(Float.valueOf(weightA));
//		FloatWriteChannel weightBchannel = this.channel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B);
//		weightBchannel.setNextWriteValue(Float.valueOf(weightB));
//		FloatWriteChannel weightCchannel = this.channel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C);
//		weightCchannel.setNextWriteValue(Float.valueOf(weightC));
		
		Map<GridConChannelId, Float> map = new HashMap<>();
		map.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, (float) weightA);
		map.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, (float) weightB);
		map.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, (float) weightC);
		
		return map;
	}

	private boolean isBatteryReady(Battery battery) {
		if (battery == null) {
			return false;
		}
		
		Optional<Boolean> readyOpt = battery.getReadyForWorking().getNextValue().asOptional();
		
		return (readyOpt.isPresent() && readyOpt.get());
	}

	@Override
	public int getPowerPrecision() {
		return 100; // TODO estimated value
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	private Collection<Battery> getBatteries() {
		Collection<Battery> batteries = new ArrayList<>();
		{
			Battery batteryStringA;
			try {
				batteryStringA = this.componentManager.getComponent(this.config.batteryStringA_id());
				if (batteryStringA != null) {
					batteries.add(batteryStringA);
				}
			} catch (OpenemsNamedException e) {
				// ignore
			}
		}
		{
			try {
				Battery batteryStringB = this.componentManager.getComponent(this.config.batteryStringB_id());
				if (batteryStringB != null) {
					batteries.add(batteryStringB);
				}
			} catch (OpenemsNamedException e) {
				// ignore
			}
		}
		{
			try {
				Battery batteryStringC = this.componentManager.getComponent(this.config.batteryStringC_id());
				if (batteryStringC != null) {
					batteries.add(batteryStringC);
				}
			} catch (OpenemsNamedException e) {
				// ignore
			}
		}
		return batteries;
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
	
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		int inverterCount = this.config.inverterCount().getCount();
	
		ModbusProtocol result = new ModbusProtocol(this, //
				/*
				 * CCU State
				 */
				new FC3ReadRegistersTask(32528, Priority.HIGH, //
						m(new BitsWordElement(32528, this) //
								.bit(0, GridConChannelId.CCU_STATE_IDLE) //
								.bit(1, GridConChannelId.CCU_STATE_PRECHARGE) //
								.bit(2, GridConChannelId.CCU_STATE_STOP_PRECHARGE) //
								.bit(3, GridConChannelId.CCU_STATE_READY) //
								.bit(4, GridConChannelId.CCU_STATE_PAUSE) //
								.bit(5, GridConChannelId.CCU_STATE_RUN) //
								.bit(6, GridConChannelId.CCU_STATE_ERROR) //
								.bit(7, GridConChannelId.CCU_STATE_VOLTAGE_RAMPING_UP) //
								.bit(8, GridConChannelId.CCU_STATE_OVERLOAD) //
								.bit(9, GridConChannelId.CCU_STATE_SHORT_CIRCUIT_DETECTED) //
								.bit(10, GridConChannelId.CCU_STATE_DERATING_POWER) //
								.bit(11, GridConChannelId.CCU_STATE_DERATING_HARMONICS) //
								.bit(12, GridConChannelId.CCU_STATE_SIA_ACTIVE) //
						), //
						new DummyRegisterElement(32529),
						m(GridConChannelId.CCU_ERROR_CODE,
								new UnsignedDoublewordElement(32530).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U12,
								new FloatDoublewordElement(32532).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U23,
								new FloatDoublewordElement(32534).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U31,
								new FloatDoublewordElement(32536).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL1,
								new FloatDoublewordElement(32538).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL2,
								new FloatDoublewordElement(32540).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL3,
								new FloatDoublewordElement(32542).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_POWER_P, new FloatDoublewordElement(32544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_POWER_Q, new FloatDoublewordElement(32546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_FREQUENCY, new FloatDoublewordElement(32548).wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * Commands
				 */
				new FC16WriteRegistersTask(32560, //
						m(new BitsWordElement(32560, this) //
								.bit(0, GridConChannelId.COMMAND_CONTROL_WORD_PLAY) //
								.bit(1, GridConChannelId.COMMAND_CONTROL_WORD_READY) //
								.bit(2, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE) //
								.bit(3, GridConChannelId.COMMAND_CONTROL_WORD_STOP) //

								.bit(4, GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL) //
								.bit(5, GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL) //
								.bit(6, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING) //
								.bit(7, GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION) //

								.bit(8, GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA) //
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET) //

								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET) //
						), //
						m(new BitsWordElement(32561, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_1) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_DATE,
								new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_TIME,
								new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				).debug(),
				/*
				 * Commands Mirror
				 */
				new FC3ReadRegistersTask(32880, Priority.LOW, //
						m(new BitsWordElement(32880, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_1) //
						), //
						m(new BitsWordElement(32881, this) //
								.bit(0, GridConChannelId.COMMAND_CONTROL_WORD_PLAY) //
								.bit(1, GridConChannelId.COMMAND_CONTROL_WORD_READY) //
								.bit(2, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE) //
								.bit(3, GridConChannelId.COMMAND_CONTROL_WORD_STOP) //
								.bit(4, GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL) //
								.bit(5, GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL) //
								.bit(6, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING) //
								.bit(7, GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION) //
								.bit(8, GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA) //
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32882).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32884).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32886).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32888).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(32890).wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * Control Parameters
				 */
				new FC16WriteRegistersTask(32592, //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
								new FloatDoublewordElement(32596).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(32598).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
								new FloatDoublewordElement(32600).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
								new FloatDoublewordElement(32602).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(32604).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
								new FloatDoublewordElement(32606).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
								new FloatDoublewordElement(32608).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
								new FloatDoublewordElement(32610).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND,
								new FloatDoublewordElement(32612).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
								new FloatDoublewordElement(32614).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
								new FloatDoublewordElement(32616).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
								new FloatDoublewordElement(32618).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
								new FloatDoublewordElement(32620).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
								new FloatDoublewordElement(32622).wordOrder(WordOrder.LSWMSW)) //
				).debug(),
				/*
				 * Control Parameters Mirror
				 */
				new FC3ReadRegistersTask(32912, Priority.LOW,
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32912).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32914).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
								new FloatDoublewordElement(32916).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(32918).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
								new FloatDoublewordElement(32920).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
								new FloatDoublewordElement(32922).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(32924).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
								new FloatDoublewordElement(32926).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
								new FloatDoublewordElement(32928).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
								new FloatDoublewordElement(32930).wordOrder(WordOrder.LSWMSW)) //
				));

		if (inverterCount > 0) {
			/*
			 * At least 1 Inverter -> Add IPU 1
			 */
			result.addTasks(//
					/*
					 * IPU 1 State
					 */
					new FC3ReadRegistersTask(33168, Priority.LOW, //
							m(GridConChannelId.INVERTER_1_STATUS_STATE_MACHINE, new UnsignedWordElement(33168)), //
							m(GridConChannelId.INVERTER_1_STATUS_MCU, new UnsignedWordElement(33169)), //
							m(GridConChannelId.INVERTER_1_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33170).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33172).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33174).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33176).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33178).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33180).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33182).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33184).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33186).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33188).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33190).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33192).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_1,
									new FloatDoublewordElement(33194).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_2,
									new FloatDoublewordElement(33196).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_3,
									new FloatDoublewordElement(33198).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 1 Control Parameters
					 */
					new FC16WriteRegistersTask(32624, //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32624).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32626).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32628).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32630).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32632).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32634).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32636).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32638).wordOrder(WordOrder.LSWMSW)) //
					).debug(),
					/*
					 * IPU 1 Mirror Control
					 */
					new FC3ReadRegistersTask(32944, Priority.LOW,
							m(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32944).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32946).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32948).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32950).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32952).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32954).wordOrder(WordOrder.LSWMSW)) //
					));
		}

		if (inverterCount > 1) {
			/*
			 * At least 2 Inverters -> Add IPU 2
			 */
			result.addTasks(//
					/*
					 * IPU 2 State
					 */
					new FC3ReadRegistersTask(33200, Priority.LOW, //
							m(GridConChannelId.INVERTER_2_STATUS_STATE_MACHINE, new UnsignedWordElement(33200)), //
							m(GridConChannelId.INVERTER_2_STATUS_MCU, new UnsignedWordElement(33201)), //
							m(GridConChannelId.INVERTER_2_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33202).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33204).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33206).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33208).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33210).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33212).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33214).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33216).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33218).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33220).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33222).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33224).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_1,
									new FloatDoublewordElement(33226).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_2,
									new FloatDoublewordElement(33228).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_3,
									new FloatDoublewordElement(33230).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 2 Control Parameters
					 */
					new FC16WriteRegistersTask(32656, //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32656).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32658).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32660).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32662).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32664).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32666).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32668).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32670).wordOrder(WordOrder.LSWMSW)) //
					).debug(),
					/*
					 * IPU 2 Mirror Control
					 */
					new FC3ReadRegistersTask(32976, Priority.LOW,
							m(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32976).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32978).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32980).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32982).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32984).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32986).wordOrder(WordOrder.LSWMSW)) //
					));
		}
		if (inverterCount > 2) {
			/*
			 * 3 Inverters -> Add IPU 3
			 */
			result.addTasks(//
					/*
					 * IPU 3 State
					 */
					new FC3ReadRegistersTask(33232, Priority.LOW, //
							m(GridConChannelId.INVERTER_3_STATUS_STATE_MACHINE, new UnsignedWordElement(33232)), //
							m(GridConChannelId.INVERTER_3_STATUS_MCU, new UnsignedWordElement(33233)), //
							m(GridConChannelId.INVERTER_3_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33234).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33236).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33238).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33240).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33242).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33244).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33246).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33248).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33250).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33252).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33254).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33256).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_1,
									new FloatDoublewordElement(33258).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_2,
									new FloatDoublewordElement(33260).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_3,
									new FloatDoublewordElement(33262).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 3 Control Parameters
					 */
					new FC16WriteRegistersTask(32688, //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32688).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32690).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32692).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32694).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32696).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32698).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32700).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32702).wordOrder(WordOrder.LSWMSW)) //
					).debug(),
					/*
					 * IPU 3 Mirror Control
					 */
					new FC3ReadRegistersTask(33008, Priority.LOW,
							m(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(33008).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(33010).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33012).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33014).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33016).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33018).wordOrder(WordOrder.LSWMSW)) //
					));
		}

		{
			/*
			 * DCDC
			 * 
			 * if one inverter is used, dc dc converter is ipu2 ...
			 */
			int startAddressIpuControl = 32720; // == THREE
			int startAddressIpuControlMirror = 33040; // == THREE
			int startAddressIpuState = 33264; // == THREE
			int startAddressIpuDcdc = 33584; // == THREE
			switch (this.config.inverterCount()) {
			case ONE:
				startAddressIpuControl = 32656;
				startAddressIpuControlMirror = 32976;
				startAddressIpuState = 33200;
				startAddressIpuDcdc = 33520;
				break;
			case TWO:
				startAddressIpuControl = 32688;
				startAddressIpuControlMirror = 33008;
				startAddressIpuState = 33232;
				startAddressIpuDcdc = 33552;
				break;
			case THREE:
				// default
				break;
			}

			result.addTasks(//
					/*
					 * DCDC Control
					 */
					new FC16WriteRegistersTask(startAddressIpuControl, //
							m(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(startAddressIpuControl).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A,
									new FloatDoublewordElement(startAddressIpuControl + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B,
									new FloatDoublewordElement(startAddressIpuControl + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C,
									new FloatDoublewordElement(startAddressIpuControl + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A,
									new FloatDoublewordElement(startAddressIpuControl + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B,
									new FloatDoublewordElement(startAddressIpuControl + 10)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C,
									new FloatDoublewordElement(startAddressIpuControl + 12)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE,
									new FloatDoublewordElement(startAddressIpuControl + 14).wordOrder(WordOrder.LSWMSW)) //
					).debug(),
					/*
					 * DCDC Control Mirror
					 */
					new FC3ReadRegistersTask(startAddressIpuControlMirror, Priority.LOW,
							m(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(startAddressIpuControlMirror)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A,
									new FloatDoublewordElement(startAddressIpuControlMirror + 2)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B,
									new FloatDoublewordElement(startAddressIpuControlMirror + 4)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C,
									new FloatDoublewordElement(startAddressIpuControlMirror + 6)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A,
									new FloatDoublewordElement(startAddressIpuControlMirror + 8)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B,
									new FloatDoublewordElement(startAddressIpuControlMirror + 10)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C,
									new FloatDoublewordElement(startAddressIpuControlMirror + 12)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE,
									new FloatDoublewordElement(startAddressIpuControlMirror + 14)
											.wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC State
					 */
					new FC3ReadRegistersTask(startAddressIpuState, Priority.LOW, // // IPU 4 state
							m(GridConChannelId.DCDC_STATUS_STATE_MACHINE,
									new UnsignedWordElement(startAddressIpuState)), //
							m(GridConChannelId.DCDC_STATUS_MCU, new UnsignedWordElement(startAddressIpuState + 1)), //
							m(GridConChannelId.DCDC_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(startAddressIpuState + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressIpuState + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressIpuState + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(startAddressIpuState + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(startAddressIpuState + 10).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(startAddressIpuState + 12).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(startAddressIpuState + 14)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(startAddressIpuState + 16)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(startAddressIpuState + 18).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(startAddressIpuState + 20).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(startAddressIpuState + 22).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(startAddressIpuState + 24).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_1,
									new FloatDoublewordElement(startAddressIpuState + 26).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_2,
									new FloatDoublewordElement(startAddressIpuState + 28).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_3,
									new FloatDoublewordElement(startAddressIpuState + 30).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC Measurements
					 */
					new FC3ReadRegistersTask(startAddressIpuDcdc, Priority.LOW, // IPU 4 measurements
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 10).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 12).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 14).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 16).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 18).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 20).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 22).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
									new FloatDoublewordElement(startAddressIpuDcdc + 24).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
									new FloatDoublewordElement(startAddressIpuDcdc + 26).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_1,
									new FloatDoublewordElement(startAddressIpuDcdc + 28).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_2,
									new FloatDoublewordElement(startAddressIpuDcdc + 30).wordOrder(WordOrder.LSWMSW)) //
					));
		}

		// Calculate Total Active Power
		FloatReadChannel ap1 = this.channel(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER);
		FloatReadChannel ap2 = this.channel(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER);
		FloatReadChannel ap3 = this.channel(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER);
		final Consumer<Value<Float>> calculateActivePower = ignoreValue -> {
			float ipu1 = ap1.getNextValue().orElse(0f);
			float ipu2 = ap2.getNextValue().orElse(0f);
			float ipu3 = ap3.getNextValue().orElse(0f);
			this.getActivePower().setNextValue(ipu1 + ipu2 + ipu3);
		};
		ap1.onSetNextValue(calculateActivePower);
		ap2.onSetNextValue(calculateActivePower);
		ap3.onSetNextValue(calculateActivePower);

		return result;
	}

	public void setOutputSyncDeviceBridge(boolean value) throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel outputSyncDeviceBridge = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.outputSyncDeviceBridge()));
		this.setOutput(outputSyncDeviceBridge, value);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value true to switch ON, false to switch ON
	 */
	private void setOutput(BooleanWriteChannel channel, boolean value) {
		Optional<Boolean> currentValueOpt = channel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			log.info("Set output [" + channel.address() + "] " + (value ? "ON" : "OFF") + ".");
			try {
				channel.setNextWriteValue(value);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Unable to set output: [" + channel.address() + "] " + e.getMessage());
			}
		}
	}

	protected BridgeModbus getModbus() {
		return super.getModbus();
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GridconPCS.class, accessMode, 300) //
						.build());
	}
}
