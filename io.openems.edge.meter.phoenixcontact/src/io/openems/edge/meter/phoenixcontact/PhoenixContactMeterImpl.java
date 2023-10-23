package io.openems.edge.meter.phoenixcontact;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.PhoenixContact", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PhoenixContactMeterImpl extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, PhoenixContactMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	private MeterType type = MeterType.PRODUCTION;
	private boolean invert = false;

	public PhoenixContactMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				PhoenixContactMeter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.type = config.type();
		this.invert = config.invert();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x8006, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(0x8006) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(0x8008) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(0x800A) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(0x800C) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(0x800E) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(0x8010) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(0x8012) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(0x8014) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3), //
						new DummyRegisterElement(0x8016, 0x8015), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(0x8016) //
								.wordOrder(LSWMSW), INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(0x8018, 0x801D), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(0x801E) //
								.wordOrder(LSWMSW), INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(0x8020) //
								.wordOrder(LSWMSW), INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(0x8022) //
								.wordOrder(LSWMSW), INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(0x8024, 0x803C), //
						m(ElectricityMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(0x803D) //
								.wordOrder(LSWMSW), SCALE_FACTOR_3)), //

				new FC3ReadRegistersTask(0x8100, Priority.HIGH, //
						m(this.invert ? ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
								: ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(0x8100) //
										.wordOrder(LSWMSW)), //
						new DummyRegisterElement(0x8102, 0x8105), //
						m(this.invert ? ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY
								: ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(0x8106) //
										.wordOrder(LSWMSW)) //
				));

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.type;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(PhoenixContactMeter.class, accessMode, 100) //
						.build());
	}
}
