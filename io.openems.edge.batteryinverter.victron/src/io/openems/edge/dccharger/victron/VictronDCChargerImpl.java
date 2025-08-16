package io.openems.edge.dccharger.victron;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "DCCharger.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		}) //
public class VictronDCChargerImpl extends AbstractOpenemsModbusComponent
		implements EssDcCharger, VictronDCCharger, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public VictronDCChargerImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				VictronDCCharger.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config)
			throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
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
	public String debugLog() {
		return this.getActualPower().asString();
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(771, Priority.LOW,
						m(VictronDCCharger.ChannelId.BATTERY_VOLTAGE,
								new UnsignedWordElement(771),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronDCCharger.ChannelId.BATTERY_CURRENT,
								new SignedWordElement(772),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.BATTERY_TEMPERATURE,
								new SignedWordElement(773),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.CHARGER_ON_OFF,
								new UnsignedWordElement(774)),
						m(VictronDCCharger.ChannelId.CHARGE_STATE,
								new UnsignedWordElement(775)),
						m(VictronDCCharger.ChannelId.VOLTAGE,
								new UnsignedWordElement(776),
								ElementToChannelConverter.SCALE_FACTOR_1)),
				new FC3ReadRegistersTask(778, Priority.LOW,
						m(VictronDCCharger.ChannelId.EQUALIZATION_PENDING,
								new UnsignedWordElement(778)),
						m(VictronDCCharger.ChannelId.EQUALIZATION_TIME_REMAINING,
								new UnsignedWordElement(779),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.RELAY_ON_THE_CHARGER,
								new UnsignedWordElement(780)),
						new DummyRegisterElement(781),
						m(VictronDCCharger.ChannelId.LOW_BATTERY_VOLTAGE_ALARM,
								new UnsignedWordElement(782)),
						m(VictronDCCharger.ChannelId.HIGH_BATTERY_VOLTAGE_ALARM,
								new UnsignedWordElement(783)),
						m(VictronDCCharger.ChannelId.YIELD_TODAY,
								new UnsignedWordElement(784),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_TODAY,
								new UnsignedWordElement(785)),
						m(VictronDCCharger.ChannelId.YIELD_YESTERDAY,
								new UnsignedWordElement(786),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY,
								new UnsignedWordElement(787)),
						m(VictronDCCharger.ChannelId.ERROR_CODE,
								new UnsignedWordElement(788)),
						m(EssDcCharger.ChannelId.ACTUAL_POWER,
								new UnsignedWordElement(789),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssDcCharger.ChannelId.ACTUAL_ENERGY,
								new UnsignedWordElement(790),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(VictronDCCharger.ChannelId.MPP_OPERATION_MODE,
								new UnsignedWordElement(791))),
				new FC3ReadRegistersTask(3700, Priority.LOW,
						m(VictronDCCharger.ChannelId.PV_VOLTAGE_TRACKER_0,
								new UnsignedWordElement(3700),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronDCCharger.ChannelId.PV_VOLTAGE_TRACKER_1,
								new UnsignedWordElement(3701),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronDCCharger.ChannelId.PV_VOLTAGE_TRACKER_2,
								new UnsignedWordElement(3702),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronDCCharger.ChannelId.PV_VOLTAGE_TRACKER_3,
								new UnsignedWordElement(3703),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(3708, Priority.LOW,
						m(VictronDCCharger.ChannelId.YIELD_TODAY_TRACKER_0,
								new UnsignedWordElement(3708),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_TODAY_TRACKER_1,
								new UnsignedWordElement(3709),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_TODAY_TRACKER_2,
								new UnsignedWordElement(3710),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_TODAY_TRACKER_3,
								new UnsignedWordElement(3711),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_YESTERDAY_TRACKER_0,
								new UnsignedWordElement(3712),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_YESTERDAY_TRACKER_1,
								new UnsignedWordElement(3713),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_YESTERDAY_TRACKER_2,
								new UnsignedWordElement(3714),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.YIELD_YESTERDAY_TRACKER_3,
								new UnsignedWordElement(3715),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_0,
								new UnsignedWordElement(3716)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_1,
								new UnsignedWordElement(3717)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_2,
								new UnsignedWordElement(3718)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_TODAY_TRACKER_3,
								new UnsignedWordElement(3719)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_0,
								new UnsignedWordElement(3720)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_1,
								new UnsignedWordElement(3721)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_2,
								new UnsignedWordElement(3722)),
						m(VictronDCCharger.ChannelId.MAX_CHARGE_POWER_YESTERDAY_TRACKER_3,
								new UnsignedWordElement(3723))
				// ILLEGAL DATA ADDRESS
//						m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_0, new UnsignedWordElement(3724)),
//						m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_1, new UnsignedWordElement(3725)),
//						m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_2, new UnsignedWordElement(3726)),
//						m(VictronSolarcharger.ChannelId.PV_POWER_TRACKER_3, new UnsignedWordElement(3727))						
				)); //

	}

}
