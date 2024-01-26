package io.openems.edge.meter.entes.mpr15s22;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Entes.Mpr15S22", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Mpr15S22Impl extends AbstractOpenemsModbusComponent
		implements Mpr15S22, ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public Mpr15S22Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Mpr15S22.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
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
		return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(Mpr15S22.ChannelId.VOLTAGE_L1_N, new UnsignedDoublewordElement(0)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L2_N, new UnsignedDoublewordElement(2)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L3_N, new UnsignedDoublewordElement(4)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L4_N, new UnsignedDoublewordElement(6)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L1_L2, new UnsignedDoublewordElement(8)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L2_L3, new UnsignedDoublewordElement(10)), //
						m(Mpr15S22.ChannelId.VOLTAGE_L3_L1, new UnsignedDoublewordElement(12)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(14)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(16)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(18)), //
						m(Mpr15S22.ChannelId.CURRENT_L4, new UnsignedDoublewordElement(20)), //
						m(Mpr15S22.ChannelId.NEUTRAL_CURRENT, new UnsignedDoublewordElement(22)), //
						m(Mpr15S22.ChannelId.MEASURED_FREQUENCY, new UnsignedDoublewordElement(24)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L1_N, new FloatDoublewordElement(26)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L2_N, new FloatDoublewordElement(28)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L3_N, new FloatDoublewordElement(30)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L4_N, new FloatDoublewordElement(32)), //
						m(Mpr15S22.ChannelId.TOTAL_IMPORT_ACTIVE_POWER, new FloatDoublewordElement(34)), //
						m(Mpr15S22.ChannelId.TOTAL_EXPORT_ACTIVE_POWER, new FloatDoublewordElement(36)), //
						m(Mpr15S22.ChannelId.SUM_ACTIVE_POWER, new FloatDoublewordElement(38)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(40)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(42)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(44)), //
						m(Mpr15S22.ChannelId.REACTIVE_POWER_L4, new FloatDoublewordElement(46)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(48)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(50)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(52)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(54)), //
						m(Mpr15S22.ChannelId.SUM_REACTIVE_POWER, new FloatDoublewordElement(56)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L1_N, new FloatDoublewordElement(58)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L2_N, new FloatDoublewordElement(60)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L3_N, new FloatDoublewordElement(62)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L4_N, new FloatDoublewordElement(64)), //
						m(Mpr15S22.ChannelId.TOTAL_IMPORT_APPARENT_POWER, new FloatDoublewordElement(66)), //
						m(Mpr15S22.ChannelId.TOTAL_EXPORT_APPARENT_POWER, new FloatDoublewordElement(68)), //
						m(Mpr15S22.ChannelId.SUM_APPARENT_POWER, new FloatDoublewordElement(70)), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L1, new SignedDoublewordElement(72)), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L2, new SignedDoublewordElement(74)), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L3, new SignedDoublewordElement(76)), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L4, new SignedDoublewordElement(78)), //
						m(Mpr15S22.ChannelId.SUM_POWER_FACTOR, new SignedDoublewordElement(80)), //
						m(Mpr15S22.ChannelId.COSPHI_L1, new SignedDoublewordElement(82)), //
						m(Mpr15S22.ChannelId.COSPHI_L2, new SignedDoublewordElement(84)), //
						m(Mpr15S22.ChannelId.COSPHI_L3, new SignedDoublewordElement(86)), //
						m(Mpr15S22.ChannelId.COSPHI_L4, new SignedDoublewordElement(88)), //
						m(Mpr15S22.ChannelId.SUM_COS_PHI, new SignedDoublewordElement(90)), //
						m(Mpr15S22.ChannelId.ROTATION_FIELD, new SignedDoublewordElement(92)), //
						m(Mpr15S22.ChannelId.VOLTAGE_UNBALANCE, new UnsignedDoublewordElement(94)), //
						m(Mpr15S22.ChannelId.CURRENT_UNBALANCE, new UnsignedDoublewordElement(96)), //
						m(Mpr15S22.ChannelId.L1_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(98)), //
						m(Mpr15S22.ChannelId.L2_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(100)), //
						m(Mpr15S22.ChannelId.L3_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(102)), //
						m(Mpr15S22.ChannelId.L4_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(104)), //
						m(Mpr15S22.ChannelId.L1_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(106)), //
						m(Mpr15S22.ChannelId.L2_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(108)), //
						m(Mpr15S22.ChannelId.L3_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(110)), //
						m(Mpr15S22.ChannelId.L4_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(112)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_1, new FloatDoublewordElement(114)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_2, new FloatDoublewordElement(116)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_3, new FloatDoublewordElement(118)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_4, new FloatDoublewordElement(120)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_5, new FloatDoublewordElement(122)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_6, new FloatDoublewordElement(124)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_7, new FloatDoublewordElement(126)), //
						m(Mpr15S22.ChannelId.ANALOG_INPUT_8, new FloatDoublewordElement(128)), //
						m(Mpr15S22.ChannelId.ANALOG_OUTPUT_1, new FloatDoublewordElement(130)), //
						m(Mpr15S22.ChannelId.ANALOG_OUTPUT_2, new FloatDoublewordElement(132)), //
						m(Mpr15S22.ChannelId.ANALOG_OUTPUT_3, new FloatDoublewordElement(134)), //
						m(Mpr15S22.ChannelId.ANALOG_OUTPUT_4, new FloatDoublewordElement(136)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_1, new FloatDoublewordElement(138)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_2, new FloatDoublewordElement(140)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_3, new FloatDoublewordElement(142)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_4, new FloatDoublewordElement(144)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_5, new FloatDoublewordElement(146)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_6, new FloatDoublewordElement(148)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_7, new FloatDoublewordElement(150)), //
						m(Mpr15S22.ChannelId.TEMPERATURE_INPUT_8, new FloatDoublewordElement(152)), //
						m(Mpr15S22.ChannelId.HOUR_METER_NON_RESETABLE, new UnsignedDoublewordElement(154)), //
						m(Mpr15S22.ChannelId.WORKING_HOUR_COUNTER, new UnsignedDoublewordElement(156)), //
						m(Mpr15S22.ChannelId.INPUT_STATUS, new UnsignedDoublewordElement(158)), //
						m(Mpr15S22.ChannelId.OUTPUT_STATUS, new UnsignedDoublewordElement(160)),
								new DummyRegisterElement(162, 199), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(200)), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(204)), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(208)), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(212)), //
						m(Mpr15S22.ChannelId.TOTAL_CONSUMED_ENERGY_L1_L3, new UnsignedQuadruplewordElement(216)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(220)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(224)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(228)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(232)), //
						m(Mpr15S22.ChannelId.TOTAL_DELIVERED_ENERGY_L1_L3, new UnsignedQuadruplewordElement(236)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L1, new UnsignedQuadruplewordElement(240)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L2, new UnsignedQuadruplewordElement(244)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L3, new UnsignedQuadruplewordElement(248)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L4, new UnsignedQuadruplewordElement(252)), //
						m(Mpr15S22.ChannelId.TOTAL_CONSUMED_APPARENT_ENERGY_L1_L3,
								new UnsignedQuadruplewordElement(256)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L1, new UnsignedQuadruplewordElement(260)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L2, new UnsignedQuadruplewordElement(264)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L3, new UnsignedQuadruplewordElement(268)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L4, new UnsignedQuadruplewordElement(272)), //
						m(Mpr15S22.ChannelId.TOTAL_DELIVERED_APPARENT_ENERGY_L1_L3,
								new UnsignedQuadruplewordElement(276)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(280)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(284)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(288)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(292)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(296)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(300)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(304)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(308)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(312)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(316)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(320)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(324)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(328)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(332)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(336)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(340)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(344)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(348)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L4, new UnsignedQuadruplewordElement(352)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(356)), //
						m(Mpr15S22.ChannelId.NUMBER_OF_PULSE_METER, new UnsignedDoublewordElement(360)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_1, new UnsignedDoublewordElement(362)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_2, new UnsignedDoublewordElement(364)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_3, new UnsignedDoublewordElement(366)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_4, new UnsignedDoublewordElement(368)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_5, new UnsignedDoublewordElement(370)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_6, new UnsignedDoublewordElement(372)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_7, new UnsignedDoublewordElement(374)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_8, new UnsignedDoublewordElement(376))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
