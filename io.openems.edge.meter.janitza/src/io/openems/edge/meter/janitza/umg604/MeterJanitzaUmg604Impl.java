package io.openems.edge.meter.janitza.umg604;

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
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the Janitza UMG 604 power analyzer.
 *
 * <p>
 * https://www.janitza.de/umg-604-pro.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Janitza.UMG604", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterJanitzaUmg604Impl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg604, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;
	/** Invert power values. */
	private boolean invert = false;

	public MeterJanitzaUmg604Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterJanitzaUmg604.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();

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
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1317, Priority.HIGH, //
						m(new FloatDoublewordElement(1317)).m(AsymmetricMeter.ChannelId.VOLTAGE_L1, SCALE_FACTOR_3)//
								.m(SymmetricMeter.ChannelId.VOLTAGE, SCALE_FACTOR_3)//
								.build(),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(1319), //
								SCALE_FACTOR_3), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(1321), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(1323, 1324), //
						m(new FloatDoublewordElement(1325)) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L1, SCALE_FACTOR_3) //
								.m(SymmetricMeter.ChannelId.CURRENT, SCALE_FACTOR_3) //
								.build(), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(1327), //
								SCALE_FACTOR_3), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(1329), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(1331, 1332), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(1333), //
								INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(1335), //
								INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(1337), //
								INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(1339, 1340), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(1341), //
								INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(1343), //
								INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(1345), //
								INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(1347, 1368), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(1369), //
								INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(1371), //
								INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(1439, Priority.LOW, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(1439), //
								SCALE_FACTOR_3)));

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(9851, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(9851)),
					new DummyRegisterElement(9853, 9862),
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(9863))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(9851, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(9851)),
					new DummyRegisterElement(9853, 9862),
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(9863))));
		}

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
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
