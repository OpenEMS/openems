package io.openems.edge.victron.meter.acin;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Victron.Meter.AcIn", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class VictronAcInPowerMeterImpl extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, OpenemsComponent, ModbusSlave {

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	public VictronAcInPowerMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values() //
		);
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumReactivePowerFromPhases(this);
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

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
		return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		/*
		 * Invert power values
		 */
		boolean invert = this.config.invert();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(3, Priority.HIGH, //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new SignedWordElement(3), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L2, new SignedWordElement(4), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L3, new SignedWordElement(5), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(6), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(7), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(8), SCALE_FACTOR_2),
						this.m(ElectricityMeter.ChannelId.FREQUENCY, new SignedWordElement(9), SCALE_FACTOR_1),
						new DummyRegisterElement(10, 11),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(12),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(invert)),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(13),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(invert)),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(14),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(invert))));
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
