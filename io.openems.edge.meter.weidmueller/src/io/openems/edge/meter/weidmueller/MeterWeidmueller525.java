package io.openems.edge.meter.weidmueller;

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
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Weidmueller.525", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterWeidmueller525 extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterWeidmueller525() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				WeidmuellerChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(19000)), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(19002)), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(19004)), //
						m(WeidmuellerChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(19006)), //
						m(WeidmuellerChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(19008)), //
						m(WeidmuellerChannelId.VOLTAGE_L1_L3, new FloatDoublewordElement(19010)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(19012)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(19014)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(19016)), //
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(19018)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(19020)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(19022)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(19024)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(19026)), //
						m(WeidmuellerChannelId.APPARENT_POWER_S1_L1N, new FloatDoublewordElement(19028)), //
						m(WeidmuellerChannelId.APPARENT_POWER_S2_L2N, new FloatDoublewordElement(19030)), //
						m(WeidmuellerChannelId.APPARENT_POWER_S3_L3N, new FloatDoublewordElement(19032)), //
						m(WeidmuellerChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(19034)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(19036)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(19038)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(19040)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(19042)), //
						m(WeidmuellerChannelId.COSPHI_L1, new FloatDoublewordElement(19044)), //
						m(WeidmuellerChannelId.COSPHI_L2, new FloatDoublewordElement(19046)), //
						m(WeidmuellerChannelId.COSPHI_L3, new FloatDoublewordElement(19048)), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(19050)), //
						m(WeidmuellerChannelId.ROTATION_FIELD, new FloatDoublewordElement(19052)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L1, new FloatDoublewordElement(19054)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L2, new FloatDoublewordElement(19056)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L3, new FloatDoublewordElement(19058)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L1_L3, new FloatDoublewordElement(19060)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L1_CONSUMED, new FloatDoublewordElement(19062)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L2_CONSUMED, new FloatDoublewordElement(19064)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L3_CONSUMED, new FloatDoublewordElement(19066)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19068)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L1_DELIVERED, new FloatDoublewordElement(19070)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L2_DELIVERED, new FloatDoublewordElement(19072)), //
						m(WeidmuellerChannelId.REAL_ENERGY_L3_DELIVERED, new FloatDoublewordElement(19074)), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19076)), //
						m(WeidmuellerChannelId.APPARENT_ENERGY_L1, new FloatDoublewordElement(19078)), //
						m(WeidmuellerChannelId.APPARENT_ENERGY_L2, new FloatDoublewordElement(19080)), //
						m(WeidmuellerChannelId.APPARENT_ENERGY_L3, new FloatDoublewordElement(19082)), //
						m(WeidmuellerChannelId.APPARENT_ENERGY_L1_L3, new FloatDoublewordElement(19084)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_L1, new FloatDoublewordElement(19086)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_L2, new FloatDoublewordElement(19088)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_L3, new FloatDoublewordElement(19090)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_L1_L3, new FloatDoublewordElement(19092)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_INDUCTIVE_L1, new FloatDoublewordElement(19094)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_INDUCTIVE_L2, new FloatDoublewordElement(19096)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_INDUCTIVE_L3, new FloatDoublewordElement(19098)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_INDUCTIVE_L1_L3, new FloatDoublewordElement(19100)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_CAPACITIVE_L1, new FloatDoublewordElement(19102)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_CAPACITIVE_L2, new FloatDoublewordElement(19104)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_CAPACITIVE_L3, new FloatDoublewordElement(19106)), //
						m(WeidmuellerChannelId.REACTIVE_ENERGY_CAPACITIVE_L1_L3, new FloatDoublewordElement(19108)), //
						m(WeidmuellerChannelId.HARMONIC_THD_VOLT_L1N, new FloatDoublewordElement(19110)), //
						m(WeidmuellerChannelId.HARMONIC_THD_VOLT_L2N, new FloatDoublewordElement(19112)), //
						m(WeidmuellerChannelId.HARMONIC_THD_VOLT_L3N, new FloatDoublewordElement(19114)), //
						m(WeidmuellerChannelId.HARMONIC_THD_CURRENT_L1N, new FloatDoublewordElement(19116)), //
						m(WeidmuellerChannelId.HARMONIC_THD_CURRENT_L2N, new FloatDoublewordElement(19118)), //
						m(WeidmuellerChannelId.HARMONIC_THD_CURRENT_L3N, new FloatDoublewordElement(19120))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}

}
