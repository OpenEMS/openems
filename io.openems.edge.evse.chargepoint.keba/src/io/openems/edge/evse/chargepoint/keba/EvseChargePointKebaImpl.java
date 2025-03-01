package io.openems.edge.evse.chargepoint.keba;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.TypeUtils.getAsType;

import java.net.UnknownHostException;
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

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.types.Tuple;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.chargepoint.Status;
import io.openems.edge.evse.chargepoint.keba.enums.Phase;
import io.openems.edge.evse.chargepoint.keba.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.enums.ProductTypeAndFeatures;
import io.openems.edge.evse.chargepoint.keba.enums.SetEnable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Keba", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EvseChargePointKebaImpl extends AbstractOpenemsModbusComponent implements EvseChargePointKeba,
		EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointKebaImpl.class);
	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public EvseChargePointKebaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointKeba.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), 1 /* Unit-ID */, this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), 1 /* Unit-ID */, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
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
	protected ModbusProtocol defineModbusProtocol() {
		// TODO: Add functionality to distinguish between firmware version. For firmware
		// version >= 5.22 there are several new registers. Currently it is programmed
		// for firmware version 5.14.
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1000, Priority.LOW, //
						m(EvseChargePoint.ChannelId.STATUS, new UnsignedDoublewordElement(1000),
								new ElementToChannelConverter(t -> {
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
						m(EvseChargePointKeba.ChannelId.PLUG, new UnsignedDoublewordElement(1004))),
				new FC3ReadRegistersTask(1006, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.ERROR_CODE, new UnsignedDoublewordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW, // TODO apply phase rotation
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(new UnsignedDoublewordElement(1016)).build().onUpdateCallback(value -> {
							var ptaf = ProductTypeAndFeatures.from(value);
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_PRODUCT_TYPE, ptaf.productType());
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_CABLE_OR_SOCKET, ptaf.cableOrSocket());
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_SUPPORTED_CURRENT,
									ptaf.supportedCurrent());
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_DEVICE_SERIES, ptaf.deviceSeries());
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_ENERGY_METER, ptaf.energyMeter());
							setValue(this, EvseChargePointKeba.ChannelId.PTAF_AUTHORIZATION, ptaf.authorization());
						})),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018),
								CONVERT_FIRMWARE_VERSION)),
				new FC3ReadRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(1036),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1040, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(1040))),
				new FC3ReadRegistersTask(1042, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(1042))),
				new FC3ReadRegistersTask(1044, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(1044))),
				new FC3ReadRegistersTask(1046, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.POWER_FACTOR, new UnsignedDoublewordElement(1046),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1100, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.MAX_CHARGING_CURRENT, new UnsignedDoublewordElement(1100),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(1500, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.RFID, new UnsignedDoublewordElement(1500))),
				new FC3ReadRegistersTask(1502, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(1502))),
				new FC3ReadRegistersTask(1550, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.PHASE_SWITCH_SOURCE, new UnsignedDoublewordElement(1550))),
				new FC3ReadRegistersTask(1552, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.PHASE_SWITCH_STATE, new UnsignedDoublewordElement(1552))),
				new FC3ReadRegistersTask(1600, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.FAILSAFE_CURRENT_SETTING, new UnsignedDoublewordElement(1600))),
				new FC3ReadRegistersTask(1602, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.FAILSAFE_TIMEOUT_SETTING, new UnsignedDoublewordElement(1602))), //

				new FC6WriteRegisterTask(5004,
						m(EvseChargePointKeba.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(5004))),
				new FC6WriteRegisterTask(5010, // TODO Scalefactor for Unit: 10 Wh
						m(EvseChargePointKeba.ChannelId.SET_ENERGY_LIMIT, new UnsignedWordElement(5010))),
				new FC6WriteRegisterTask(5012,
						m(EvseChargePointKeba.ChannelId.SET_UNLOCK_PLUG, new UnsignedWordElement(5012))),
				new FC6WriteRegisterTask(5014,
						m(EvseChargePointKeba.ChannelId.SET_ENABLE, new UnsignedWordElement(5014))),
				new FC6WriteRegisterTask(5050,
						m(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_SOURCE, new UnsignedWordElement(5050))),
				new FC6WriteRegisterTask(5052,
						m(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_STATE, new UnsignedWordElement(5052))));
	}

	@Override
	public ChargeParams getChargeParams() {
		var config = this.config;
		var phaseSwitchState = this.getPhaseSwitchState().actual;
		if (config == null || config.readOnly() || phaseSwitchState == null) {
			return null;
		}
		var singlePhaseLimit = new Limit(SingleThreePhase.SINGLE, 6000, 32000);
		var threePhaseLimit = new Limit(SingleThreePhase.THREE, 6000, 32000);

		var limit = switch (config.phase()) {
		case FIXED_SINGLE -> singlePhaseLimit;
		case FIXED_THREE -> threePhaseLimit;
		case HAS_S10_PHASE_SWITCHING_DEVICE //
			-> switch (phaseSwitchState) { // Read current phase switch state
			case SINGLE -> singlePhaseLimit;
			case THREE -> threePhaseLimit;
			};
		};

		var profiles = ImmutableList.<Profile>builder();
		if (config.phase() == Phase.HAS_S10_PHASE_SWITCHING_DEVICE) {
			profiles.add(switch (phaseSwitchState) {
			case SINGLE -> new Profile.PhaseSwitchToThreePhase(threePhaseLimit);
			case THREE -> new Profile.PhaseSwitchToSinglePhase(singlePhaseLimit);
			});
		}

		return new ChargeParams(limit, profiles.build());
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString()) //
					.append("|SetEnable:") //
					.append(this.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_ENABLE).value().asString());
		}
		return b.toString();
	}

	@Override
	public void apply(ApplyCharge applyCharge, ImmutableList<Profile.Command> profileCommands) {
		// TODO this apply method should use a StateMachine. Consider having the
		// StateMachine inside EVSE Single-Controller

		// TODO Phase Switch Three-to-Single is always possible without interruption
		// TODO Allow Phase Switch always if no car is connected
		final var p = EvseChargePointKebaImpl.this;
		final var now = Instant.now();

		this.handleApplyCharge(now, applyCharge);

		for (var pc : profileCommands) {
			switch (pc) {
			case Profile.PhaseSwitchToThreePhase.Command tp ->
				this.handlePhaseSwitch(p, now, PhaseSwitchState.Actual.THREE);
			case Profile.PhaseSwitchToSinglePhase.Command sp ->
				this.handlePhaseSwitch(p, now, PhaseSwitchState.Actual.SINGLE);
			}
		}
	}

	private Tuple<Instant, ApplyCharge> previousApplyCharge = null;

	private void handleApplyCharge(Instant now, ApplyCharge ac) {
		if (this.previousApplyCharge != null && Duration.between(this.previousApplyCharge.a(), now).getSeconds() < 5) {
			return;
		}
		this.previousApplyCharge = Tuple.of(now, ac);

		this.logDebug("Apply " + ac);
		try {
			var setEnable = this.<EnumWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_ENABLE);
			switch (ac) {
			case ApplyCharge.SetCurrent sc -> {
				setEnable.setNextWriteValue(SetEnable.ENABLE);
				var setChargingCurrent = this
						.<IntegerWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_CHARGING_CURRENT);
				setChargingCurrent.setNextWriteValue(sc.current());
			}
			case ApplyCharge.Zero z -> {
				setEnable.setNextWriteValue(SetEnable.DISABLE);
			}
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private Tuple<Instant, ApplyCharge> previousPhaseSwitch = null;

	private void handlePhaseSwitch(EvseChargePointKebaImpl p, Instant now, PhaseSwitchState.Actual pss) {
		if (this.previousPhaseSwitch != null && Duration.between(this.previousPhaseSwitch.a(), now).getSeconds() < 5) {
			return;
		}

		p.logInfo(p.log, "[" + p.id() + "] Apply Phase Switch to " + pss);
		try {
			// Set Phase Switch Source to MODBUS if it was not set
			if (p.getPhaseSwitchSource() == PhaseSwitchSource.VIA_MODBUS) {
				var setPhaseSwitchSource = p
						.<EnumWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_SOURCE);
				setPhaseSwitchSource.setNextWriteValue(PhaseSwitchSource.VIA_MODBUS);
			}

			// Apply actual phase switch
			// TODO evaluate if this has to be more complicated, i.e. wait for a while or
			// block any concurrent writes to SET_CHARGING_CURRENT.
			var setPhaseSwitchState = p.<EnumWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_STATE);
			setPhaseSwitchState.setNextWriteValue(//
					switch (pss) {
					case SINGLE -> PhaseSwitchState.SINGLE;
					case THREE -> PhaseSwitchState.THREE;
					});
			// TODO set PHASE_SWITCH_STATE prio to HIGH to track change faster
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	/**
	 * Converts an unsigned 32-bit integer value to a firmware version string.
	 * 
	 * @param value the unsigned 32-bit integer value representing the firmware
	 *              version
	 * @return the firmware version string in the format "major.minor.patch" or null
	 */
	protected static final ElementToChannelConverter CONVERT_FIRMWARE_VERSION = new ElementToChannelConverter(obj -> {
		if (obj == null) {
			return null;
		}
		var value = (long) getAsType(LONG, obj);
		// Extract major, minor, and patch versions using bit manipulation
		return new StringBuilder() //
				.append((value >> 24) & 0xFF) //
				.append(".") //
				.append((value >> 16) & 0xFF) //
				.append(".") //
				.append((value >> 8) & 0xFF) //
				.toString();
	});

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
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
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());
			break;
		}
	}
}
