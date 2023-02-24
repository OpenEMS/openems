package io.openems.edge.evcs.webasto;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
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

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evcs.webasto.api.Webasto;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Webasto.Unite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class WebastoImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, Webasto, Evcs, ManagedEvcs, EventHandler {

	private final Logger log = LoggerFactory.getLogger(WebastoImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	private Config config;

	private int minCurrent;
	private int maxCurrent;
	private WebastoReadHandler readHandler;

	@Reference
	private EvcsPower evcsPower;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public WebastoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Webasto.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;

		this.minCurrent = config.minHwCurrent();
		this.maxCurrent = config.maxHwCurrent();
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this.readHandler = new WebastoReadHandler(this);

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(100, Priority.HIGH,
						m(Webasto.ChannelId.SERIAL_NUMBER, new StringWordElement(100, 25))),
				new FC4ReadInputRegistersTask(130, Priority.HIGH,
						m(Webasto.ChannelId.CHARGE_POINT_ID, new StringWordElement(130, 50))),
				new FC4ReadInputRegistersTask(190, Priority.HIGH,
						m(Webasto.ChannelId.BRAND, new StringWordElement(190, 10))),
				new FC4ReadInputRegistersTask(210, Priority.HIGH,
						m(Webasto.ChannelId.MODEL, new StringWordElement(210, 5))),
				new FC4ReadInputRegistersTask(230, Priority.HIGH,
						m(Webasto.ChannelId.FIRMWARE_VERSION, new StringWordElement(230, 50))),
				new FC4ReadInputRegistersTask(290, Priority.HIGH,
						m(Webasto.ChannelId.DATE, new UnsignedDoublewordElement(290))),
				new FC4ReadInputRegistersTask(294, Priority.HIGH,
						m(Webasto.ChannelId.TIME, new UnsignedDoublewordElement(294))),
				new FC4ReadInputRegistersTask(400, Priority.HIGH,
						m(Webasto.ChannelId.CHARGE_POINT_POWER, new UnsignedDoublewordElement(400))),
				new FC4ReadInputRegistersTask(404, Priority.HIGH,
						m(Webasto.ChannelId.NUMBER_OF_PHASES, new UnsignedWordElement(404))),
				new FC4ReadInputRegistersTask(1000, Priority.HIGH,
						m(Webasto.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(1000))),
				new FC4ReadInputRegistersTask(1001, Priority.HIGH,
						m(Webasto.ChannelId.CHARGING_STATE, new UnsignedWordElement(1001))),
				new FC4ReadInputRegistersTask(1002, Priority.HIGH,
						m(Webasto.ChannelId.EQUIPMENT_STATE, new UnsignedWordElement(1002))),
				new FC4ReadInputRegistersTask(1004, Priority.HIGH,
						m(Webasto.ChannelId.CABLE_STATE, new UnsignedWordElement(1004))),
				new FC4ReadInputRegistersTask(1006, Priority.HIGH,
						m(Webasto.ChannelId.EVSE_FAULT_CODE, new UnsignedDoublewordElement(1006))),
				new FC4ReadInputRegistersTask(1008, Priority.HIGH,
						m(Webasto.ChannelId.CURRENT_L1, new UnsignedWordElement(1008))),
				new FC4ReadInputRegistersTask(1010, Priority.HIGH,
						m(Webasto.ChannelId.CURRENT_L2, new UnsignedWordElement(1010))),
				new FC4ReadInputRegistersTask(1012, Priority.HIGH,
						m(Webasto.ChannelId.CURRENT_L3, new UnsignedWordElement(1012))),
				new FC4ReadInputRegistersTask(1014, Priority.HIGH,
						m(Webasto.ChannelId.VOLTAGE_L1, new UnsignedWordElement(1014))),
				new FC4ReadInputRegistersTask(1016, Priority.HIGH,
						m(Webasto.ChannelId.VOLTAGE_L2, new UnsignedWordElement(1016))),
				new FC4ReadInputRegistersTask(1018, Priority.HIGH,
						m(Webasto.ChannelId.VOLTAGE_L3, new UnsignedWordElement(1018))),
				new FC4ReadInputRegistersTask(1020, Priority.HIGH,
						m(Webasto.ChannelId.ACTIVE_POWER_TOTAL, new UnsignedDoublewordElement(1020))),
				new FC4ReadInputRegistersTask(1024, Priority.HIGH,
						m(Webasto.ChannelId.ACTIVE_POWER_L1, new UnsignedDoublewordElement(1024))),
				new FC4ReadInputRegistersTask(1028, Priority.HIGH,
						m(Webasto.ChannelId.ACTIVE_POWER_L2, new UnsignedDoublewordElement(1028))),
				new FC4ReadInputRegistersTask(1032, Priority.HIGH,
						m(Webasto.ChannelId.ACTIVE_POWER_L3, new UnsignedDoublewordElement(1032))),
				new FC4ReadInputRegistersTask(1036, Priority.HIGH,
						m(Webasto.ChannelId.METER_READING, new UnsignedDoublewordElement(1036))),
				new FC4ReadInputRegistersTask(1100, Priority.HIGH,
						m(Webasto.ChannelId.SESSION_MAX_CURRENT, new UnsignedWordElement(1100))),
				new FC4ReadInputRegistersTask(1102, Priority.HIGH,
						m(Webasto.ChannelId.EVSE_MIN_CURRENT, new UnsignedWordElement(1102))),
				new FC4ReadInputRegistersTask(1104, Priority.HIGH,
						m(Webasto.ChannelId.EVSE_MAX_CURRENT, new UnsignedWordElement(1104))),
				new FC4ReadInputRegistersTask(1106, Priority.HIGH,
						m(Webasto.ChannelId.CABLE_MAX_CURRENT, new UnsignedWordElement(1106))),
				new FC4ReadInputRegistersTask(1502, Priority.HIGH,
						m(Webasto.ChannelId.SESSION_ENERGY, new UnsignedDoublewordElement(1502))),
				new FC4ReadInputRegistersTask(1504, Priority.HIGH,
						m(Webasto.ChannelId.SESSION_START_TIME, new UnsignedDoublewordElement(1504))),
				new FC4ReadInputRegistersTask(1508, Priority.HIGH,
						m(Webasto.ChannelId.SESSION_DURATION, new UnsignedDoublewordElement(1508))),
				new FC4ReadInputRegistersTask(1512, Priority.HIGH,
						m(Webasto.ChannelId.SESSION_END_TIME, new UnsignedDoublewordElement(1512))),
				new FC3ReadRegistersTask(2000, Priority.HIGH,
						m(Webasto.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(2000))),
				new FC3ReadRegistersTask(2002, Priority.HIGH,
						m(Webasto.ChannelId.FAILSAFE_TIMEOUT, new UnsignedWordElement(2002))),
				new FC3ReadRegistersTask(5004, Priority.HIGH,
						m(Webasto.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(5004))),
				new FC3ReadRegistersTask(6000, Priority.HIGH,
						m(Webasto.ChannelId.ALIVE_REGISTER, new UnsignedWordElement(6000))),
				new FC6WriteRegisterTask(2000, m(Webasto.ChannelId.FAILSAFE_CURRENT, new SignedWordElement(2000))),
				new FC6WriteRegisterTask(2002, m(Webasto.ChannelId.FAILSAFE_TIMEOUT, new SignedWordElement(2002))),
				new FC6WriteRegisterTask(5004, m(Webasto.ChannelId.CHARGING_CURRENT, new SignedWordElement(5004))),
				new FC6WriteRegisterTask(6000, m(Webasto.ChannelId.ALIVE_REGISTER, new SignedWordElement(6000)))

		);
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.minCurrent / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.maxCurrent / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		var phases = this.getPhasesAsInt();
		var current = Math.round((float) power / phases / 230f);

		/*
		 * Limits the charging value because Webasto knows only values between 6 and 32
		 */
		current = Math.min(current, 32);

		if (current < 6) {
			current = 0;
		}
		this.setCurrentLimit(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public void handleEvent(Event event) {
		final var alive = this.getAliveChannel().getNextValue();
		if (alive.isDefined() && alive.get() == 0) {
			try {
				this._setAliveValue(1);
			} catch (OpenemsError.OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
		this.readHandler.run();
		this.writeHandler.run();
	}

}
