package io.openems.edge.meter.entes.mpr15s22;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.entes.mpr15s22", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EntesMpr15S22Impl extends AbstractOpenemsModbusComponent
		implements EntesMpr15S22, ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public EntesMpr15S22Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EntesMpr15S22.ChannelId.values() //
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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x0000, Priority.HIGH,
						// Measurements
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x0000), SCALE_FACTOR_1),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x0002), SCALE_FACTOR_1),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x0004), SCALE_FACTOR_1)
				// TODO: channel ids for voltage L1-L2, L2-L3, L3-L1
				),

				new FC3ReadRegistersTask(0x000E, Priority.HIGH,
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x000E), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x0010), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x0012),
								SCALE_FACTOR_3)),

				new FC3ReadRegistersTask(0x0016, Priority.HIGH,
						m(ElectricityMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0x0016), SCALE_FACTOR_3),

						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0x0018), SCALE_FACTOR_2),

						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(0x001A)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(0x001C)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(0x001E))),
				// TODO: channel ids for total import active power, totoal export active power
				// and sum of active power
				new FC3ReadRegistersTask(0x0028, Priority.HIGH,
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(0x0028)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(0x002A)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(0x002C))
				// TODO: channel ids for Quadrant 1, 2, 3, 4 total reactive power and sum of
				// reactive power

				// TODO: channel ids for apparent power L1-N, L2-N, L3-N, total import apparent
				// power, total export apparent power, sum of apparent power

				// TODO: channel ids for power factor L1, L2, L3 sum of power factor; CosPhi L1,
				// L2, L3, sum of CosPhi, Rotation field

				// TODO: channel ids for voltage and current angles

				// TODO: channel ids for hour meter and working hour counter

				// Energy
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}
}