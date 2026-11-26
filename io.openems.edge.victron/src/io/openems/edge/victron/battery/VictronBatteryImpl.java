package io.openems.edge.victron.battery;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_MINUS_1_AND_INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_MINUS_2;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.victron.batteryinverter.VictronBatteryInverterImpl;
import io.openems.edge.victron.ess.VictronEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Victron.Battery", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@GenerateTargetsFromReferences({ "Modbus", "ess" })
public class VictronBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, VictronBattery, ModbusComponent, OpenemsComponent, ModbusSlave {

	public static final int DEFAULT_UNIT_ID = 225;
	public static final int BATTERY_VOLTAGE = 48;

	private int minSocPercentage;

	private final Logger log = LoggerFactory.getLogger(VictronBatteryInverterImpl.class);

	protected Config config;

	public VictronBatteryImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				VictronBattery.ChannelId.values() //
		);
	}

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(//
			policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL, //
			target = "(&(id=${config.ess_id})(enabled=true))")
	private volatile VictronEss ess;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private volatile List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, target = "(enabled=true)")
	private volatile List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	private int maxSocPercentage;

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (config.nominalBatteryVoltage() <= 0) {
			throw new IllegalArgumentException("Nominal battery voltage must be greater than zero.");
		}
		if (config.batterySource() == BatterySource.DBUS_AGGREGATOR
				&& (!Double.isFinite(config.dischargeMinVoltage()) || config.dischargeMinVoltage() <= 0
						|| config.dischargeMinVoltage() > Integer.MAX_VALUE)) {
			throw new IllegalArgumentException(
					"Minimum discharge voltage must be a positive, finite value within the supported range for the D-Bus battery aggregator.");
		}
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());

		if (config.batterySource() == BatterySource.DBUS_AGGREGATOR) {
			this._setDischargeMinVoltage((int) Math.ceil(config.dischargeMinVoltage()));
		}

		if (this.ess != null) {
			this.ess.setBattery(this);
		}

		this.installListener();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "SoC: " + this.getSoc();
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO implement battery start/stop if needed
		this._setStartStop(value);
	}

	private void checkSocControllers() {
		if (this.ess == null) {
			this.logDebug(this.log, "No Controller active on ESS, exiting.");
			this.minSocPercentage = 0;
			this.maxSocPercentage = 100;
			return;
		}

		this.minSocPercentage = Utils.getEssMinSocPercentage(this.ctrlLimitTotalDischarges,
				this.ctrlEmergencyCapacityReserves);
		this.maxSocPercentage = 100; // Default max SoC

		this.logDebug(this.log, "checkSocControllers: MinSoC set to " + this.minSocPercentage + ", MaxSoC set to "
				+ this.maxSocPercentage);
	}

	private void installListener() {
		this.getCapacityInAmphoursChannel().onUpdate(value -> this.updateCapacityAndUsableSoc());
		this.getSocChannel().onUpdate(value -> this.updateCapacityAndUsableSoc());
	}

	private void updateCapacityAndUsableSoc() {
		var capacityAh = this.getCapacityInAmphours().get();
		if (capacityAh == null) {
			return;
		}
		if (capacityAh < 0) {
			this.logError(this.log, "Invalid battery capacity received: " + capacityAh + " Ah.");
			return;
		}

		var capacityWh = (int) Math.round(capacityAh * this.config.nominalBatteryVoltage());
		this._setCapacity(capacityWh);

		var soc = this.getSoc().get();
		if (soc == null || this.ess == null) {
			return;
		}

		this.checkSocControllers();
		var minSoc = Math.max(0, Math.min(this.minSocPercentage, 100));
		var maxSoc = Math.max(minSoc, Math.min(this.maxSocPercentage, 100));
		int usableSoc;
		if (soc <= minSoc) {
			usableSoc = 0;
		} else if (soc >= maxSoc) {
			usableSoc = 100;
		} else {
			usableSoc = (int) (((double) (soc - minSoc) / (maxSoc - minSoc)) * 100);
		}
		var usableCapacityWh = (int) (capacityWh * ((double) (soc - minSoc) / 100));
		var maxUsableCapacityWh = (int) (capacityWh * ((double) (maxSoc - minSoc) / 100));
		usableCapacityWh = Math.max(0, Math.min(usableCapacityWh, maxUsableCapacityWh));

		this.ess._setUseableSoc(usableSoc);
		this.ess._setUseableCapacity(usableCapacityWh);
	}

	@Override
	public void setMinSocPercentage(int minSocPercentage) {
		this.minSocPercentage = minSocPercentage;

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		if (this.config.batterySource() == BatterySource.DBUS_AGGREGATOR) {
			return this.defineDbusAggregatorModbusProtocol();
		}

		return this.defineCanBmsModbusProtocol();
	}

	private ModbusProtocol defineDbusAggregatorModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(259, Priority.HIGH,
						this.m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(259), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(261, Priority.HIGH,
						this.m(Battery.ChannelId.CURRENT, new SignedWordElement(261), SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.TEMPERATURE, new SignedWordElement(262),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(266, Priority.HIGH,
						this.m(Battery.ChannelId.SOC, new UnsignedWordElement(266), SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(305, Priority.HIGH,
						this.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(305),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(307, Priority.HIGH,
						this.m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(307),
								SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(308),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.CAPACITY_IN_AMPHOURS, new UnsignedWordElement(309),
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1289, Priority.HIGH,
						this.m(VictronBattery.ChannelId.NUMBER_OF_CELLS_PER_BATTERY,
								new UnsignedWordElement(1289)),
						this.m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1290),
								SCALE_FACTOR_1),
						this.m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1291),
								SCALE_FACTOR_1)),
				new FC3ReadRegistersTask(1297, Priority.HIGH,
						this.m(VictronBattery.ChannelId.ALLOW_TO_CHARGE, new UnsignedWordElement(1297)),
						this.m(VictronBattery.ChannelId.ALLOW_TO_DISCHARGE, new UnsignedWordElement(1298))));
	}

	private ModbusProtocol defineCanBmsModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(259, Priority.HIGH,
						this.m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(259), SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.STARTER_BATTERY_VOLTAGE, new UnsignedWordElement(260),
								SCALE_FACTOR_MINUS_2),
						this.m(Battery.ChannelId.CURRENT, new SignedWordElement(261), SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.TEMPERATURE, new SignedWordElement(262), SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.MID_VOLTAGE, new UnsignedWordElement(263),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.MID_VOLTAGE_DEVIATION, new UnsignedWordElement(264),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.CONSUMED_AMPHOURS, new UnsignedWordElement(265),
								SCALE_FACTOR_MINUS_1_AND_INVERT),
						this.m(Battery.ChannelId.SOC, new UnsignedWordElement(266), SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.ALARM, new UnsignedWordElement(267)),
						this.m(VictronBattery.ChannelId.LOW_VOLTAGE_ALARM, new UnsignedWordElement(268)),
						this.m(VictronBattery.ChannelId.HIGH_VOLTAGE_ALARM, new UnsignedWordElement(269)),
						this.m(VictronBattery.ChannelId.LOW_STARTER_VOLTAGE_ALARM, new UnsignedWordElement(270)),
						this.m(VictronBattery.ChannelId.HIGH_STARTER_VOLTAGE_ALARM, new UnsignedWordElement(271)),
						this.m(VictronBattery.ChannelId.LOW_STATE_OF_CHARGE_ALARM, new UnsignedWordElement(272)),
						this.m(VictronBattery.ChannelId.LOW_TEMPERATURE_ALARM, new UnsignedWordElement(273)),
						this.m(VictronBattery.ChannelId.HIGH_TEMPERATURE_ALARM, new UnsignedWordElement(274)),
						this.m(VictronBattery.ChannelId.MID_VOLTAGE_ALARM, new UnsignedWordElement(275)),
						this.m(VictronBattery.ChannelId.LOW_FUSED_VOLTAGE_ALARM, new UnsignedWordElement(276)),
						this.m(VictronBattery.ChannelId.HIGH_FUSED_VOLTAGE_ALARM, new UnsignedWordElement(277)),
						this.m(VictronBattery.ChannelId.FUSE_BLOWN_ALARM, new UnsignedWordElement(278)),
						this.m(VictronBattery.ChannelId.HIGH_INTERNAL_TEMPERATURE_ALARM, new UnsignedWordElement(279)),
						this.m(VictronBattery.ChannelId.RELAY_STATUS, new UnsignedWordElement(280)),
						this.m(VictronBattery.ChannelId.DEEPEST_DISCHARGE, new UnsignedWordElement(281),
								SCALE_FACTOR_MINUS_1_AND_INVERT),
						this.m(VictronBattery.ChannelId.LAST_DISCHARGE, new UnsignedWordElement(282),
								SCALE_FACTOR_MINUS_1_AND_INVERT),
						this.m(VictronBattery.ChannelId.AVERAGE_DISCHARGE, new UnsignedWordElement(283),
								SCALE_FACTOR_MINUS_1_AND_INVERT),
						this.m(VictronBattery.ChannelId.CHARGE_CYCLES, new UnsignedWordElement(284)),
						this.m(VictronBattery.ChannelId.FULL_DISCHARGES, new UnsignedWordElement(285)),
						this.m(VictronBattery.ChannelId.TOTAL_AMPHOURS_DRAWN, new UnsignedWordElement(286),
								SCALE_FACTOR_MINUS_1_AND_INVERT),
						this.m(VictronBattery.ChannelId.HISTORY_MIN_VOLTAGE, new UnsignedWordElement(287),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.HISTORY_MAX_VOLTAGE, new UnsignedWordElement(288),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.TIME_SINCE_LAST_FULL_CHARGE, new UnsignedWordElement(289),
								SCALE_FACTOR_2),
						this.m(VictronBattery.ChannelId.AUTOMATIC_SYNCS, new UnsignedWordElement(290)),
						this.m(VictronBattery.ChannelId.LOW_VOLTAGE_ALARMS, new UnsignedWordElement(291)),
						this.m(VictronBattery.ChannelId.HIGH_VOLTAGE_ALARMS, new UnsignedWordElement(292)),
						this.m(VictronBattery.ChannelId.LOW_STARTER_VOLTAGE_ALARMS, new UnsignedWordElement(293)),
						this.m(VictronBattery.ChannelId.HIGH_STARTER_VOLTAGE_ALARMS, new UnsignedWordElement(294)),
						this.m(VictronBattery.ChannelId.MIN_STARTER_VOLTAGE, new UnsignedWordElement(295),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.MAX_STARTER_VOLTAGE, new UnsignedWordElement(296),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.LOW_FUSED_VOLTAGE_ALARMS, new UnsignedWordElement(297)),
						this.m(VictronBattery.ChannelId.HIGH_FUSED_VOLTAGE_ALARMS, new UnsignedWordElement(298)),
						this.m(VictronBattery.ChannelId.MIN_FUSED_VOLTAGE, new UnsignedWordElement(299),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.MAX_FUSED_VOLTAGE, new UnsignedWordElement(300),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.DC_DISCHARGED_ENERGY, new UnsignedWordElement(301),
								SCALE_FACTOR_2),
						this.m(VictronBattery.ChannelId.DC_CHARGED_ENERGY, new UnsignedWordElement(302),
								SCALE_FACTOR_2),
						this.m(VictronBattery.ChannelId.TIME_TO_GO, new UnsignedWordElement(303), SCALE_FACTOR_2),
						this.m(Battery.ChannelId.SOH, new UnsignedWordElement(304), SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(305),
								SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(306),
								SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(307),
								SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(308),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.CAPACITY_IN_AMPHOURS, new UnsignedWordElement(309),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.TIMESTAMP_1ST_LAST_ERROR, new SignedDoublewordElement(310)),
						this.m(VictronBattery.ChannelId.TIMESTAMP_2ND_LAST_ERROR, new SignedDoublewordElement(312)),
						this.m(VictronBattery.ChannelId.TIMESTAMP_3RD_LAST_ERROR, new SignedDoublewordElement(314)),
						this.m(VictronBattery.ChannelId.TIMESTAMP_4TH_LAST_ERROR, new SignedDoublewordElement(316)),
						this.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(318),
								SCALE_FACTOR_MINUS_1),
						this.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(319),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronBattery.ChannelId.HIGH_CHARGE_CURRENT_ALARM, new UnsignedWordElement(320)),
						this.m(VictronBattery.ChannelId.HIGH_DISCHARGE_CURRENT_ALARM, new UnsignedWordElement(321)),
						this.m(VictronBattery.ChannelId.CELL_IMBALANCE_ALARM, new UnsignedWordElement(322)),
						this.m(VictronBattery.ChannelId.INTERNAL_FAILURE_ALARM, new UnsignedWordElement(323)),
						this.m(VictronBattery.ChannelId.HIGH_CHARGE_TEMPERATURE_ALARM, new UnsignedWordElement(324)),
						this.m(VictronBattery.ChannelId.LOW_CHARGE_TEMPERATURE_ALARM, new UnsignedWordElement(325)),
						this.m(VictronBattery.ChannelId.LOW_CELL_VOLTAGE_ALARM, new UnsignedWordElement(326))),

				new FC3ReadRegistersTask(1282, Priority.HIGH,
						this.m(VictronBattery.ChannelId.VICTRON_STATE, new UnsignedWordElement(1282)),
						this.m(VictronBattery.ChannelId.ERROR, new UnsignedWordElement(1283)),
						this.m(VictronBattery.ChannelId.SYSTEM_SWITCH, new UnsignedWordElement(1284)),
						this.m(VictronBattery.ChannelId.BALANCING, new UnsignedWordElement(1285)),
						this.m(VictronBattery.ChannelId.NUMBER_OF_BATTERIES, new UnsignedWordElement(1286)),
						this.m(VictronBattery.ChannelId.BATTERIES_PARALLEL, new UnsignedWordElement(1287)),
						this.m(VictronBattery.ChannelId.BATTERIES_SERIES, new UnsignedWordElement(1288)),
						this.m(VictronBattery.ChannelId.NUMBER_OF_CELLS_PER_BATTERY, new UnsignedWordElement(1289)),
						this.m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1290), SCALE_FACTOR_1),
						this.m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1291), SCALE_FACTOR_1),
						this.m(VictronBattery.ChannelId.SHUTDOWNS_DUE_ERROR, new UnsignedWordElement(1292)),
						this.m(VictronBattery.ChannelId.DIAGNOSTICS_1ST_LAST_ERROR, new UnsignedWordElement(1293)),
						this.m(VictronBattery.ChannelId.DIAGNOSTICS_2ND_LAST_ERROR, new UnsignedWordElement(1294)),
						this.m(VictronBattery.ChannelId.DIAGNOSTICS_3RD_LAST_ERROR, new UnsignedWordElement(1295)),
						this.m(VictronBattery.ChannelId.DIAGNOSTICS_4TH_LAST_ERROR, new UnsignedWordElement(1296)),
						this.m(VictronBattery.ChannelId.ALLOW_TO_CHARGE, new UnsignedWordElement(1297)),
						this.m(VictronBattery.ChannelId.ALLOW_TO_DISCHARGE, new UnsignedWordElement(1298)),
						this.m(VictronBattery.ChannelId.EXTERNAL_RELAY, new UnsignedWordElement(1299)),
						this.m(VictronBattery.ChannelId.HISTORY_MIN_CELL_VOLTAGE, new UnsignedWordElement(1300),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronBattery.ChannelId.HISTORY_MAX_CELL_VOLTAGE, new UnsignedWordElement(1301),
								SCALE_FACTOR_MINUS_2))); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(VictronBattery.class, accessMode, 100) //
						.channel(0, VictronBattery.ChannelId.CHARGE_CYCLES, ModbusType.UINT16) //
						.channel(1, VictronBattery.ChannelId.DC_CHARGED_ENERGY, ModbusType.UINT16) //
						.channel(2, VictronBattery.ChannelId.DC_DISCHARGED_ENERGY, ModbusType.UINT16) //
						.channel(3, VictronBattery.ChannelId.INTERNAL_FAILURE_ALARM, ModbusType.UINT16) //
						.channel(4, VictronBattery.ChannelId.ERROR, ModbusType.UINT16) //
						.channel(5, VictronBattery.ChannelId.BALANCING, ModbusType.UINT16) //
						.channel(6, VictronBattery.ChannelId.ALARM, ModbusType.UINT16) //
						.channel(7, VictronBattery.ChannelId.CELL_IMBALANCE_ALARM, ModbusType.UINT16) //
						.channel(8, VictronBattery.ChannelId.CAPACITY_IN_AMPHOURS, ModbusType.UINT16) //
						.channel(9, VictronBattery.ChannelId.HIGH_CHARGE_CURRENT_ALARM, ModbusType.UINT16) //
						.channel(10, VictronBattery.ChannelId.HIGH_DISCHARGE_CURRENT_ALARM, ModbusType.UINT16) //
						.build());
	}
}
