package io.openems.edge.meter.schneider.acti9.smartlink;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Schneider.Acti9.Smartlink", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSchneiderActi9Smartlink extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;
	private boolean inverted;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSchneiderActi9Smartlink() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
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
		this.inverted = config.invert();
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
		final var offset = 1;
		/**
		 * See Datasheet PDF-file in doc directory.
		 *
		 * Phase specific reactive Power, Single Current and Voltage figures are not
		 * implemented by this meter.
		 */
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(3000 - offset, Priority.HIGH,
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(3000 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(3002 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(3004 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3028 - offset, Priority.LOW,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(3028 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(3030 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(3032 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3054 - offset, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(3054 - offset),
								ElementToChannelConverter.INVERT_IF_TRUE(this.inverted)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(3056 - offset),
								ElementToChannelConverter.INVERT_IF_TRUE(this.inverted)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(3058 - offset),
								ElementToChannelConverter.INVERT_IF_TRUE(this.inverted)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(3060 - offset),
								ElementToChannelConverter.INVERT_IF_TRUE(this.inverted)),
						new DummyRegisterElement(3062 - offset, 3067 - offset),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(3068 - offset),
								ElementToChannelConverter.INVERT_IF_TRUE(this.inverted))),
				new FC4ReadInputRegistersTask(3110 - offset, Priority.LOW,
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(3110 - offset),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3208 - offset, Priority.LOW,
						m(this.inverted ? ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY
								: ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedQuadruplewordElement(3208 - offset)),
						m(this.inverted ? ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
								: ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedQuadruplewordElement(3212 - offset))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
