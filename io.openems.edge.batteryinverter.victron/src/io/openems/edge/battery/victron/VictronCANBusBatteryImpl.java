package io.openems.edge.battery.victron;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class VictronCANBusBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, VictronBattery, ModbusComponent, OpenemsComponent {

	public static final int DEFAULT_UNIT_ID = 225;
	public static final int BATTERY_VOLTAGE = 48;

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public VictronCANBusBatteryImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				VictronBattery.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.installListener();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public String debugLog() {
		return this.getSoc().asString();
	}

	private void installListener() {
		this.getCapacityInAmphoursChannel().onUpdate((value) -> {
			if (!value.isDefined()) {
				return;
			}
			Integer capacityInWatthours = value.get() * BATTERY_VOLTAGE;
			this._setCapacity(capacityInWatthours);
		});
	}
	
	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO implement battery start/stop if needed
		this._setStartStop(value);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(259, Priority.HIGH,
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(259),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.STARTER_BATTERY_VOLTAGE, new UnsignedWordElement(260),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(Battery.ChannelId.CURRENT, new SignedWordElement(261),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.TEMPERATURE, new SignedWordElement(262),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.MID_VOLTAGE, new UnsignedWordElement(263),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.MID_VOLTAGE_DEVIATION, new UnsignedWordElement(264),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.CONSUMED_AMPHOURS, new UnsignedWordElement(265),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(266),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.ALARM, new UnsignedWordElement(267)),
						m(VictronBattery.ChannelId.LOW_VOLTAGE_ALARM, new UnsignedWordElement(268)),
						m(VictronBattery.ChannelId.HIGH_VOLTAGE_ALARM, new UnsignedWordElement(269)),
						m(VictronBattery.ChannelId.LOW_STARTER_VOLTAGE_ALARM, new UnsignedWordElement(270)),
						m(VictronBattery.ChannelId.HIGH_STARTER_VOLTAGE_ALARM, new UnsignedWordElement(271)),
						m(VictronBattery.ChannelId.LOW_STATE_OF_CHARGE_ALARM, new UnsignedWordElement(272)),
						m(VictronBattery.ChannelId.LOW_TEMPERATURE_ALARM, new UnsignedWordElement(273)),
						m(VictronBattery.ChannelId.HIGH_TEMPERATURE_ALARM, new UnsignedWordElement(274)),
						m(VictronBattery.ChannelId.MID_VOLTAGE_ALARM, new UnsignedWordElement(275)),
						m(VictronBattery.ChannelId.LOW_FUSED_VOLTAGE_ALARM, new UnsignedWordElement(276)),
						m(VictronBattery.ChannelId.HIGH_FUSED_VOLTAGE_ALARM, new UnsignedWordElement(277)),
						m(VictronBattery.ChannelId.FUSE_BLOWN_ALARM, new UnsignedWordElement(278)),
						m(VictronBattery.ChannelId.HIGH_INTERNAL_TEMPERATURE_ALARM, new UnsignedWordElement(279)),
						m(VictronBattery.ChannelId.RELAY_STATUS, new UnsignedWordElement(280)),
						m(VictronBattery.ChannelId.DEEPEST_DISCHARGE, new UnsignedWordElement(281),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(VictronBattery.ChannelId.LAST_DISCHARGE, new UnsignedWordElement(282),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(VictronBattery.ChannelId.AVERAGE_DISCHARGE, new UnsignedWordElement(283),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(VictronBattery.ChannelId.CHARGE_CYCLES, new UnsignedWordElement(284)),
						m(VictronBattery.ChannelId.FULL_DISCHARGES, new UnsignedWordElement(285)),
						m(VictronBattery.ChannelId.TOTAL_AMPHOURS_DRAWN, new UnsignedWordElement(286),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(VictronBattery.ChannelId.HISTORY_MIN_VOLTAGE, new UnsignedWordElement(287),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.HISTORY_MAX_VOLTAGE, new UnsignedWordElement(288),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.TIME_SINCE_LAST_FULL_CHARGE, new UnsignedWordElement(289),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(VictronBattery.ChannelId.AUTOMATIC_SYNCS, new UnsignedWordElement(290)),
						m(VictronBattery.ChannelId.LOW_VOLTAGE_ALARMS, new UnsignedWordElement(291)),
						m(VictronBattery.ChannelId.HIGH_VOLTAGE_ALARMS, new UnsignedWordElement(292)),
						m(VictronBattery.ChannelId.LOW_STARTER_VOLTAGE_ALARMS, new UnsignedWordElement(293)),
						m(VictronBattery.ChannelId.HIGH_STARTER_VOLTAGE_ALARMS, new UnsignedWordElement(294)),
						m(VictronBattery.ChannelId.MIN_STARTER_VOLTAGE, new UnsignedWordElement(295),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.MAX_STARTER_VOLTAGE, new UnsignedWordElement(296),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.LOW_FUSED_VOLTAGE_ALARMS, new UnsignedWordElement(297)),
						m(VictronBattery.ChannelId.HIGH_FUSED_VOLTAGE_ALARMS, new UnsignedWordElement(298)),
						m(VictronBattery.ChannelId.MIN_FUSED_VOLTAGE, new UnsignedWordElement(299),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.MAX_FUSED_VOLTAGE, new UnsignedWordElement(300),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.DISCHARGED_ENERGY, new UnsignedWordElement(301),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(302),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.TIME_TO_GO, new UnsignedWordElement(303),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(Battery.ChannelId.SOH, new UnsignedWordElement(304),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(305),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(306),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(307),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(308),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.CAPACITY_IN_AMPHOURS, new UnsignedWordElement(309),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.TIMESTAMP_1ST_LAST_ERROR, new SignedDoublewordElement(310)),
						m(VictronBattery.ChannelId.TIMESTAMP_2ND_LAST_ERROR, new SignedDoublewordElement(312)),
						m(VictronBattery.ChannelId.TIMESTAMP_3RD_LAST_ERROR, new SignedDoublewordElement(314)),
						m(VictronBattery.ChannelId.TIMESTAMP_4TH_LAST_ERROR, new SignedDoublewordElement(316)),
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(318),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(319),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBattery.ChannelId.HIGH_CHARGE_CURRENT_ALARM, new UnsignedWordElement(320)),
						m(VictronBattery.ChannelId.HIGH_DISCHARGE_CURRENT_ALARM, new UnsignedWordElement(321)),
						m(VictronBattery.ChannelId.CELL_IMBALANCE_ALARM, new UnsignedWordElement(322)),
						m(VictronBattery.ChannelId.INTERNAL_FAILURE_ALARM, new UnsignedWordElement(323)),
						m(VictronBattery.ChannelId.HIGH_CHARGE_TEMPERATURE_ALARM, new UnsignedWordElement(324)),
						m(VictronBattery.ChannelId.LOW_CHARGE_TEMPERATURE_ALARM, new UnsignedWordElement(325)),
						m(VictronBattery.ChannelId.LOW_CELL_VOLTAGE_ALARM, new UnsignedWordElement(326))),
				new FC3ReadRegistersTask(1282, Priority.HIGH,
						m(VictronBattery.ChannelId.VICTRON_STATE, new UnsignedWordElement(1282)),
						m(VictronBattery.ChannelId.ERROR, new UnsignedWordElement(1283)),
						m(VictronBattery.ChannelId.SYSTEM_SWITCH, new UnsignedWordElement(1284)),
						m(VictronBattery.ChannelId.BALANCING, new UnsignedWordElement(1285)),
						m(VictronBattery.ChannelId.NUMBER_OF_BATTERIES, new UnsignedWordElement(1286)),
						m(VictronBattery.ChannelId.BATTERIES_PARALLEL, new UnsignedWordElement(1287)),
						m(VictronBattery.ChannelId.BATTERIES_SERIES, new UnsignedWordElement(1288)),
						m(VictronBattery.ChannelId.NUMBER_OF_CELLS_PER_BATTERY, new UnsignedWordElement(1289)),
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1290),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1291),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.SHUTDOWNS_DUE_ERROR, new UnsignedWordElement(1292)),
						m(VictronBattery.ChannelId.DIAGNOSTICS_1ST_LAST_ERROR, new UnsignedWordElement(1293)),
						m(VictronBattery.ChannelId.DIAGNOSTICS_2ND_LAST_ERROR, new UnsignedWordElement(1294)),
						m(VictronBattery.ChannelId.DIAGNOSTICS_3RD_LAST_ERROR, new UnsignedWordElement(1295)),
						m(VictronBattery.ChannelId.DIAGNOSTICS_4TH_LAST_ERROR, new UnsignedWordElement(1296)),
						m(VictronBattery.ChannelId.ALLOW_TO_CHARGE, new UnsignedWordElement(1297)),
						m(VictronBattery.ChannelId.ALLOW_TO_DISCHARGE, new UnsignedWordElement(1298)),
						m(VictronBattery.ChannelId.EXTERNAL_RELAY, new UnsignedWordElement(1299)),
						m(VictronBattery.ChannelId.HISTORY_MIN_CELL_VOLTAGE, new UnsignedWordElement(1300),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronBattery.ChannelId.HISTORY_MAX_CELL_VOLTAGE, new UnsignedWordElement(1301),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2))); //

	}



}
