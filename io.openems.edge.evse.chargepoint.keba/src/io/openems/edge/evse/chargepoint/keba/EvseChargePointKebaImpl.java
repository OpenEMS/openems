package io.openems.edge.evse.chargepoint.keba;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.type.TypeUtils.getAsType;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
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
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Status;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Keba", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvseChargePointKebaImpl extends AbstractOpenemsModbusComponent implements EvseChargePointKeba,
		EvseChargePoint, ElectricityMeter, OpenemsComponent, EventHandler, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointKebaImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

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
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.config.readOnly()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			// this.writeHandler.run();
			break;
		}
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
				new FC3ReadRegistersTask(1008, Priority.LOW, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.PRODUCT_TYPE, new UnsignedDoublewordElement(1016))),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(EvseChargePointKeba.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018),
								new ElementToChannelConverter(t -> {
									return convertToFirmwareVersion(getAsType(LONG, t));
								}))),
				new FC3ReadRegistersTask(1020, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(1036))),
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
						m(EvseChargePointKeba.ChannelId.SET_PHASE_SWITCH_TOGGLE, new UnsignedWordElement(5050))),
				new FC6WriteRegisterTask(5052,
						m(EvseChargePointKeba.ChannelId.SET_TRIGGER_PHASE_SWITCH, new UnsignedWordElement(5052))));
	}

	@Override
	public ChargeParams getChargeParams() {
		var config = this.config;
		if (config == null || config.readOnly()) {
			return null;
		}
		var limit = switch (config.phase()) {
		// TODO adjust phase for phase rotation
		case SINGLE -> new Limit(io.openems.edge.evse.api.Phase.L1, 6000, 32000);
		case THREE -> new Limit(io.openems.edge.evse.api.Phase.ALL, 6000, 32000);
		};
		return new ChargeParams(limit, this.applyChargeCallback);
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

	private final class ApplyChargeCallback implements Consumer<ApplyCharge> {

		private Tuple<Instant, ApplyCharge> previous = null;

		@Override
		public void accept(ApplyCharge ac) {
			var p = EvseChargePointKebaImpl.this;
			var now = Instant.now();
			if (this.previous != null && Duration.between(this.previous.a(), now).getSeconds() < 5) {
				p.logInfo(p.log, "[" + p.id() + "] NOT applying " + ac);
				return;
			}
			this.previous = Tuple.of(now, ac);

			p.logInfo(p.log, "[" + p.id() + "] Apply " + ac);
			try {
				var setEnable = p.<IntegerWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_ENABLE);
				switch (ac) {
				case ApplyCharge.SetCurrent sc -> {
					setEnable.setNextWriteValue(1 /* Enable charging station (Charging) */);
					var setChargingCurrent = p
							.<IntegerWriteChannel>channel(EvseChargePointKeba.ChannelId.SET_CHARGING_CURRENT);
					setChargingCurrent.setNextWriteValue(sc.current());
				}
				case ApplyCharge.Zero z -> {
					setEnable.setNextWriteValue(0 /* Disable charging station (Suspended mode) */);
				}
				}
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
	}

	private final ApplyChargeCallback applyChargeCallback = new ApplyChargeCallback();

	/**
	 * Converts an unsigned 32-bit integer value to a firmware version string.
	 * 
	 * @param value the unsigned 32-bit integer value representing the firmware
	 *              version
	 * @return the firmware version string in the format "major.minor.patch" or null
	 */
	protected static String convertToFirmwareVersion(Long value) {
		if (value == null) {
			return null;
		}
		var hexValue = Long.toHexString(value & 0xFFFFFFFFL).toUpperCase();
		while (hexValue.length() < 7) {
			hexValue = "0" + hexValue;
		}
		int major = Integer.parseInt(hexValue.substring(0, 1), 16);
		int minor = Integer.parseInt(hexValue.substring(1, 3), 16);
		int patch = Integer.parseInt(hexValue.substring(3, 5), 16);
		return major + "." + minor + "." + patch;
	}
}
