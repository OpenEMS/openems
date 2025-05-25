package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.TypeUtils.getAsType;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.UnknownHostException;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.keba.common.CommonConfig;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.Utils;
import io.openems.edge.evse.chargepoint.keba.common.enums.ProductTypeAndFeatures;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Keba.Modbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointKebaModbusImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointKebaModbus, EvseChargePointKeba, EvseChargePoint, ElectricityMeter, OpenemsComponent,
		TimedataProvider, EventHandler, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointKebaModbusImpl.class);
	private final Utils utils = new Utils(this);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private CommonConfig config;

	public EvseChargePointKebaModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointKeba.ChannelId.values(), //
				EvseChargePointKebaModbus.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = CommonConfig.from(config);
		if (super.activate(context, config.id(), config.alias(), config.enabled(), 1 /* Unit-ID */, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = CommonConfig.from(config);
		if (super.modified(context, config.id(), config.alias(), config.enabled(), 1 /* Unit-ID */, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return Utils.getMeterType(this.config);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotated = this.getPhaseRotation();

		// TODO: Add functionality to distinguish between firmware version. For firmware
		// version >= 5.22 there are several new registers. Currently it is programmed
		// for firmware version 5.14.
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1000, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.CHARGING_STATE, new UnsignedDoublewordElement(1000))), //
				new FC3ReadRegistersTask(1004, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.CABLE_STATE, new UnsignedDoublewordElement(1004))),
				new FC3ReadRegistersTask(1006, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.ERROR_CODE, new UnsignedDoublewordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW, //
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(new UnsignedDoublewordElement(1016)).build().onUpdateCallback(value -> {
							var ptaf = ProductTypeAndFeatures.from(value);
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_PRODUCT_TYPE, ptaf.productType());
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_CABLE_OR_SOCKET,
									ptaf.cableOrSocket());
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_SUPPORTED_CURRENT,
									ptaf.supportedCurrent());
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_DEVICE_SERIES, ptaf.deviceSeries());
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_ENERGY_METER, ptaf.energyMeter());
							setValue(this, EvseChargePointKebaModbus.ChannelId.PTAF_AUTHORIZATION,
									ptaf.authorization());
						})),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018),
								CONVERT_FIRMWARE_VERSION)),
				new FC3ReadRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3)
								.onUpdateCallback(this.calculateActivePowerL1L2L3)),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(1036),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1040, Priority.LOW, //
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(1040), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1042, Priority.LOW, //
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(1042), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1044, Priority.LOW, //
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(1044), SCALE_FACTOR_3)),
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
		return this.utils.getChargeParams(this.config);
	}

	// TODO handle phase switch
	// if (config.phase() == Phase.HAS_S10_PHASE_SWITCHING_DEVICE) {
	// profiles.add(switch (phaseSwitchState) {
	// case SINGLE -> new Profile.PhaseSwitchToThreePhase(threePhaseLimit);
	// case THREE -> new Profile.PhaseSwitchToSinglePhase(singlePhaseLimit);
	// });
	// }
	// if (config.p30HasS10PhaseSwitchingDevice()) {
	// var phaseSwitchState = this.getPhaseSwitchState().actual;
	// switch (phaseSwitchState) {
	// case SINGLE_PHASE -> new Profile.PhaseSwitchToThreePhase(threePhaseLimit);
	// case THREE_PHASE -> new Profile.PhaseSwitchToSinglePhase(singlePhaseLimit);
	// }
	// }
	// for (var pc : profileCommands) {
	// switch (pc) {
	// case Profile.PhaseSwitchToThreePhase.Command tp ->
	// this.handlePhaseSwitch(p, now, PhaseSwitchState.Actual.THREE);
	// case Profile.PhaseSwitchToSinglePhase.Command sp ->
	// this.handlePhaseSwitch(p, now, PhaseSwitchState.Actual.SINGLE);
	// }
	// }

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
	public void apply(ChargePointActions actions) {
		this.utils.handleApplyCharge(actions);
	}

	// TODO this currently sometimes triggers a fuse ^^
	// private Tuple<Instant, ApplyCharge> previousPhaseSwitch = null;
	// private void handlePhaseSwitch(EvseChargePointKebaImpl p, Instant now,
	// PhaseSwitchState.Actual pss) {
	// if (this.previousPhaseSwitch != null &&
	// Duration.between(this.previousPhaseSwitch.a(), now).getSeconds() < 5) {
	// return;
	// }
	//
	// p.logInfo(p.log, "[" + p.id() + "] Apply Phase Switch to " + pss);
	// try {
	// // Set Phase Switch Source to MODBUS if it was not set
	// if (p.getPhaseSwitchSource() == PhaseSwitchSource.VIA_MODBUS) {
	// var setPhaseSwitchSource = p
	// .<EnumWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_SOURCE);
	// setPhaseSwitchSource.setNextWriteValue(PhaseSwitchSource.VIA_MODBUS);
	// }
	//
	// // Apply actual phase switch
	// // TODO evaluate if this has to be more complicated, i.e. wait for a while or
	// // block any concurrent writes to SET_CHARGING_CURRENT.
	// var setPhaseSwitchState =
	// p.<EnumWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_STATE);
	// setPhaseSwitchState.setNextWriteValue(//
	// switch (pss) {
	// case SINGLE -> PhaseSwitchState.SINGLE;
	// case THREE -> PhaseSwitchState.THREE;
	// });
	// // TODO set PHASE_SWITCH_STATE prio to HIGH to track change faster
	// } catch (OpenemsNamedException e) {
	// e.printStackTrace();
	// }
	// }

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
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.utils.onBeforeProcessImage();
		}
		}
	}

	/**
	 * On Update of ACTIVE_POWER, calculates ACTIVE_POWER_L1, L2 and L3 from CURRENT
	 * and VOLTAGE values and distributes the power to match the sum.
	 */
	private final Consumer<Long> calculateActivePowerL1L2L3 = (activePower) -> {
		var currentL1 = this.getCurrentL1Channel().getNextValue().get();
		var currentL2 = this.getCurrentL2Channel().getNextValue().get();
		var currentL3 = this.getCurrentL3Channel().getNextValue().get();
		var voltageL1 = this.getVoltageL1Channel().getNextValue().get();
		var voltageL2 = this.getVoltageL2Channel().getNextValue().get();
		var voltageL3 = this.getVoltageL3Channel().getNextValue().get();
		final Integer activePowerL1;
		final Integer activePowerL2;
		final Integer activePowerL3;
		if (activePower == null || currentL1 == null || currentL2 == null || currentL3 == null || voltageL1 == null
				|| voltageL2 == null || voltageL3 == null) {
			activePowerL1 = null;
			activePowerL2 = null;
			activePowerL3 = null;
		} else {
			var pL1 = (voltageL1 / 1000F) * (currentL1 / 1000F);
			var pL2 = (voltageL2 / 1000F) * (currentL2 / 1000F);
			var pL3 = (voltageL3 / 1000F) * (currentL3 / 1000F);
			var pSum = pL1 + pL2 + pL3;
			var factor = activePower / pSum / 1000F; // distribute power to match sum
			activePowerL1 = round(pL1 * factor);
			activePowerL2 = round(pL2 * factor);
			activePowerL3 = round(pL3 * factor);
		}
		this._setActivePowerL1(activePowerL1);
		this._setActivePowerL2(activePowerL2);
		this._setActivePowerL3(activePowerL3);
	};
}
