package io.openems.edge.meter.inepro.pro380modct;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.inepro.Pro380ModCT", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)


public class Pro380modctImpl extends AbstractOpenemsModbusComponent
		implements Pro380modct, ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;
	private boolean invert;

	public Pro380modctImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Pro380modct.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.invert = config.invert();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(Pro380modct.class, accessMode, 100) //
						.build());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x5002, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, //
								new FloatDoublewordElement(0x5002).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, //
								new FloatDoublewordElement(0x5004).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, //
								new FloatDoublewordElement(0x5006).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.FREQUENCY, //
								new FloatDoublewordElement(0x5008).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(0x500A, 0x500B), // Current* (PRO1 only)
						m(ElectricityMeter.ChannelId.CURRENT_L1, //
								new FloatDoublewordElement(0x500C).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, //
								new FloatDoublewordElement(0x500E).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, //
								new FloatDoublewordElement(0x5010).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
								new FloatDoublewordElement(0x5012).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
								new FloatDoublewordElement(0x5014).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
								new FloatDoublewordElement(0x5016).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
								new FloatDoublewordElement(0x5018).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, //
								new FloatDoublewordElement(0x501A).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, //
								new FloatDoublewordElement(0x501C).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, //
								new FloatDoublewordElement(0x501E).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, //
								new FloatDoublewordElement(0x5020).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(Pro380modct.ChannelId.APPARENT_POWER, //
								new FloatDoublewordElement(0x5022).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(Pro380modct.ChannelId.APPARENT_POWER_L1, //
								new FloatDoublewordElement(0x5024).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(Pro380modct.ChannelId.APPARENT_POWER_L2, //
								new FloatDoublewordElement(0x5026).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(Pro380modct.ChannelId.APPARENT_POWER_L3, //
								new FloatDoublewordElement(0x5028).wordOrder(WordOrder.MSWLSW), //
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)) //
				) //
		);

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x600C, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new FloatDoublewordElement(0x600C).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x600E, 0x6011), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new FloatDoublewordElement(0x6012).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6014).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6016).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new FloatDoublewordElement(0x6018).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x601A, 0x601D), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new FloatDoublewordElement(0x601E).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6020).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6022).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3) //
			));
		} else { // not invert
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x600C, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new FloatDoublewordElement(0x600C).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x600E, 0x6011), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new FloatDoublewordElement(0x6012).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6014).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6016).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new FloatDoublewordElement(0x6018).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x601A, 0x601D), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new FloatDoublewordElement(0x601E).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6020).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6022).wordOrder(WordOrder.MSWLSW), //
							SCALE_FACTOR_3) //
			));
		}

		return modbusProtocol;
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