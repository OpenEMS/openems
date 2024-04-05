
package io.openems.edge.meter.bgetech;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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
@Component(//
		name = "Meter.BGE-TECH.DRT428M2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterBgeTechDrt428M2Impl extends AbstractOpenemsModbusComponent
		implements MeterBgeTechDrt428M2, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;

	public MeterBgeTechDrt428M2Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterBgeTechDrt428M2.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
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
		ModbusProtocol modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(14, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(14), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(16), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(18), SCALE_FACTOR_3), //

						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(20), SCALE_FACTOR_3), //

						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(22), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(24), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(26), SCALE_FACTOR_3), //

						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(28), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(30), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(32), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(34), SCALE_FACTOR_3), //

						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(36), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(38), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(40), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(42), SCALE_FACTOR_3), //

						m(MeterBgeTechDrt428M2.ChannelId.TOTAL_APPARENT_POWER, new FloatDoublewordElement(44)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_APPARENT_POWER, new FloatDoublewordElement(46)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_APPARENT_POWER, new FloatDoublewordElement(48)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_APPARENT_POWER, new FloatDoublewordElement(50)),

						m(MeterBgeTechDrt428M2.ChannelId.TOTAL_POWER_FACTOR, new FloatDoublewordElement(52)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_POWER_FACTOR, new FloatDoublewordElement(54)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_POWER_FACTOR, new FloatDoublewordElement(56)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_POWER_FACTOR, new FloatDoublewordElement(58))),

				new FC3ReadRegistersTask(256, Priority.LOW, //
						m(MeterBgeTechDrt428M2.ChannelId.TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(256)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(258)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(260)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(262)),

						m(MeterBgeTechDrt428M2.ChannelId.FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(264)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(266)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(268)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(270)),

						m(MeterBgeTechDrt428M2.ChannelId.REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(272)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(274)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(276)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(278)),

						m(MeterBgeTechDrt428M2.ChannelId.TOTAL_REACTIVE_ENERGY, new FloatDoublewordElement(280)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_REACTIVE_ENERGY, new FloatDoublewordElement(282)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_REACTIVE_ENERGY, new FloatDoublewordElement(284)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_REACTIVE_ENERGY, new FloatDoublewordElement(286)),

						m(MeterBgeTechDrt428M2.ChannelId.FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(288)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(290)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(292)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(294)),

						m(MeterBgeTechDrt428M2.ChannelId.REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(296)),
						m(MeterBgeTechDrt428M2.ChannelId.L1_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(298)),
						m(MeterBgeTechDrt428M2.ChannelId.L2_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(300)),
						m(MeterBgeTechDrt428M2.ChannelId.L3_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(302)),

						m(MeterBgeTechDrt428M2.ChannelId.T1_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(304)),
						m(MeterBgeTechDrt428M2.ChannelId.T1_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(306)),
						m(MeterBgeTechDrt428M2.ChannelId.T1_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(308)),
						m(MeterBgeTechDrt428M2.ChannelId.T1_TOTAL_REACTIVE_ENERGY, new FloatDoublewordElement(310)),
						m(MeterBgeTechDrt428M2.ChannelId.T1_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(312)),
						m(MeterBgeTechDrt428M2.ChannelId.T1_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(314)),

						m(MeterBgeTechDrt428M2.ChannelId.T2_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(316)),
						m(MeterBgeTechDrt428M2.ChannelId.T2_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(318)),
						m(MeterBgeTechDrt428M2.ChannelId.T2_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(320)),
						m(MeterBgeTechDrt428M2.ChannelId.T2_TOTAL_REACTIVE_ENERGY, new FloatDoublewordElement(322)),
						m(MeterBgeTechDrt428M2.ChannelId.T2_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(324)),
						m(MeterBgeTechDrt428M2.ChannelId.T2_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(326)),

						m(MeterBgeTechDrt428M2.ChannelId.T3_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(328)),
						m(MeterBgeTechDrt428M2.ChannelId.T3_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(330)),
						m(MeterBgeTechDrt428M2.ChannelId.T3_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(332)),
						m(MeterBgeTechDrt428M2.ChannelId.T3_TOTAL_REACTIVE_ENERGY, new FloatDoublewordElement(334)),
						m(MeterBgeTechDrt428M2.ChannelId.T3_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(336)),
						m(MeterBgeTechDrt428M2.ChannelId.T3_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(338)),

						m(MeterBgeTechDrt428M2.ChannelId.T4_TOTAL_ACTIVE_ENERGY, new FloatDoublewordElement(340)),
						m(MeterBgeTechDrt428M2.ChannelId.T4_FORWARD_ACTIVE_ENERGY, new FloatDoublewordElement(342)),
						m(MeterBgeTechDrt428M2.ChannelId.T4_REVERSE_ACTIVE_ENERGY, new FloatDoublewordElement(344)),
						m(MeterBgeTechDrt428M2.ChannelId.T4_TOTAL_REACTIVE_ENERGY, new FloatDoublewordElement(346)),
						m(MeterBgeTechDrt428M2.ChannelId.T4_FORWARD_REACTIVE_ENERGY, new FloatDoublewordElement(348)),
						m(MeterBgeTechDrt428M2.ChannelId.T4_REVERSE_REACTIVE_ENERGY, new FloatDoublewordElement(350))));

		return modbusProtocol;
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
