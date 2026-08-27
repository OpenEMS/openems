package io.openems.edge.meter.chint.dtsu666;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Chint.DTSU666", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class MeterChintDtsu666Impl extends AbstractOpenemsModbusComponent
		implements MeterChintDtsu666, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private Config config;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MeterChintDtsu666Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterChintDtsu666.ChannelId.values() //
		);

		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
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
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotation = this.getPhaseRotation();
		final var modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(0x2006, Priority.HIGH, //
						m(phaseRotation.channelVoltageL1(), new FloatDoublewordElement(0x2006), SCALE_FACTOR_2), //
						m(phaseRotation.channelVoltageL2(), new FloatDoublewordElement(0x2008), SCALE_FACTOR_2), //
						m(phaseRotation.channelVoltageL3(), new FloatDoublewordElement(0x200A), SCALE_FACTOR_2), //
						m(phaseRotation.channelCurrentL1(), new FloatDoublewordElement(0x200C), DIRECT_1_TO_1), //
						m(phaseRotation.channelCurrentL2(), new FloatDoublewordElement(0x200E), DIRECT_1_TO_1), //
						m(phaseRotation.channelCurrentL3(), new FloatDoublewordElement(0x2010), DIRECT_1_TO_1), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(0x2012),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(phaseRotation.channelActivePowerL1(), new FloatDoublewordElement(0x2014),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(phaseRotation.channelActivePowerL2(), new FloatDoublewordElement(0x2016),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(phaseRotation.channelActivePowerL3(), new FloatDoublewordElement(0x2018),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))), //
				new FC4ReadInputRegistersTask(0x202A, Priority.HIGH, //
						m(MeterChintDtsu666.ChannelId.TOTAL_POWER_FACTOR, new FloatDoublewordElement(0x202A),
								DIRECT_1_TO_1)), //
				new FC4ReadInputRegistersTask(0x2044, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(0x2044), SCALE_FACTOR_1)));

		if (this.config.invert()) {
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(0x4000, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(0x4000),
							SCALE_FACTOR_3)));
		} else {
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(0x4000, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(0x4000),
							SCALE_FACTOR_3)));
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
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				MeterChintDtsu666.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MeterChintDtsu666Impl.class, accessMode, 100) //
						.build() //
		);
	}
}
