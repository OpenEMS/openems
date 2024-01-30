package io.openems.edge.meter.entes.mpr15s22;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

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
				// refere to https://www.entes.eu/uploads/files/MPR-1X_Register_Table_EN.pdf
				// Measurement registers
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(Mpr15S22.ChannelId.VOLTAGE_L1_N, new UnsignedDoublewordElement(0), SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.VOLTAGE_L2_N, new UnsignedDoublewordElement(2), SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.VOLTAGE_L3_N, new UnsignedDoublewordElement(4), SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(8, Priority.HIGH,
						m(Mpr15S22.ChannelId.VOLTAGE_L1_L2, new UnsignedDoublewordElement(8), SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.VOLTAGE_L2_L3, new UnsignedDoublewordElement(10), SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.VOLTAGE_L3_L1, new UnsignedDoublewordElement(12), SCALE_FACTOR_MINUS_1), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(14),
								SCALE_FACTOR_MINUS_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(16),
								SCALE_FACTOR_MINUS_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(18),
								SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(22, Priority.HIGH,
						m(Mpr15S22.ChannelId.NEUTRAL_CURRENT, new UnsignedDoublewordElement(22), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.MEASURED_FREQUENCY, new UnsignedDoublewordElement(24),
								SCALE_FACTOR_MINUS_2), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L1_N, new FloatDoublewordElement(26)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L2_N, new FloatDoublewordElement(28)), //
						m(Mpr15S22.ChannelId.ACTIVE_POWER_L3_N, new FloatDoublewordElement(30))), //
				new FC3ReadRegistersTask(34, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_IMPORT_ACTIVE_POWER, new FloatDoublewordElement(34)), //
						m(Mpr15S22.ChannelId.TOTAL_EXPORT_ACTIVE_POWER, new FloatDoublewordElement(36)), //
						m(Mpr15S22.ChannelId.SUM_ACTIVE_POWER, new FloatDoublewordElement(38)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(40)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(42)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(44))), //
				new FC3ReadRegistersTask(48, Priority.HIGH,
						m(Mpr15S22.ChannelId.QUADRANT_1_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(48)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(50)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(52)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_TOTAL_REACTIVE_POWER, new FloatDoublewordElement(54)), //
						m(Mpr15S22.ChannelId.SUM_REACTIVE_POWER, new FloatDoublewordElement(56)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L1_N, new FloatDoublewordElement(58)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L2_N, new FloatDoublewordElement(60)), //
						m(Mpr15S22.ChannelId.APPARENT_POWER_L3_N, new FloatDoublewordElement(62))), //
				new FC3ReadRegistersTask(66, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_IMPORT_APPARENT_POWER, new FloatDoublewordElement(66)), //
						m(Mpr15S22.ChannelId.TOTAL_EXPORT_APPARENT_POWER, new FloatDoublewordElement(68)), //
						m(Mpr15S22.ChannelId.SUM_APPARENT_POWER, new FloatDoublewordElement(70)), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L1, new SignedDoublewordElement(72), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L2, new SignedDoublewordElement(74), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.POWER_FACTOR_L3, new SignedDoublewordElement(76), SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(80, Priority.HIGH,
						m(Mpr15S22.ChannelId.SUM_POWER_FACTOR, new SignedDoublewordElement(80), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.COSPHI_L1, new SignedDoublewordElement(82), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.COSPHI_L2, new SignedDoublewordElement(84), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.COSPHI_L3, new SignedDoublewordElement(86), SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(90, Priority.HIGH,
						m(Mpr15S22.ChannelId.SUM_COS_PHI, new SignedDoublewordElement(90), SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.ROTATION_FIELD, new SignedDoublewordElement(92))), //
				new FC3ReadRegistersTask(98, Priority.HIGH,
						m(Mpr15S22.ChannelId.L1_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(98),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L2_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(100),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L3_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(102),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L4_PHASE_VOLTAGE_ANGLE, new UnsignedDoublewordElement(104),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L1_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(106),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L2_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(108),
								SCALE_FACTOR_MINUS_1), //
						m(Mpr15S22.ChannelId.L3_PHASE_CURRENT_ANGLE, new UnsignedDoublewordElement(110),
								SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(154, Priority.HIGH,
						m(Mpr15S22.ChannelId.HOUR_METER_NON_RESETABLE, new UnsignedDoublewordElement(154),
								SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.WORKING_HOUR_COUNTER, new UnsignedDoublewordElement(156),
								SCALE_FACTOR_MINUS_3), //
						m(Mpr15S22.ChannelId.INPUT_STATUS, new UnsignedDoublewordElement(158)), //
						m(Mpr15S22.ChannelId.OUTPUT_STATUS, new UnsignedDoublewordElement(160))), //

				// Energy registers
				new FC3ReadRegistersTask(200, Priority.HIGH,
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(200)), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(204)), //
						m(Mpr15S22.ChannelId.CONSUMED_ACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(208))), //
				new FC3ReadRegistersTask(216, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_CONSUMED_ENERGY_L1_L3, new UnsignedQuadruplewordElement(216)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(220)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(224)), //
						m(Mpr15S22.ChannelId.DELIVERED_ACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(228))), //
				new FC3ReadRegistersTask(236, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_DELIVERED_ENERGY_L1_L3, new UnsignedQuadruplewordElement(236)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L1, new UnsignedQuadruplewordElement(240)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L2, new UnsignedQuadruplewordElement(244)), //
						m(Mpr15S22.ChannelId.CONSUMED_APPARENT_ENERGY_L3, new UnsignedQuadruplewordElement(248))), //
				new FC3ReadRegistersTask(256, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_CONSUMED_APPARENT_ENERGY_L1_L3,
								new UnsignedQuadruplewordElement(256)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L1, new UnsignedQuadruplewordElement(260)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L2, new UnsignedQuadruplewordElement(264)), //
						m(Mpr15S22.ChannelId.DELIVERED_APPARENT_ENERGY_L3, new UnsignedQuadruplewordElement(268))), //
				new FC3ReadRegistersTask(276, Priority.HIGH,
						m(Mpr15S22.ChannelId.TOTAL_DELIVERED_APPARENT_ENERGY_L1_L3,
								new UnsignedQuadruplewordElement(276)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(280)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(284)), //
						m(Mpr15S22.ChannelId.QUADRANT_1_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(288))), //
				new FC3ReadRegistersTask(296, Priority.HIGH,
						m(Mpr15S22.ChannelId.QUADRANT_1_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(296)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(300)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(304)), //
						m(Mpr15S22.ChannelId.QUADRANT_2_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(308))), //
				new FC3ReadRegistersTask(316, Priority.HIGH,
						m(Mpr15S22.ChannelId.QUADRANT_2_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(316)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(320)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(324)), //
						m(Mpr15S22.ChannelId.QUADRANT_3_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(328))), //
				new FC3ReadRegistersTask(336, Priority.HIGH,
						m(Mpr15S22.ChannelId.QUADRANT_3_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(336)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L1, new UnsignedQuadruplewordElement(340)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L2, new UnsignedQuadruplewordElement(344)), //
						m(Mpr15S22.ChannelId.QUADRANT_4_REACTIVE_ENERGY_L3, new UnsignedQuadruplewordElement(348))), //
				new FC3ReadRegistersTask(356, Priority.HIGH,
						m(Mpr15S22.ChannelId.QUADRANT_4_TOTAL_REACTIVE_ENERGY, new UnsignedQuadruplewordElement(356)), //
						m(Mpr15S22.ChannelId.NUMBER_OF_PULSE_METER, new UnsignedDoublewordElement(360)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_1, new UnsignedDoublewordElement(362)), //
						m(Mpr15S22.ChannelId.TOTAL_PULSE_METER_INPUT_2, new UnsignedDoublewordElement(364)) //
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
