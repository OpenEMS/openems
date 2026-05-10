package io.openems.edge.battery.bmw;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bmw.enums.BatteryState;
import io.openems.edge.battery.bmw.enums.BatteryStateCommand;
import io.openems.edge.battery.bmw.statemachine.Context;
import io.openems.edge.battery.bmw.statemachine.StateMachine;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.BMW", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BatteryBmwImpl extends AbstractOpenemsModbusComponent
		implements ModbusComponent, BatteryBmw, Battery, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private static final int INNER_RESISTANCE = 200; // [mOhm]
	private static final double MIN_ALLOWED_SOC = 4d;
	private static final double MAX_ALLOWED_SOC = 96d;
	private static final String URI_LOGIN = "login";

	private final Logger log = LoggerFactory.getLogger(BatteryBmwImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config;
	private BatteryProtection batteryProtection = null;
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Reference
	private BmwToken token;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public BatteryBmwImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				BatteryBmw.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setInnerResistance(INNER_RESISTANCE);

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinition(), this.componentManager) //
				.build();

		this.httpBridge = this.httpBridgeFactory.get();
		this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

		this.initializeAuthentication();
	}

	private void initializeAuthentication() {
		var auth = this.oem.getBmwBatteryAuth();
		if (auth == null) {
			return;
		}

		final var userName = auth.a();
		final var password = auth.b();

		final var url = this.getUrl((BridgeModbusTcp) this.getBridgeModbus(), URI_LOGIN, "");
		final var endpoint = BridgeHttp.create(url) //
				.setBodyJson(JsonUtils.buildJsonObject() //
						.add("userCredentials", JsonUtils.buildJsonObject() //
								.addProperty("name", userName) //
								.addProperty("password", password) //
								.build()) //
						.build())
				.build();

		this.token.fetchToken(endpoint);
	}

	public String getToken() {
		return this.token.getToken();
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(1400, //
						m(BatteryBmw.ChannelId.BATTERY_STATE_COMMAND, new UnsignedWordElement(1400)) //
				),

				new FC4ReadInputRegistersTask(1000, Priority.HIGH,
						m(BatteryBmw.ChannelId.BATTERY_STATE, new UnsignedWordElement(1000)),

						m(new BitsWordElement(1001, this) // ErrBits1
								.bit(0, BatteryBmw.ChannelId.UNSPECIFIED_ERROR) //
								.bit(1, BatteryBmw.ChannelId.LOW_VOLTAGE_ERROR) //
								.bit(2, BatteryBmw.ChannelId.HIGH_VOLTAGE_ERROR) //
								.bit(3, BatteryBmw.ChannelId.CHARGE_CURRENT_ERROR) //
								.bit(4, BatteryBmw.ChannelId.DISCHARGE_CURRENT_ERROR) //
								.bit(5, BatteryBmw.ChannelId.CHARGE_POWER_ERROR) //
								.bit(6, BatteryBmw.ChannelId.DISCHARGE_POWER_ERROR) //
								.bit(7, BatteryBmw.ChannelId.LOW_SOC_ERROR) //
								.bit(8, BatteryBmw.ChannelId.HIGH_SOC_ERROR) //
								.bit(9, BatteryBmw.ChannelId.LOW_TEMPERATURE_ERROR) //
								.bit(10, BatteryBmw.ChannelId.HIGH_TEMPERATURE_ERROR) //
								.bit(11, BatteryBmw.ChannelId.INSULATION_ERROR) //
								.bit(12, BatteryBmw.ChannelId.CONTACTOR_ERROR) //
								.bit(13, BatteryBmw.ChannelId.SENSOR_ERROR) //
								.bit(14, BatteryBmw.ChannelId.IMBALANCE_ERROR) //
								.bit(15, BatteryBmw.ChannelId.COMMUNICATION_ERROR) //

						), //

						m(new BitsWordElement(1002, this) // ErrBits2
								.bit(0, BatteryBmw.ChannelId.CONTAINER_ERROR) //
								.bit(1, BatteryBmw.ChannelId.SOH_ERROR) //
								.bit(2, BatteryBmw.ChannelId.RACK_STING_ERROR) //
						), //

						m(new BitsWordElement(1003, this) // WarnBits1
								.bit(0, BatteryBmw.ChannelId.UNSPECIFIED_WARNING) //
								.bit(1, BatteryBmw.ChannelId.LOW_VOLTAGE_WARNING) //
								.bit(2, BatteryBmw.ChannelId.HIGH_VOLTAGE_WARNING) //
								.bit(3, BatteryBmw.ChannelId.CHARGE_CURRENT_WARNING) //
								.bit(4, BatteryBmw.ChannelId.DISCHARGE_CURRENT_WARNING) //
								.bit(5, BatteryBmw.ChannelId.CHARGE_POWER_WARNING) //
								.bit(6, BatteryBmw.ChannelId.DISCHARGE_POWER_WARNING) //
								.bit(7, BatteryBmw.ChannelId.LOW_SOC_WARNING) //
								.bit(8, BatteryBmw.ChannelId.HIGH_SOC_WARNING) //
								.bit(9, BatteryBmw.ChannelId.LOW_TEMPERATURE_WARNING) //
								.bit(10, BatteryBmw.ChannelId.HIGH_TEMPERATURE_WARNING) //
								.bit(11, BatteryBmw.ChannelId.INSULATION_WARNING) //
								.bit(12, BatteryBmw.ChannelId.CONTACTOR_WARNING) //
								.bit(13, BatteryBmw.ChannelId.SENSOR_WARNING) //
								.bit(14, BatteryBmw.ChannelId.IMBALANCE_WARNING) //
								.bit(15, BatteryBmw.ChannelId.COMMUNICATION_WARNING) //
						), //

						m(new BitsWordElement(1004, this) // WarnBits2
								.bit(0, BatteryBmw.ChannelId.CONTAINER_WARNING) //
								.bit(1, BatteryBmw.ChannelId.SOH_WARNING) //
								.bit(2, BatteryBmw.ChannelId.RACK_STING_WARNING) //
						), //

						new DummyRegisterElement(1005), //
						m(BatteryBmw.ChannelId.MAX_OPERATING_CURRENT, new SignedWordElement(1006)), //
						m(BatteryBmw.ChannelId.MIN_OPERATING_CURRENT, new SignedWordElement(1007)), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new SignedWordElement(1008), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new SignedWordElement(1009), SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new SignedWordElement(1010)), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new SignedWordElement(1011), INVERT),
						m(BatteryBmw.ChannelId.MAX_DYNAMIC_VOLTAGE, new UnsignedWordElement(1012)),
						m(BatteryBmw.ChannelId.MIN_DYNAMIC_VOLTAGE, new UnsignedWordElement(1013)),
						m(BatteryBmw.ChannelId.CONNECTED_STRING_NUMBER, new UnsignedWordElement(1014)), //
						m(BatteryBmw.ChannelId.INSTALLED_STRING_NUMBER, new UnsignedWordElement(1015)), //
						m(BatteryBmw.ChannelId.BATTERY_TOTAL_SOC, new UnsignedWordElement(1016)),
						m(BatteryBmw.ChannelId.BATTERY_SOC, new UnsignedWordElement(1017)),
						m(BatteryBmw.ChannelId.REMAINING_CHARGE_CAPACITY, new UnsignedWordElement(1018)), //
						m(BatteryBmw.ChannelId.REMAINING_DISCHARGE_CAPACITY, new UnsignedWordElement(1019)), //
						m(BatteryBmw.ChannelId.REMAINING_CHARGE_ENERGY, new UnsignedWordElement(1020)), //
						m(BatteryBmw.ChannelId.REMAINING_DISCHARGE_ENERGY, new UnsignedWordElement(1021)), //
						m(BatteryBmw.ChannelId.NOMINAL_ENERGY, new UnsignedWordElement(1022)), //
						m(BatteryBmw.ChannelId.NOMINAL_ENERGY_TOTAL, new UnsignedWordElement(1023)), //
						m(BatteryBmw.ChannelId.NOMINAL_CAPACITY, new UnsignedWordElement(1024)), //
						m(Battery.ChannelId.CAPACITY, new UnsignedWordElement(1025)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(1026), SCALE_FACTOR_MINUS_2), //
						// TODO battery.api
						m(BatteryBmw.ChannelId.LINK_VOLTAGE, new UnsignedWordElement(1027)), //
						m(BatteryBmw.ChannelId.INTERNAL_VOLTAGE, new UnsignedWordElement(1028)), //
						m(Battery.ChannelId.CURRENT, new SignedWordElement(1029), SCALE_FACTOR_MINUS_1), //
						m(BatteryBmw.ChannelId.AVG_BATTERY_TEMPERATURE, new UnsignedWordElement(1030),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(1031), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(1032), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new SignedWordElement(1033), DIRECT_1_TO_1), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new SignedWordElement(1034), DIRECT_1_TO_1), //
						m(BatteryBmw.ChannelId.AVG_CELL_TEMPERATURE, new UnsignedWordElement(1035),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(1036), //
						m(BatteryBmw.ChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(1037)), //
						new DummyRegisterElement(1038, 1040), //
						m(BatteryBmw.ChannelId.DISCHARGE_MAX_CURRENT_HIGH_RESOLUTION, new UnsignedWordElement(1041)), //
						m(BatteryBmw.ChannelId.CHARGE_MAX_CURRENT_HIGH_RESOLUTION, new UnsignedWordElement(1042)), //
						m(BatteryBmw.ChannelId.FULL_CYCLE_COUNT, new UnsignedWordElement(1043)) //
				));
	}

	@Override
	public String debugLog() {
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryBmw.class, accessMode, 100) //
						.build());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.batteryProtection.apply();
			this.handleStateMachine();
		}
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		final var currentState = this.stateMachine.getCurrentState();
		this._setStateMachine(currentState);

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		try {
			var context = new Context(this, this.componentManager.getClock(), this.httpBridge, this.cycleService);
			this.stateMachine.run(context);
			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	protected synchronized void updateSoc() {
		Channel<Double> batterySocChannel = this.channel(BatteryBmw.ChannelId.BATTERY_SOC);
		var batterySoc = batterySocChannel.value();
		var soc = batterySoc.asOptional().map(this::calculateNormalizedSoc).orElse(null);
		this._setSoc(soc);
	}

	/**
	 * Calculates the normalized State of Charge (SOC) from the raw battery SOC
	 * value.
	 * 
	 * <p>
	 * This method transforms the battery's raw SOC percentage (which operates in
	 * the range MIN_ALLOWED_SOC to MAX_ALLOWED_SOC) to a normalized 0-100% range.
	 * </p>
	 * 
	 * @param rawSocPercent the raw SOC percentage from the battery (0-100%)
	 * @return normalized SOC as integer percentage (0-100%), or null if invalid
	 */
	private Integer calculateNormalizedSoc(double rawSocPercent) {
		// Convert percentage to decimal and subtract the minimum allowed SOC
		double adjustedSoc = rawSocPercent / 100.0 - MIN_ALLOWED_SOC;

		// Scale to the usable SOC range (MIN_ALLOWED_SOC to MAX_ALLOWED_SOC)
		double usableRange = MAX_ALLOWED_SOC - MIN_ALLOWED_SOC;
		double normalizedSoc = (adjustedSoc / usableRange) * 100.0;

		// Convert to integer with 0.1% precision for raw value storage
		int calculatedBatterySoc = (int) (normalizedSoc * 10.0);
		this._setSocRawValue(calculatedBatterySoc);

		// Clamp to valid range and convert back to percentage
		if (calculatedBatterySoc < 0) {
			return 0;
		}
		if (calculatedBatterySoc > 1000) {
			return 100;
		}
		return calculatedBatterySoc / 10;
	}

	/**
	 * The internal voltage is preferred when the battery is started, as the
	 * internal voltage is more accurate than the junction voltage.
	 */
	protected synchronized void updateVoltage() {
		Integer batteryVoltage = null;
		if (this.getInternalVoltage().isDefined() && this.getLinkVoltage().isDefined()) {
			if (this.isStarted()) {
				batteryVoltage = this.getInternalVoltage().get();
			} else {
				batteryVoltage = this.getLinkVoltage().get();
			}
		}
		this._setVoltage(TypeUtils.divide(batteryVoltage, 10));
	}

	/**
	 * Start the battery.
	 */
	public void startBattery() {
		try {
			this.setBatteryStateCommand(BatteryStateCommand.CLOSE_CONTACTOR);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Battery can not start : " + e.getMessage());
		}
	}

	/**
	 * Stop the battery.
	 */
	public void stopBattery() {
		try {
			this.setBatteryStateCommand(BatteryStateCommand.OPEN_CONTACTOR);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Battery can not stop : " + e.getMessage());
		}
	}

	/**
	 * Check if battery is in running state.
	 * 
	 * @return true if battery is started
	 */
	public boolean isRunning() {
		return this.getBatteryState().asEnum() == BatteryState.OPERATION;
	}

	/**
	 * Check if battery is in stopped state.
	 * 
	 * @return true if battery is stopped
	 */
	public boolean isShutdown() {
		final var batterySystemState = this.getBatteryState().asEnum();
		return batterySystemState == BatteryState.READY || batterySystemState == BatteryState.OFF
				|| batterySystemState == BatteryState.UNDEFINED;
	}

	/**
	 * Gets the URL from ip address, uri and battery id.
	 * 
	 * @param bridge the modbus bridge
	 * @param uri    the uri
	 * @param id     the battery id
	 * @return the url
	 */
	public String getUrl(BridgeModbusTcp bridge, String uri, String id) {
		return "http://" + bridge.getIpAddress().getHostAddress() + "/" + uri + "/" + id;
	}

}