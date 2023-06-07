package io.openems.edge.meter.schneider.acti9.smartlink;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Schneider.Acti9.Smartlink", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSchneiderActi9Smartlink extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;
	private boolean inverted;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSchneiderActi9Smartlink() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values() //
		);
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
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(3000 - offset),
								SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(3002 - offset),
								SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(3004 - offset),
								SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3028 - offset, Priority.LOW,
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(3028 - offset),
								SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(3030 - offset),
								SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(3032 - offset),
								SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3054 - offset, Priority.HIGH,
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(3054 - offset),
								INVERT_IF_TRUE(this.inverted)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(3056 - offset),
								INVERT_IF_TRUE(this.inverted)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(3058 - offset),
								INVERT_IF_TRUE(this.inverted)),
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(3060 - offset),
								INVERT_IF_TRUE(this.inverted)),
						new DummyRegisterElement(3062 - offset, 3067 - offset),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(3068 - offset),
								INVERT_IF_TRUE(this.inverted))),
				new FC4ReadInputRegistersTask(3110 - offset, Priority.LOW,
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(3110 - offset),
								SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(3208 - offset, Priority.LOW,
						m(this.inverted ? SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY
								: SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedQuadruplewordElement(3208 - offset)),
						m(this.inverted ? SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
								: SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedQuadruplewordElement(3212 - offset))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
