package io.openems.edge.evcs.heidelberg.connect;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

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
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
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

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Heidelberg.Connect", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsHeidelbergConnectImpl extends AbstractOpenemsModbusComponent
		implements EvcsHeidelbergConnect, Evcs, ManagedEvcs, ModbusComponent, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EvcsHeidelbergConnectImpl.class);
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);
	private final WriteHandler writeHandler = new WriteHandler(this);
	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsHeidelbergConnectImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsHeidelbergConnect.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limits used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		/*
		 * The availability of registers depends on the layout version within the
		 * connect series.
		 * 
		 * TODO: Add Warning Channel if important registers not available because of an
		 * old software version.
		 */

		var modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(4, Priority.HIGH,
						// TODO: Check scale factors
						m(EvcsHeidelbergConnect.ChannelId.LAYOUT_VERSION, new UnsignedWordElement(4)), //
						m(EvcsHeidelbergConnect.ChannelId.RAW_STATE, new UnsignedWordElement(5)),
						m(EvcsHeidelbergConnect.ChannelId.CURRENT_L1, new UnsignedWordElement(6), SCALE_FACTOR_2),
						m(EvcsHeidelbergConnect.ChannelId.CURRENT_L2, new UnsignedWordElement(7), SCALE_FACTOR_2),
						m(EvcsHeidelbergConnect.ChannelId.CURRENT_L3, new UnsignedWordElement(8), SCALE_FACTOR_2),
						m(EvcsHeidelbergConnect.ChannelId.TEMPERATURE_PCB, new UnsignedWordElement(9)),
						m(EvcsHeidelbergConnect.ChannelId.VOLTAGE_L1, new UnsignedWordElement(10)),
						m(EvcsHeidelbergConnect.ChannelId.VOLTAGE_L2, new UnsignedWordElement(11)),
						m(EvcsHeidelbergConnect.ChannelId.VOLTAGE_L3, new UnsignedWordElement(12)),
						m(EvcsHeidelbergConnect.ChannelId.EXTERN_LOCK_STATE, new UnsignedWordElement(13)),
						m(Evcs.ChannelId.CHARGE_POWER, new UnsignedWordElement(14)), //
						new DummyRegisterElement(15, 16), // Energy since on

						/*
						 * TODO: Check doubleWord - if not possible split in two registers
						 * 
						 * high Byte = 10 → 10 * 216 VAh = 655360 VAh
						 * 
						 * low byte = 100 → 100 VAh
						 * 
						 * Result: 655360 VAh + 100 VAh = 655460 Vah
						 */
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(17)),

						/*
						 * TODO: Check doubleWord - if not possible split in two registers
						 * 
						 * high Byte = 5 → 5 * 216 VAh = 327680 VAh
						 * 
						 * low byte = 37 → 37 VAh
						 * 
						 * Result: 327680 VAh + 37 VAh = 327717 VAh
						 */
						m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(19)) //

				), new FC4ReadInputRegistersTask(100, Priority.LOW,

						m(EvcsHeidelbergConnect.ChannelId.RAW_MAXIMAL_CURRENT, new UnsignedWordElement(100)),
						m(EvcsHeidelbergConnect.ChannelId.RAW_MINIMAL_CURRENT, new UnsignedWordElement(101))
				// ... inernal use
				),

				/*
				 * Internal watchdog (currently not used).
				 * 
				 * Default: 15 seconds
				 */
				new FC3ReadRegistersTask(257, Priority.LOW,
						m(EvcsHeidelbergConnect.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257))),
				new FC6WriteRegisterTask(257,
						m(EvcsHeidelbergConnect.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257))),

				/*
				 * Remote lock (currently not used).
				 */
				new FC3ReadRegistersTask(259, Priority.LOW,
						m(EvcsHeidelbergConnect.ChannelId.REMOTE_LOCK, new UnsignedWordElement(259))),
				new FC6WriteRegisterTask(259,
						m(EvcsHeidelbergConnect.ChannelId.REMOTE_LOCK, new UnsignedWordElement(259))),

				/*
				 * Maximal current. The system can be locked by setting 0 in register 261.
				 * However, this is not displayed to the user. It is noticed that the charging
				 * does not start or is terminated. It is recommended to leave the current
				 * setting constant for 20 sec. after a change.
				 */
				new FC3ReadRegistersTask(261, Priority.LOW,
						m(EvcsHeidelbergConnect.ChannelId.APPLY_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(261),
								SCALE_FACTOR_2)),
				new FC6WriteRegisterTask(261,
						m(EvcsHeidelbergConnect.ChannelId.APPLY_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(261),
								SCALE_FACTOR_2)),

				// TODO: currently not used - default would be 0
				new FC3ReadRegistersTask(262, Priority.LOW,
						m(EvcsHeidelbergConnect.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(262),
								SCALE_FACTOR_2)),
				new FC6WriteRegisterTask(262, m(EvcsHeidelbergConnect.ChannelId.FAILSAFE_CURRENT,
						new UnsignedWordElement(262), SCALE_FACTOR_2))

		/*
		 * Further interesting blocks
		 * 
		 * [1000 - 1017] Wallbox Serial Number
		 * 
		 * [1250 - 1290] Firmware Version
		 * 
		 * [1300 - 1340] Firmware Variant
		 * 
		 * RFID Card Handling: [300] RFID Configuration Commands ... [2100] RFID Status
		 * Information TODO: Check if RFID is working independently
		 * 
		 * [2020] Wallbox Ready for Charging (FC04)
		 * 
		 * Internal MID Power Meter: [3000] Int. MID available Could be used as an
		 * information state, but should be not handled within OpenEMS as long the
		 * payment should be handled separately via a OCPP Backend
		 * 
		 * Internal HCB relevant?
		 * 
		 * Phase Switch TODO: Detect if Phase switch is possible or not (device old?)
		 * Only available for connect.solar
		 * 
		 * [500] Maximal Power Target Command Please note: If this command is used,
		 * don’t use register [501] Phase Switch Control and register [261] Maximal
		 * Current Command.
		 * 
		 * Switch manually [501] Phase Switch Control
		 * 
		 * [503] Duration Time Phase Switch (min: 15, default: 90 seconds)
		 * 
		 * [504] Waiting Time Phase Switch (min: 0, default: 300 seconds)
		 * 
		 * [505] Disconnect Simulation Command (required to set a value before
		 * switching?)
		 * 
		 * [5000] Maximal Power Set (read value of [500] were possible)
		 * 
		 * [5001] Phase Switch State (0: in progress, 1 & 3)
		 */
		);

		this.addStatusCallback();
		this.addPhaseDetectionCallback();

		return modbusProtocol;
	}

	/**
	 * Maps the EVCS state to a {@link Status}.
	 */
	private void addStatusCallback() {
		this.channel(EvcsHeidelbergConnect.ChannelId.RAW_STATE).onSetNextValue(s -> {
			HeidelbergStates state = s.asEnum();
			Status evcsStatus = switch (state) {
			case A1, A2, F -> Status.NOT_READY_FOR_CHARGING;
			case B1, C1 -> Status.CHARGING_REJECTED;
			case B2 -> Status.NOT_READY_FOR_CHARGING; // Could be also FINISHED
			case C2, D -> Status.CHARGING;
			case E, F2 -> Status.ERROR;
			default -> Status.UNDEFINED;
			};

			this._setStatus(evcsStatus);
		});
	}

	private void addPhaseDetectionCallback() {
		final Consumer<Value<Integer>> setPhasesCallback = ignore -> {

			var phases = 0;
			if (this.getCurrentL1().isDefined() && this.getCurrentL1().get() > 100) {
				phases++;
			}
			if (this.getCurrentL2().isDefined() && this.getCurrentL2().get() > 100) {
				phases++;
			}
			if (this.getCurrentL3().isDefined() && this.getCurrentL3().get() > 100) {
				phases++;
			}

			this._setPhases(phases);
		};

		this.getChargePowerChannel().onUpdate(setPhasesCallback);
	}

	@Override
	public String debugLog() {
		return "Status: " + getStatus().getName() + " | " + "Charging Power: " + this.getChargePower().get();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.currentToPower(this.config.minHwCurrent());
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.currentToPower(this.config.maxHwCurrent());
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {

		// TODO: If phase switching configured & possible, use power register

		// TODO: Change only after 20 seconds recommended (similar to
		// getMinimumTimeTillChargingLimitTaken currently only used for evcs cluster)
		var phases = this.getPhasesAsInt();
		var current = Math.round(1000f * power / phases / 230f);

		current = Math.min(current, this.config.maxHwCurrent());

		if (current < 6000) {
			current = 0;
		}
		this.setApplyChargeCurrentLimit(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		this.setApplyChargeCurrentLimit(0);
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		}
	}

	private void applyConfig(Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setPhases(Phases.THREE_PHASE.getValue());
		this._setFixedMinimumHardwarePower(this.currentToPower(config.minHwCurrent()));
		this._setFixedMaximumHardwarePower(this.currentToPower(config.maxHwCurrent()));
		this._setPowerPrecision(DEFAULT_POWER_RECISION * 0.1); // 0.1A steps
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	private Integer currentToPower(Integer current) {
		return Math.round(current / 1000f) * DEFAULT_VOLTAGE * getPhasesAsInt();
	}
}
