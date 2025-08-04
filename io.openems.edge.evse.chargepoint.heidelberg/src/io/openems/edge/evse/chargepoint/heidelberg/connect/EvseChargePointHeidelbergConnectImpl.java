package io.openems.edge.evse.chargepoint.heidelberg.connect;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.chargepoint.heidelberg.connect.enums.ChargingState.stateFromValue;

import java.time.Duration;
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
import io.openems.common.types.Tuple;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.ChargingState;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.LockState;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.ReadyForCharging;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Heidelberg.Connect", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointHeidelbergConnectImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointHeidelbergConnect, ModbusComponent, OpenemsComponent, TimedataProvider,
		EvseChargePoint, EventHandler, ElectricityMeter {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointHeidelbergConnectImpl.class);
	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointHeidelbergConnectImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointHeidelbergConnect.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
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

	private void applyConfig(Config config) {
		this.config = config;
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
		 */
		// TODO: Add functionality to distinguish between different series if needed.
		// register 4–13 are similar for the most of the series
		var modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(4, Priority.HIGH,
						// TODO: Check scale factors
						m(EvseChargePointHeidelbergConnect.ChannelId.LAYOUT_VERSION, new UnsignedWordElement(4)), //
						m(EvseChargePointHeidelbergConnect.ChannelId.CHARGING_STATE, new UnsignedWordElement(5),
								new ElementToChannelConverter(t -> {
									return stateFromValue(TypeUtils.<Integer>getAsType(INTEGER, t)).state;
								})), //

						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(6), SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(7), SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(8), SCALE_FACTOR_2),
						m(EvseChargePointHeidelbergConnect.ChannelId.TEMPERATURE_PCB, new UnsignedWordElement(9)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(10)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(11)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(12)),
						m(EvseChargePointHeidelbergConnect.ChannelId.EXTERN_LOCK_STATE, new UnsignedWordElement(13)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(14)), //
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
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(17)),

						/*
						 * TODO: Check doubleWord - if not possible split in two registers
						 * 
						 * high Byte = 5 → 5 * 216 VAh = 327680 VAh
						 * 
						 * low byte = 37 → 37 VAh
						 * 
						 * Result: 327680 VAh + 37 VAh = 327717 VAh
						 */
						m(EvseChargePointHeidelbergConnect.ChannelId.HEIDELBERG_ENERGY_SESSION,
								new UnsignedDoublewordElement(19))),

				new FC4ReadInputRegistersTask(100, Priority.LOW,

						m(EvseChargePointHeidelbergConnect.ChannelId.RAW_MAXIMAL_CURRENT, new UnsignedWordElement(100)),
						m(EvseChargePointHeidelbergConnect.ChannelId.RAW_MINIMAL_CURRENT,
								new UnsignedWordElement(101))),

				/*
				 * Internal watchdog (currently not used).
				 * 
				 * Default: 15 seconds
				 */
				new FC3ReadRegistersTask(257, Priority.LOW,
						m(EvseChargePointHeidelbergConnect.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257))),
				new FC6WriteRegisterTask(257,
						m(EvseChargePointHeidelbergConnect.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257))),

				/*
				 * Remote lock (currently not used).
				 */
				new FC3ReadRegistersTask(259, Priority.LOW,
						m(EvseChargePointHeidelbergConnect.ChannelId.REMOTE_LOCK, new UnsignedWordElement(259))),
				new FC6WriteRegisterTask(259,
						m(EvseChargePointHeidelbergConnect.ChannelId.REMOTE_LOCK, new UnsignedWordElement(259))),

				/*
				 * Maximal current. The system can be locked by setting 0 in register 261.
				 * However, this is not displayed to the user. It is noticed that the charging
				 * does not start or is terminated. It is recommended to leave the current
				 * setting constant for 20 sec. after a change.
				 */
				new FC3ReadRegistersTask(261, Priority.LOW,
						m(EvseChargePointHeidelbergConnect.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(261),
								SCALE_FACTOR_2)),
				new FC6WriteRegisterTask(261,
						m(EvseChargePointHeidelbergConnect.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(261),
								SCALE_FACTOR_2)),

				// TODO: currently not used - default would be 0
				new FC3ReadRegistersTask(262, Priority.LOW,
						m(EvseChargePointHeidelbergConnect.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(262),
								SCALE_FACTOR_2)),
				new FC6WriteRegisterTask(262,
						m(EvseChargePointHeidelbergConnect.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(262),
								SCALE_FACTOR_2)),

				// Phase Switch Control
				new FC3ReadRegistersTask(501, Priority.LOW,
						m(EvseChargePointHeidelbergConnect.ChannelId.PHASE_SWITCH_CONTROL,
								new UnsignedWordElement(501))),
				new FC6WriteRegisterTask(501, m(EvseChargePointHeidelbergConnect.ChannelId.PHASE_SWITCH_CONTROL,
						new UnsignedWordElement(501)))

		);

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointHeidelbergConnect.ChannelId.DEBUG_SET_CHARGING_CURRENT).value()
							.asString()) //
					.append("|Used Phases:") //
					.append(this.channel(EvseChargePointHeidelbergConnect.ChannelId.PHASE_SWITCH_CONTROL).value()
							.asString());
		}
		return b.toString();
	}

	private void logIfDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		var config = this.config;
		final var phases = this.getWiring();
		if (config == null || config.readOnly()) {
			return null;
		}

		// TODO: Add phase switching ability

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, 6000, 16000)) //
				.build();
	}

	private SingleOrThreePhase getWiring() {
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
		}

		this.logIfDebug("Fallback ");
		// TODO: Check if the read value changing directly or after 90sec.
		return switch (this.getPhaseSwitchControl()) {
		case SINGLE -> SINGLE_PHASE;
		case THREE -> THREE_PHASE;
		case UNDEFINED ->
			throw new UnsupportedOperationException("Unimplemented case: " + this.getPhaseSwitchControl());
		};
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.config.readOnly()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());
			// Could be set directly in modbus mapping when the value can be used directly
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING,
					((ReadyForCharging) this.getReadyForChargingChannel().getNextValue().asEnum()).isReady);

			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING,
					evaluateIsReadyForCharging(
							this.channel(EvseChargePointHeidelbergConnect.ChannelId.CHARGING_STATE).getNextValue()
									.asEnum(),
							this.channel(EvseChargePointHeidelbergConnect.ChannelId.EXTERN_LOCK_STATE).getNextValue()
									.asEnum()));

		}
		}
	}

	protected static boolean evaluateIsReadyForCharging(ChargingState chargingState, LockState lockState) {
		if (lockState != LockState.UNLOCKED) {
			return false;
		}
		return switch (chargingState) {
		// C1 could be both (Depends what command/setting was pausing the charge)
		case B2, C1, C2 -> true;
		default -> false;
		};
	}

	@Override
	public void apply(ChargePointActions actions) {
		// TODO this apply method should use a StateMachine. Consider having the
		// StateMachine inside EVSE Single-Controller

		// TODO Phase Switch Three-to-Single is always possible without interruption
		// TODO Allow Phase Switch always if no car is connected
		final var now = Instant.now();

		final var current = actions.getApplySetPointInMilliAmpere().value();

		this.handleApplyCharge(now, current);
	}

	private Tuple<Instant, Integer> previousCurrent = null;

	private void handleApplyCharge(Instant now, int current) {
		if (this.previousCurrent != null && Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			return;
		}
		this.previousCurrent = Tuple.of(now, current);

		try {
			this.setChargingCurrent(current);

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
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
	public boolean isReadOnly() {
		return this.config.readOnly();
	}
}
