package io.openems.edge.victron.pvinverterviadccharger;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PvInverter.via.DCCharger.Victron", //
		immediate = true, //
		configurationPolicy = REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		}) //
public class VictronPvInverterViaDcChargerImpl extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, ManagedSymmetricPvInverter, VictronPvInverterViaDcCharger, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public VictronPvInverterViaDcChargerImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				VictronPvInverterViaDcCharger.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(771, Priority.LOW,
						this.m(VictronPvInverterViaDcCharger.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(771),
								SCALE_FACTOR_MINUS_2),
						this.m(VictronPvInverterViaDcCharger.ChannelId.BATTERY_CURRENT, new SignedWordElement(772),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.BATTERY_TEMPERATURE, new SignedWordElement(773),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.CHARGER_ON_OFF, new UnsignedWordElement(774)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.CHARGE_STATE, new UnsignedWordElement(775)),
						this.m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedWordElement(776), SCALE_FACTOR_1)),
				new FC3ReadRegistersTask(778, Priority.LOW,
						this.m(VictronPvInverterViaDcCharger.ChannelId.EQUALIZATION_PENDING,
								new UnsignedWordElement(778)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.EQUALIZATION_TIME_REMAINING,
								new UnsignedWordElement(779), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.RELAY_ON_THE_CHARGER,
								new UnsignedWordElement(780)),
						new DummyRegisterElement(781),
						this.m(VictronPvInverterViaDcCharger.ChannelId.LOW_BATTERY_VOLTAGE_ALARM,
								new UnsignedWordElement(782)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.HIGH_BATTERY_VOLTAGE_ALARM,
								new UnsignedWordElement(783)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_TODAY, new UnsignedWordElement(784),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_TODAY,
								new UnsignedWordElement(785)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_YESTERDAY, new UnsignedWordElement(786),
								SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY,
								new UnsignedWordElement(787)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.ERROR_CODE, new UnsignedWordElement(788)),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(789),
								SCALE_FACTOR_MINUS_1),
						this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedWordElement(790),
								SCALE_FACTOR_2),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MPP_OPERATION_MODE,
								new UnsignedWordElement(791))),
				new FC3ReadRegistersTask(3700, Priority.LOW,
						this.m(VictronPvInverterViaDcCharger.ChannelId.PV_VOLTAGE_TRACKER_0,
								new UnsignedWordElement(3700), SCALE_FACTOR_MINUS_2),
						this.m(VictronPvInverterViaDcCharger.ChannelId.PV_VOLTAGE_TRACKER_1,
								new UnsignedWordElement(3701), SCALE_FACTOR_MINUS_2),
						this.m(VictronPvInverterViaDcCharger.ChannelId.PV_VOLTAGE_TRACKER_2,
								new UnsignedWordElement(3702), SCALE_FACTOR_MINUS_2),
						this.m(VictronPvInverterViaDcCharger.ChannelId.PV_VOLTAGE_TRACKER_3,
								new UnsignedWordElement(3703), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(3708, Priority.LOW,
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_TODAY_TRACKER_0,
								new UnsignedWordElement(3708), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_TODAY_TRACKER_1,
								new UnsignedWordElement(3709), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_TODAY_TRACKER_2,
								new UnsignedWordElement(3710), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_TODAY_TRACKER_3,
								new UnsignedWordElement(3711), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_YESTERDAY_TRACKER_0,
								new UnsignedWordElement(3712), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_YESTERDAY_TRACKER_1,
								new UnsignedWordElement(3713), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_YESTERDAY_TRACKER_2,
								new UnsignedWordElement(3714), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.YIELD_YESTERDAY_TRACKER_3,
								new UnsignedWordElement(3715), SCALE_FACTOR_MINUS_1),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_0,
								new UnsignedWordElement(3716)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_1,
								new UnsignedWordElement(3717)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_2,
								new UnsignedWordElement(3718)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_3,
								new UnsignedWordElement(3719)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_0,
								new UnsignedWordElement(3720)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_1,
								new UnsignedWordElement(3721)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_2,
								new UnsignedWordElement(3722)),
						this.m(VictronPvInverterViaDcCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_3,
								new UnsignedWordElement(3723))
				// ILLEGAL DATA ADDRESS
				// m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_0, new
				// UnsignedWordElement(3724)),
				// m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_1, new
				// UnsignedWordElement(3725)),
				// m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_2, new
				// UnsignedWordElement(3726)),
				// m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_3, new
				// UnsignedWordElement(3727))
				)); //

	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}
