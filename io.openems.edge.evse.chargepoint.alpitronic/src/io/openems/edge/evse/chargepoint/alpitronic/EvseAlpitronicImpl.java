package io.openems.edge.evse.chargepoint.alpitronic;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState.UNDEFINED;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.alpitronic.AlpitronicVersionUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Alpitronic", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseAlpitronicImpl extends EvseAlpitronic implements EvseChargePoint, ElectricityMeter, OpenemsComponent,
		TimedataProvider, EventHandler, ModbusComponent {

	private static final int MAX_FIRMWARE_VERSION_READ_ERRORS = 5;
	private static final SemanticVersion FALLBACK_FIRMWARE_VERSION = new SemanticVersion(2, 5, 0);

	private final Logger log = LoggerFactory.getLogger(EvseAlpitronicImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	private AlpitronicsUtils utils = new AlpitronicsUtils(this);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final IntFunction<Integer> offset = addr -> addr + this.config.connector().modbusOffset;

	private Config config;

	private final ModbusProtocol modbusProtocol;

	private SemanticVersion detectedFirmwareVersion = null;
	private boolean protocolConfigured = false;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseAlpitronicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				Alpitronic.ChannelId.values(), //
				EvseAlpitronic.ChannelId.values() //
		);

		ElectricityMeter.calculatePhasesFromActivePower(this);
		this.addCalculateVoltageAndCurrentListeners();

		this.modbusProtocol = new ModbusProtocol(this);
	}

	private void addCalculateVoltageAndCurrentListeners() {
		this.getVoltageChannel().onSetNextValue(value -> {
			var voltage = value.get();
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, voltage);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, voltage);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, voltage);
		});

		this.getCurrentChannel().onSetNextValue(value -> {
			var phaseCurrent = TypeUtils.divide(value.get(), 3);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, phaseCurrent);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, phaseCurrent);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, phaseCurrent);
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Detect firmware version and configure protocol once.
		this.detectFirmwareVersion();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	private void detectFirmwareVersion() {
		final var consecutiveReadErrors = new AtomicInteger(0);
		final var lastReadErrorState = new AtomicReference<Task.ExecuteState>(null);

		ModbusUtils.readElementsOnce(ModbusUtils.FunctionCode.FC4, this.modbusProtocol, (executeState, value) -> {
			if (value != null) {
				consecutiveReadErrors.set(0);
				lastReadErrorState.set(null);
				return false;
			}

			if (executeState instanceof Task.ExecuteState.Error) {
				if (lastReadErrorState.getAndSet(executeState) != executeState) {
					consecutiveReadErrors.incrementAndGet();
				}
				return consecutiveReadErrors.get() < MAX_FIRMWARE_VERSION_READ_ERRORS;
			}

			consecutiveReadErrors.set(0);
			lastReadErrorState.set(null);
			return true;
		}, new UnsignedWordElement(46), new UnsignedWordElement(47), new UnsignedWordElement(48)).thenAccept(result -> {
			var values = result.values();
			var major = (int) values.get(0);
			var minor = (int) values.get(1);
			var patch = (int) values.get(2);
			var version = SemanticVersion.fromNullable(major, minor, patch).orElse(FALLBACK_FIRMWARE_VERSION);
			this.configureProtocolForVersion(version);
		}).exceptionally(e -> {
			this.logInfo(this.log, "Firmware version detection failed; fallback to " + FALLBACK_FIRMWARE_VERSION
					+ ". Reason: " + e.getMessage());
			this.configureProtocolForVersion(FALLBACK_FIRMWARE_VERSION);
			return null;
		});
	}

	private void configureProtocolForVersion(SemanticVersion version) {
		if (this.protocolConfigured) {
			return;
		}
		this.detectedFirmwareVersion = version;

		this.addCommonStationTasks();
		this.addProtocolTasksForVersion(version);

		this.addCalculatePowerListeners();

		this.protocolConfigured = true;
		this.logInfo(this.log, "Detected Alpitronic firmware version: " + version);
	}

	private void addProtocolTasksForVersion(SemanticVersion version) {
		if (AlpitronicVersionUtils.isVersion18(version)) {
			this.addProtocolTasksV18();
		} else if (AlpitronicVersionUtils.isVersion23(version)) {
			this.addProtocolTasksV23();
		} else if (AlpitronicVersionUtils.isVersion24(version)) {
			this.addProtocolTasksV24();
		} else {
			// Default to v2.5+
			this.addProtocolTasksV25();
		}
	}

	private int getSetpointReactivePowerWriteAddress() {
		if (this.detectedFirmwareVersion != null && AlpitronicVersionUtils.isVersion18(this.detectedFirmwareVersion)) {
			return this.offset.apply(2);
		}
		return 2;
	}

	private void addCommonStationTasks() {
		// Common tasks for station-level registers
		this.modbusProtocol.addTask(new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER_SET, new UnsignedDoublewordElement(this.offset.apply(0)))));

		this.modbusProtocol.addTask(new FC16WriteRegistersTask(this.offset.apply(0),
				m(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT, new UnsignedDoublewordElement(this.offset.apply(0)))));
		final var reactivePowerWriteAddress = this.getSetpointReactivePowerWriteAddress();
		this.modbusProtocol.addTask(
				new FC16WriteRegistersTask(reactivePowerWriteAddress, m(Alpitronic.ChannelId.SETPOINT_REACTIVE_POWER,
						new UnsignedDoublewordElement(reactivePowerWriteAddress))));
	}

	private ElementToChannelConverter connectorTypeConverter(boolean isVersion25OrLater) {
		return new ElementToChannelConverter(value -> SelectedConnector
				.fromRawValue(TypeUtils.getAsType(OpenemsType.INTEGER, value), isVersion25OrLater));
	}

	private void addProtocolTasksV23OrLater(boolean isVersion25OrLater) {
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
				m(EvseAlpitronic.ChannelId.AVAILABLE_STATE, new UnsignedWordElement(this.offset.apply(0))),
				m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
						SCALE_FACTOR_MINUS_2),
				m(ElectricityMeter.ChannelId.CURRENT, new UnsignedWordElement(this.offset.apply(3)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
				m(Alpitronic.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
				m(Alpitronic.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)), SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9)),
						this.connectorTypeConverter(isVersion25OrLater)),
				m(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(10))),
				m(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(12)))));

		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(32), Priority.LOW,
				m(EvseAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY,
						new UnsignedQuadruplewordElement(this.offset.apply(32)))));

		// Station-level VAR registers for v2.3+
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(49, Priority.LOW,
				m(Alpitronic.ChannelId.VAR_REACTIVE_MAX, new UnsignedDoublewordElement(49)),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(51), INVERT)));
	}

	private void addProtocolTasksV18() {
		// v1.8: station-level VAR registers with connector-level reactive power at
		// offset+2
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
				m(EvseAlpitronic.ChannelId.AVAILABLE_STATE, new UnsignedWordElement(this.offset.apply(0))),
				m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
						SCALE_FACTOR_MINUS_2),
				m(ElectricityMeter.ChannelId.CURRENT, new UnsignedWordElement(this.offset.apply(3)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
				m(Alpitronic.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
				m(Alpitronic.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)), SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9)),
						this.connectorTypeConverter(false)),
				m(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(10))),
				m(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(12))),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MAX, new UnsignedDoublewordElement(this.offset.apply(14))),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(this.offset.apply(16)),
						INVERT)));
	}

	private void addProtocolTasksV23() {
		// v2.3: station-level VAR registers, added TOTAL_CHARGED_ENERGY
		this.addProtocolTasksV23OrLater(false);
	}

	private void addProtocolTasksV24() {
		// v2.4: all v2.3 features + MAX_CHARGING_POWER_AC
		this.addProtocolTasksV23();

		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(36), Priority.LOW, m(
				EvseAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, new UnsignedDoublewordElement(this.offset.apply(36)))));
	}

	private void addProtocolTasksV25() {
		this.addProtocolTasksV23OrLater(true);

		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(36), Priority.LOW, m(
				EvseAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, new UnsignedDoublewordElement(this.offset.apply(36)))));
	}

	/*
	 * TODO: Remove if the charge power register returns valid values with newer
	 * firmware versions.
	 */
	private void addCalculatePowerListeners() {

		// Calculate power from voltage and current
		final Consumer<Value<Integer>> calculatePower = ignore -> {
			setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER,
					TypeUtils.getAsType(OpenemsType.INTEGER, TypeUtils.multiply(//
							this.getVoltageChannel().getNextValue().get(), //
							this.getCurrentChannel().getNextValue().get() //
			)));
		};
		this.getVoltageChannel().onSetNextValue(calculatePower);
		this.getCurrentChannel().onSetNextValue(calculatePower);
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		return this.utils.getChargePointAbilities(this.config);
	}

	@Override
	public String debugLog() {
		return this.utils.debugLog(this.config);
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.utils.applyChargePointActions(this.config, actions);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		if (event.getTopic().equals(TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
			this.utils.onBeforeProcessImage(this.config);
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public boolean getIsReadyForCharging() {
		return this.getIsReadyForChargingChannel().value().orElse(false);
	}

	@Override
	public boolean getIsEvConnected() {
		return switch (this.getAvailableStateChannel().getNextValue().orElse(UNDEFINED)) {
		case PREPARING_EV_READY, CHARGING, SUSPENDED_EV, SUSPENDED_EV_SE, FINISHING -> true;
		case UNDEFINED, AVAILABLE, PREPARING_TAG_ID_READY, RESERVED, UNAVAILABLE, UNAVAILABLE_FW_UPDATE, FAULTED,
				UNAVAILABLE_CONNECTION_OBJECT ->
			false;
		};
	}

	@Override
	public int getMinChargePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getMaxChargePower() {
		return this.config.maxHwPower();
	}
}
