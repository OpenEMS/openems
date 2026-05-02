package io.openems.edge.meter.chint.ddsu666;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.nio.ByteOrder;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
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
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Chint.DDSU666", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterChintDdsu666Impl extends AbstractOpenemsModbusComponent
		implements MeterChintDdsu666, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.GRID;
	private boolean invert;
	private final Consumer<Value<Integer>> onImportEnergySetNextValueCallback = value -> {
		final var importEnergy = value.get();
		final var channelId = this.invert ? ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
				: ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
		this.channel(channelId).setNextValue(importEnergy == null ? null : Long.valueOf(importEnergy.longValue()));
	};

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MeterChintDdsu666Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterChintDdsu666.ChannelId.values() //
		);

		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();
		this.getActiveImportEnergyChannel().onSetNextValue(this.onImportEnergySetNextValueCallback);

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.getActiveImportEnergyChannel().removeOnSetNextValueCallback(this.onImportEnergySetNextValueCallback);
		super.deactivate();
	}

	@SuppressWarnings("unchecked")
	private Channel<Integer> getActiveImportEnergyChannel() {
		return (Channel<Integer>) this.channel(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY);
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(0x0006, Priority.HIGH,
						m(MeterChintDdsu666.ChannelId.COMMUNICATION_ADDRESS, new UnsignedWordElement(0x0006))),
				new FC3ReadRegistersTask(0x2000, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1,
								new FloatDoublewordElement(0x2000).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(0x2002).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(0x2004).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								io.openems.edge.bridge.modbus.api.ElementToChannelConverter
										.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(0x2006, 0x2009)),

				new FC3ReadRegistersTask(0x200A, Priority.HIGH, //
						new DummyRegisterElement(0x200A, 0x200D),
						m(ElectricityMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(0x200E).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						new DummyRegisterElement(0x2010, 0x2011)),

				new FC3ReadRegistersTask(0x4000, Priority.LOW, //
						m(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY, new FloatDoublewordElement(0x4000)
								.wordOrder(WordOrder.MSWLSW).byteOrder(ByteOrder.BIG_ENDIAN), SCALE_FACTOR_3)));
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActivePower().asString() //
				+ " Q:" + this.getReactivePower().asString() //
				+ " V1:" + this.getVoltageL1().asString() //
				+ " I1:" + this.getCurrentL1().asString() //
				+ " f:" + this.getFrequency().asString() //
				+ " Eimp:" + this.channel(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY).value().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
