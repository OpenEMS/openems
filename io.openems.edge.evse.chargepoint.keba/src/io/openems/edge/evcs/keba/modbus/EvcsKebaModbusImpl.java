package io.openems.edge.evcs.keba.modbus;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.calculateActivePowerL1L2L3;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.handleFirmwareVersion;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.UnknownHostException;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evse.chargepoint.keba.common.EvcsKeba;
import io.openems.edge.evse.chargepoint.keba.common.EvseKeba;
import io.openems.edge.evse.chargepoint.keba.common.Keba;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;
import io.openems.edge.evse.chargepoint.keba.common.KebaUtils;
import io.openems.edge.evse.chargepoint.keba.common.ProductTypeAndFeatures;
import io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Keba.P40", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsKebaModbusImpl extends KebaModbus implements EvcsKeba, ManagedEvcs, Evcs, DeprecatedEvcs,
		ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave, ModbusComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(EvcsKebaModbusImpl.class);
	private final KebaUtils kebaUtils = new KebaUtils(this);
	private final KebaModbusUtils kebaModbusUtils = new KebaModbusUtils(this);

	private Config config;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	private Instant lastWrite;
	private boolean setEnableSet;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsKebaModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				KebaModbus.ChannelId.values(), //
				Keba.ChannelId.values(), //
				EvcsKeba.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		Evcs.addCalculatePowerLimitListeners(this);
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		this.activateOrModified();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.activateOrModified();
	}

	private void activateOrModified() {
		this._setPowerPrecision(0.23);
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this.setEnableSet = false;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.getMaxChargingCurrent().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) / 1000f)
				* DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsNamedException {
		if (this.isReadOnly()) {
			return false;
		}
		// ensure that it is only written once every 5 seconds at most
		if (this.checkWriteIntervall()) {
			return false;
		}
		var phases = this.getPhasesAsInt();
		var current = Math.round((power * 1000) / phases / 230f);

		/*
		 * Limits the charging value because KEBA knows only values between 6000 and
		 * 32000
		 */
		IntegerReadChannel maxHw = this.channel(EvcsKeba.ChannelId.MAX_HARDWARE_CURRENT);
		current = Math.min(current, maxHw.getNextValue().orElse(DEFAULT_MAXIMUM_HARDWARE_CURRENT));

		if (current < 6000) {
			current = 0;
		}
		this.setChargingCurrent(current);
		this.lastWrite = Instant.now(this.componentManager.getClock());
		return true;
	}

	private void setChargingCurrent(int value) throws OpenemsNamedException {
		this.setSetChargingCurrent(value);
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.kebaUtils.onBeforeProcessImage();
		}
		case TOPIC_CYCLE_EXECUTE_WRITE -> {
			this.calculatePhases();
			if (!this.isReadOnly()) {
				this.writeHandler.run();
				this.setEnableOnce();
			}
		}
		}
	}

	private void calculatePhases() {
		var currentL1 = this.getCurrentL1().orElse(0);
		var currentL2 = this.getCurrentL2().orElse(0);
		var currentL3 = this.getCurrentL3().orElse(0);
		var phases = Evcs.evaluatePhaseCountFromCurrent(currentL1, currentL2, currentL3);
		this._setPhases(phases);
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		this.logDebug(this.log, message);
	}

	private void setEnableOnce() {
		if (this.setEnableSet) {
			return;
		}
		final var status = this.<EnumReadChannel>channel(Evcs.ChannelId.STATUS).getNextValue();
		if (status.isDefined() ? status.get() == 2 : false) {
			try {
				this.setSetEnable(1);
				this.setEnableSet = true;
			} catch (OpenemsNamedException e) {
				this.logDebug(
						"A problem occurred while setting the EVCS KEBA P40 charging station 'Enable user' to 'enable'.");
				e.printStackTrace();
			}
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// NOTE: Changes here must be copied to EvseChargePointKebaModbusImpl as well

		// KEBA protocol definition states:
		// The interval for reading registers is 0.5 seconds. The interval for writing
		// registers is 5 seconds.
		// Consequently we set most registers to Priority.LOW
		final var phaseRotated = this.getPhaseRotation();
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1000, Priority.HIGH, //
						m(new UnsignedDoublewordElement(1000)) //
								.m(Keba.ChannelId.CHARGING_STATE, DIRECT_1_TO_1) //
								.m(Evcs.ChannelId.STATUS, new ElementToChannelConverter(t -> {
									return switch (TypeUtils.<Integer>getAsType(INTEGER, t)) {
									case 0 -> Status.STARTING;
									case 1 -> Status.NOT_READY_FOR_CHARGING;
									case 2 -> Status.READY_FOR_CHARGING;
									case 3 -> Status.CHARGING;
									case 4 -> Status.ERROR;
									case 5 -> Status.CHARGING_REJECTED;
									case null, default -> Status.UNDEFINED;
									};
								})) //
								.build()),

				new FC3ReadRegistersTask(1004, Priority.LOW, //
						m(new UnsignedDoublewordElement(1004)) //
								.m(Keba.ChannelId.CABLE_STATE, DIRECT_1_TO_1) //
								.m(EvcsKeba.ChannelId.PLUG, DIRECT_1_TO_1) //
								.build()), //
				new FC3ReadRegistersTask(1006, Priority.LOW, //
						m(KebaModbus.ChannelId.ERROR_CODE, new UnsignedDoublewordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW, //
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(KebaModbus.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(new UnsignedDoublewordElement(1016)).build().onUpdateCallback(value -> {
							var ptaf = ProductTypeAndFeatures.from(value);
							// TODO add Warning for PTAF_PRODUCT_FAMILY.KC_P30: KEBA P30 Modbus/TCP is not
							// supported
							setValue(this, KebaModbus.ChannelId.PTAF_PRODUCT_FAMILY, ptaf.productFamily());
							setValue(this, KebaModbus.ChannelId.PTAF_DEVICE_CURRENT, ptaf.deviceCurrent());
							setValue(this, KebaModbus.ChannelId.PTAF_CONNECTOR, ptaf.connector());
							setValue(this, KebaModbus.ChannelId.PTAF_PHASES, ptaf.phases());
							setValue(this, KebaModbus.ChannelId.PTAF_METERING, ptaf.metering());
							setValue(this, KebaModbus.ChannelId.PTAF_RFID, ptaf.rfid());
							setValue(this, KebaModbus.ChannelId.PTAF_BUTTON, ptaf.button());
						})),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(KebaModbus.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018)) //
								.onUpdateCallback((v) -> handleFirmwareVersion(this, v))),
				new FC3ReadRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3)
								.onUpdateCallback(power -> calculateActivePowerL1L2L3(this, power))),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
								new UnsignedDoublewordElement(1036), this.kebaModbusUtils.energyScaleFactor)),
				new FC3ReadRegistersTask(1040, Priority.LOW, //
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(1040), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1042, Priority.LOW, //
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(1042), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1044, Priority.LOW, //
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(1044), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1046, Priority.LOW, //
						m(Keba.ChannelId.POWER_FACTOR, new UnsignedDoublewordElement(1046), SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1100, Priority.LOW, //
						m(KebaModbus.ChannelId.MAX_CHARGING_CURRENT, new UnsignedDoublewordElement(1100))),
				new FC3ReadRegistersTask(1110, Priority.LOW, //
						m(EvcsKeba.ChannelId.MAX_HARDWARE_CURRENT, new UnsignedDoublewordElement(1110))),
				// todo: read Register 1500 RFID once solution is found
				// this register is can not always be read with keba firmware 1.1.9 or less
				// there is currently no way of knowing when it can be read
				new FC3ReadRegistersTask(1502, Priority.LOW, //
						m(Evcs.ChannelId.ENERGY_SESSION, //
								new UnsignedDoublewordElement(1502), this.kebaModbusUtils.energyScaleFactor)),
				new FC3ReadRegistersTask(1550, Priority.LOW, //
						m(Keba.ChannelId.PHASE_SWITCH_SOURCE, new UnsignedDoublewordElement(1550))),
				new FC3ReadRegistersTask(1552, Priority.LOW, //
						m(Keba.ChannelId.PHASE_SWITCH_STATE, new UnsignedDoublewordElement(1552))),
				new FC3ReadRegistersTask(1600, Priority.LOW, //
						m(KebaModbus.ChannelId.FAILSAFE_CURRENT_SETTING, new UnsignedDoublewordElement(1600))),
				new FC3ReadRegistersTask(1602, Priority.LOW, //
						m(KebaModbus.ChannelId.FAILSAFE_TIMEOUT_SETTING, new UnsignedDoublewordElement(1602))));

		if (!this.isReadOnly()) {
			modbusProtocol.addTasks(//
					new FC6WriteRegisterTask(5004,
							m(Keba.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(5004))),
					new FC6WriteRegisterTask(5010, // TODO Scalefactor for Unit: 10 Wh
							m(EvseKeba.ChannelId.SET_ENERGY_LIMIT, new UnsignedWordElement(5010))),
					new FC6WriteRegisterTask(5012, m(Keba.ChannelId.SET_UNLOCK_PLUG, new UnsignedWordElement(5012))),
					new FC6WriteRegisterTask(5014, m(Keba.ChannelId.SET_ENABLE, new UnsignedWordElement(5014))),
					new FC6WriteRegisterTask(5050,
							m(Keba.ChannelId.SET_PHASE_SWITCH_SOURCE, new UnsignedWordElement(5050))),
					new FC6WriteRegisterTask(5052,
							m(Keba.ChannelId.SET_PHASE_SWITCH_STATE, new UnsignedWordElement(5052))));
		}

		return modbusProtocol;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	private boolean checkWriteIntervall() {
		if (this.lastWrite == null) {
			return false;
		}
		return Instant.now(this.componentManager.getClock()).isBefore(this.lastWrite.plusSeconds(5));
	}

	@Override
	public String debugLog() {
		return this.kebaUtils.debugLog();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public final ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode), //
				ManagedEvcs.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EvcsKebaModbusImpl.class, accessMode, 100) //
						.build());
	}
}
