package io.openems.edge.meter.chint.ddsu666;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE;
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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Chint.DDSU666", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class MeterChintDdsu666Impl extends AbstractOpenemsModbusComponent implements MeterChintDdsu666,
		SinglePhaseMeter, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private Config config;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MeterChintDdsu666Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				MeterChintDdsu666.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromReactivePower(this);
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
	protected ModbusProtocol defineModbusProtocol() {
		final var modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(0x0006, Priority.HIGH,
						m(MeterChintDdsu666.ChannelId.COMMUNICATION_ADDRESS, new UnsignedWordElement(0x0006))),
				new FC3ReadRegistersTask(0x2000, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(0x2000), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(0x2002), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(0x2004),
								SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())),
						new DummyRegisterElement(0x2006, 0x200D)),

				new FC3ReadRegistersTask(0x200E, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(0x200E), SCALE_FACTOR_3),
						new DummyRegisterElement(0x2010, 0x2011)));

		if (this.config.invert()) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x4000, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(0x4000),
							SCALE_FACTOR_3)));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x4000, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(0x4000),
							SCALE_FACTOR_3)));
		}

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return this.getPhase() + ":" + this.getActivePower().asString();
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				SinglePhaseMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MeterChintDdsu666Impl.class, accessMode, 100) //
						.build());
	}
}
