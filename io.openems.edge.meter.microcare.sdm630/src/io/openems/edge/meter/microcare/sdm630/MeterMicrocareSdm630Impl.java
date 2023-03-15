package io.openems.edge.meter.microcare.sdm630;

import java.nio.ByteOrder;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;

import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Microcare.SDM630", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterMicrocareSdm630Impl extends AbstractOpenemsModbusComponent
		implements MeterMicrocareSdm630, AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterMicrocareSdm630Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterMicrocareSdm630.ChannelId.values() //
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
		final var offset = 30001;
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(30001 - offset, Priority.HIGH,
						m(new FloatDoublewordElement(30001 - offset).wordOrder(WordOrder.MSWLSW)
								.byteOrder(ByteOrder.BIG_ENDIAN))
										.m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
												ElementToChannelConverter.SCALE_FACTOR_3)//
										.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_3)//
										.build(),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2,
								new FloatDoublewordElement(30003 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3,
								new FloatDoublewordElement(30005 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(30007 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new FloatDoublewordElement(30009 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new FloatDoublewordElement(30011 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new FloatDoublewordElement(30013 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(30015 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(30017 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30019 - offset, 30024 - offset),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new FloatDoublewordElement(30025 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new FloatDoublewordElement(30027 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new FloatDoublewordElement(30029 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30031 - offset, 30048 - offset),
						m(SymmetricMeter.ChannelId.CURRENT,
								new FloatDoublewordElement(30049 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(30051 - offset, 30052 - offset),
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(30053 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30055 - offset, 30060 - offset),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new FloatDoublewordElement(30061 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30063 - offset, 30070 - offset),
						m(SymmetricMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(30071 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30073 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,								
								new FloatDoublewordElement(30075 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(MeterMicrocareSdm630.ChannelId.REACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30077 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(MeterMicrocareSdm630.ChannelId.REACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30079 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3)));
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
