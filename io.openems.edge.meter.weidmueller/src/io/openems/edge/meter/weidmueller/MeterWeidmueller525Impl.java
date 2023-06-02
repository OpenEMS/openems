package io.openems.edge.meter.weidmueller;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Weidmueller.525", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterWeidmueller525Impl extends AbstractOpenemsModbusComponent implements MeterWeidmueller525,
		AsymmetricMeter, SymmetricMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;

	public MeterWeidmueller525Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterWeidmueller525.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();

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
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(19000, Priority.HIGH, //
						m(new FloatDoublewordElement(19000)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, DIRECT_1_TO_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(19002)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(19004)), //
						m(MeterWeidmueller525.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(19006)), //
						m(MeterWeidmueller525.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(19008)), //
						m(MeterWeidmueller525.ChannelId.VOLTAGE_L1_L3, new FloatDoublewordElement(19010)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(19012)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(19014)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(19016)), //
						m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(19018)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(19020)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(19022)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(19024)), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(19026)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_POWER_S1_L1N, new FloatDoublewordElement(19028)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_POWER_S2_L2N, new FloatDoublewordElement(19030)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_POWER_S3_L3N, new FloatDoublewordElement(19032)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(19034)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(19036)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(19038)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(19040)), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(19042)), //
						m(MeterWeidmueller525.ChannelId.COSPHI_L1, new FloatDoublewordElement(19044)), //
						m(MeterWeidmueller525.ChannelId.COSPHI_L2, new FloatDoublewordElement(19046)), //
						m(MeterWeidmueller525.ChannelId.COSPHI_L3, new FloatDoublewordElement(19048)), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(19050)), //
						m(MeterWeidmueller525.ChannelId.ROTATION_FIELD, new FloatDoublewordElement(19052)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L1, new FloatDoublewordElement(19054)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L2, new FloatDoublewordElement(19056)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L3, new FloatDoublewordElement(19058)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L1_L3, new FloatDoublewordElement(19060)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L1_CONSUMED, new FloatDoublewordElement(19062)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L2_CONSUMED, new FloatDoublewordElement(19064)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L3_CONSUMED, new FloatDoublewordElement(19066)), //
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19068)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L1_DELIVERED, new FloatDoublewordElement(19070)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L2_DELIVERED, new FloatDoublewordElement(19072)), //
						m(MeterWeidmueller525.ChannelId.REAL_ENERGY_L3_DELIVERED, new FloatDoublewordElement(19074)), //
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19076)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_ENERGY_L1, new FloatDoublewordElement(19078)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_ENERGY_L2, new FloatDoublewordElement(19080)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_ENERGY_L3, new FloatDoublewordElement(19082)), //
						m(MeterWeidmueller525.ChannelId.APPARENT_ENERGY_L1_L3, new FloatDoublewordElement(19084)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_L1, new FloatDoublewordElement(19086)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_L2, new FloatDoublewordElement(19088)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_L3, new FloatDoublewordElement(19090)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_L1_L3, new FloatDoublewordElement(19092)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L1,
								new FloatDoublewordElement(19094)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L2,
								new FloatDoublewordElement(19096)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L3,
								new FloatDoublewordElement(19098)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L1_L3,
								new FloatDoublewordElement(19100)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L1,
								new FloatDoublewordElement(19102)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L2,
								new FloatDoublewordElement(19104)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L3,
								new FloatDoublewordElement(19106)), //
						m(MeterWeidmueller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L1_L3,
								new FloatDoublewordElement(19108)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_VOLT_L1N, new FloatDoublewordElement(19110)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_VOLT_L2N, new FloatDoublewordElement(19112)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_VOLT_L3N, new FloatDoublewordElement(19114)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_CURRENT_L1N, new FloatDoublewordElement(19116)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_CURRENT_L2N, new FloatDoublewordElement(19118)), //
						m(MeterWeidmueller525.ChannelId.HARMONIC_THD_CURRENT_L3N, new FloatDoublewordElement(19120))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}

}
