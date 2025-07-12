package io.openems.edge.evcs.keba.modbus;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.CONVERT_FIRMWARE_VERSION;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.calculateActivePowerL1L2L3;

import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evse.chargepoint.keba.common.EvcsKeba;
import io.openems.edge.evse.chargepoint.keba.common.enums.ProductTypeAndFeatures;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Keba.P40", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsKebaModbusImpl extends AbstractOpenemsModbusComponent implements EvcsKebaModbus, EvcsKeba, ManagedEvcs,
		Evcs, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave, ModbusComponent {

	private Clock clock;
	private final Logger log = LoggerFactory.getLogger(EvcsKebaModbusImpl.class);

	private Config config;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	private Instant lastWrite;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsKebaModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsKeba.ChannelId.values(), //
				EvcsKebaModbus.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		Evcs.addCalculatePowerLimitListeners(this);
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
		this.clock = this.componentManager.getClock();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
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
		return Math
				.round((this.getMaxChargingCurrent().orElse(null) != null ? this.getMaxChargingCurrent().get()
						: Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) / 1000f)
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
		 * 63000
		 */
		current = Math.min(current, 63_000);

		var setEnable = current <= 0 ? 0 : 1;
		if (current < 6000) {
			current = 0;
			setEnable = 0;
		}
		this.setEnable(setEnable);
		this.setChargingCurrent(current);
		this.lastWrite = Instant.now(this.clock);
		return true;
	}

	private void setEnable(int value) throws OpenemsNamedException {
		this.setSetEnable(value);
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
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			this.calculatePhases();
			if (!this.isReadOnly()) {
				this.writeHandler.run();
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

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// all registers are Priority.low except ActivePower because only one register
		// can be read at a time and only one readtask can be executed every 0.5 seconds
		final var phaseRotated = this.getPhaseRotation();
		ModbusProtocol modbusProtocol = new ModbusProtocol(this, new FC3ReadRegistersTask(1000, Priority.HIGH, //
				m(Evcs.ChannelId.STATUS, new UnsignedDoublewordElement(1000), new ElementToChannelConverter(t -> {
					this.logDebug("Keba Reading Status: " + TypeUtils.<Integer>getAsType(INTEGER, t));
					return switch (TypeUtils.<Integer>getAsType(INTEGER, t)) {
					case 0 -> Status.STARTING;
					case 1 -> Status.NOT_READY_FOR_CHARGING;
					case 2 -> Status.READY_FOR_CHARGING;
					case 3 -> Status.CHARGING;
					case 4 -> Status.ERROR;
					case 5 -> Status.CHARGING_REJECTED;
					case null, default -> Status.UNDEFINED;
					};
				}))), //
				new FC3ReadRegistersTask(1004, Priority.LOW, //
						m(EvcsKeba.ChannelId.PLUG, new UnsignedDoublewordElement(1004))),
				new FC3ReadRegistersTask(1006, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.ERROR_CODE, new UnsignedDoublewordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW, //
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(new UnsignedDoublewordElement(1016)).build().onUpdateCallback(value -> {
							var ptaf = ProductTypeAndFeatures.from(value);
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_PRODUCT_TYPE, ptaf.productType());
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_CABLE_OR_SOCKET, ptaf.cableOrSocket());
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_SUPPORTED_CURRENT, ptaf.supportedCurrent());
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_DEVICE_SERIES, ptaf.deviceSeries());
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_ENERGY_METER, ptaf.energyMeter());
							setValue(this, EvcsKebaModbus.ChannelId.PTAF_AUTHORIZATION, ptaf.authorization());
						})),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018),
								CONVERT_FIRMWARE_VERSION)),
				new FC3ReadRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3)
								.onUpdateCallback(power -> calculateActivePowerL1L2L3(this, power))),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(1036))),
				new FC3ReadRegistersTask(1040, Priority.LOW, //
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(1040), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1042, Priority.LOW, //
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(1042), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1044, Priority.LOW, //
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(1044), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1046, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.POWER_FACTOR, new UnsignedDoublewordElement(1046),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1100, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.MAX_CHARGING_CURRENT, new UnsignedDoublewordElement(1100),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(1110, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.MAX_HARDWARE_CURRENT, new UnsignedDoublewordElement(1110),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(1502, Priority.LOW, //
						m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(1502))),
				new FC3ReadRegistersTask(1550, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.PHASE_SWITCH_SOURCE, new UnsignedDoublewordElement(1550))),
				new FC3ReadRegistersTask(1552, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.PHASE_SWITCH_STATE, new UnsignedDoublewordElement(1552))),
				new FC3ReadRegistersTask(1600, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.FAILSAFE_CURRENT_SETTING, new UnsignedDoublewordElement(1600))),
				new FC3ReadRegistersTask(1602, Priority.LOW, //
						m(EvcsKebaModbus.ChannelId.FAILSAFE_TIMEOUT_SETTING, new UnsignedDoublewordElement(1602))));

		if (!this.isReadOnly()) {
			modbusProtocol.addTask(new FC6WriteRegisterTask(5004,
					m(EvcsKebaModbus.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(5004))));
			modbusProtocol.addTask(new FC6WriteRegisterTask(5014, //
					m(EvcsKebaModbus.ChannelId.SET_ENABLE, new UnsignedWordElement(5014))));
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
		return Instant.now(this.clock).isBefore(this.lastWrite.plusSeconds(5));
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}
}
