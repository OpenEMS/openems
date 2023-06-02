package io.openems.edge.evcs.webasto.next;

import java.util.function.Consumer;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evcs.webasto.next.enums.ChargePointState;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Webasto.Next", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsWebastoNextImpl extends AbstractOpenemsModbusComponent
		implements EvcsWebastoNext, Evcs, ManagedEvcs, ModbusComponent, OpenemsComponent, EventHandler {

	private static final int DEFAULT_LIFE_BIT = 1;
	private static final int DETECT_PHASE_ACTIVITY = 100; // W

	private final Logger log = LoggerFactory.getLogger(EvcsWebastoNext.class);

	/** Handles charge states. */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);
	/** Processes the controller's writes to this evcs component. */
	private final WriteHandler writeHandler = new WriteHandler(this);

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public EvcsWebastoNextImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsWebastoNext.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

		this.applyConfig(context, config);

		this.getModbusCommunicationFailedChannel()
				.onSetNextValue(t -> this._setChargingstationCommunicationFailed(t.orElse(false)));
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	private void applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(config.minHwCurrent() / 1000 * 3 * 230);
		this._setFixedMaximumHardwarePower(config.maxHwCurrent() / 1000 * 3 * 230);
		/*
		 * TODO: PowerPrecision need to be tested if it is really a 1A step because for
		 * limits set as power [W], is is normally PowerPrecision of 1 (Anyways, Channel
		 * is no used for now)
		 */
		this._setPowerPrecision(230);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		// Cannot read the gaps, therefore there are so many tasks
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1000, Priority.HIGH,
						m(EvcsWebastoNext.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(1000)), //
						new DummyRegisterElement(1001), // Charge State - Set already by the WriteHandler
						m(EvcsWebastoNext.ChannelId.EVSE_STATE, new UnsignedWordElement(1002))), //
				new FC3ReadRegistersTask(1004, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.CABLE_STATE, new UnsignedWordElement(1004))), //
				new FC3ReadRegistersTask(1006, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.EVSE_ERROR_CODE, new UnsignedWordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.CURRENT_L1, new UnsignedWordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.CURRENT_L2, new UnsignedWordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.CURRENT_L3, new UnsignedWordElement(1012))),
				new FC3ReadRegistersTask(1020, Priority.HIGH,
						m(Evcs.ChannelId.CHARGE_POWER, new UnsignedDoublewordElement(1020))),
				new FC3ReadRegistersTask(1024, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.POWER_L1, new UnsignedDoublewordElement(1024))),
				new FC3ReadRegistersTask(1028, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.POWER_L2, new UnsignedDoublewordElement(1028))),
				new FC3ReadRegistersTask(1032, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.POWER_L3, new UnsignedDoublewordElement(1032))),
				new FC3ReadRegistersTask(1036, Priority.LOW,
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(1036))),
				new FC3ReadRegistersTask(1100, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.MAX_HW_CURRENT, new UnsignedWordElement(1100))),
				new FC3ReadRegistersTask(1102, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.MIN_HW_CURRENT, new UnsignedWordElement(1102))),
				new FC3ReadRegistersTask(1104, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.MAX_EVSE_CURRENT, new UnsignedWordElement(1104))),
				new FC3ReadRegistersTask(1106, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.MAX_CABLE_CURRENT, new UnsignedWordElement(1106))),
				new FC3ReadRegistersTask(1108, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.MAX_EV_CURRENT, new UnsignedWordElement(1108))),
				// TODO EvcsWebastoNext.ChannelId.LAST_ENERGY_SESSION: This register remains 0
				// during the session,
				// and set a value at the end. But for the UI we need the
				// Energy charged during the session is running as well.
				new FC3ReadRegistersTask(1502, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.LAST_ENERGY_SESSION, new UnsignedWordElement(1502))),
				new FC3ReadRegistersTask(1504, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.START_TIME, new UnsignedDoublewordElement(1504))),
				new FC3ReadRegistersTask(1508, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.CHARGE_SESSION_TIME, new UnsignedDoublewordElement(1508))),
				new FC3ReadRegistersTask(1512, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.END_TIME, new UnsignedDoublewordElement(1512))),
				new FC3ReadRegistersTask(1620, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.SMART_VEHICLE_DETECTED, new UnsignedWordElement(1620))),
				new FC3ReadRegistersTask(2000, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.SAFE_CURRENT, new UnsignedWordElement(2000))),
				new FC3ReadRegistersTask(2002, Priority.LOW,
						m(EvcsWebastoNext.ChannelId.COM_TIMEOUT, new UnsignedWordElement(2002))),
				new FC16WriteRegistersTask(5000, //
						m(EvcsWebastoNext.ChannelId.EV_SET_CHARGE_POWER_LIMIT, new UnsignedDoublewordElement(5000))), //
				new FC6WriteRegisterTask(5004, //
						m(EvcsWebastoNext.ChannelId.CHARGE_CURRENT, new UnsignedWordElement(5004))), //
				new FC6WriteRegisterTask(5006, //
						m(EvcsWebastoNext.ChannelId.START_CANCEL_CHARGING_SESSION, new UnsignedWordElement(5006))),
				new FC3ReadRegistersTask(6000, Priority.LOW, //
						m(EvcsWebastoNext.ChannelId.LIFE_BIT, new UnsignedWordElement(6000))), //
				new FC6WriteRegisterTask(6000, //
						m(EvcsWebastoNext.ChannelId.LIFE_BIT, new UnsignedWordElement(6000))) //
		);
		this.addStatusListener();
		this.addPhasesListener();
		return modbusProtocol;
	}

	private void addStatusListener() {
		this.channel(EvcsWebastoNext.ChannelId.CHARGE_POINT_STATE).onSetNextValue(s -> {
			ChargePointState state = s.asEnum();
			/**
			 * Maps the raw state into a {@link Status}.
			 */
			switch (state) {
			case CHARGING:
				this._setStatus(Status.CHARGING);
				break;
			case NO_PERMISSION:
			case CHARGING_STATION_RESERVED:
				this._setStatus(Status.CHARGING_REJECTED);
				break;
			case ERROR:
				this._setStatus(Status.ERROR);
				break;
			case NO_VEHICLE_ATTACHED:
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case CHARGING_PAUSED:
				this._setStatus(Status.CHARGING_FINISHED);
				break;
			case UNDEFINED:
			default:
				this._setStatus(Status.UNDEFINED);
			}
		});
	}

	private void addPhasesListener() {
		final Consumer<Value<Integer>> setPhases = ignore -> {
			var phases = 0;
			if (this.getPowerL1().orElse(0) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (this.getPowerL2().orElse(0) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (this.getPowerL3().orElse(0) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (phases == 0) {
				phases = 3;
			}
			this._setPhases(phases);
		};
		this.getPowerL1Channel().onUpdate(setPhases);
		this.getPowerL2Channel().onUpdate(setPhases);
		this.getPowerL3Channel().onUpdate(setPhases);
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
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		this.setEvSetChargePowerLimit(power);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		this.applyChargePowerLimit(0);
		return true;
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
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			this.updateLifeBit();
			break;
		}
	}

	private void updateLifeBit() {
		try {
			this.setLifeBit(DEFAULT_LIFE_BIT);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}
