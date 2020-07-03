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

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Microcare.SDM630", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterMicrocareSDM630 extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, AsymmetricMeter, SymmetricMeter {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterMicrocareSDM630() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)

	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		REACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		REACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final int OFFSET = 30001;
		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(30001 - OFFSET, Priority.LOW,
				// VOLTAGE
				// Overall Voltage
				// measured from L1
				m(SymmetricMeter.ChannelId.VOLTAGE,
						new FloatDoublewordElement(30001 - OFFSET).wordOrder(WordOrder.MSWLSW)
								.byteOrder(ByteOrder.BIG_ENDIAN),
						ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(30001 - OFFSET, Priority.LOW,
						// Phase 1 voltage
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
								new FloatDoublewordElement(30001 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 2 voltage
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2,
								new FloatDoublewordElement(30003 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 3 voltage
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3,
								new FloatDoublewordElement(30005 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(30007 - OFFSET, Priority.HIGH,
						// CURRENT
						// Phase 1 current
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(30007 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 2 current
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new FloatDoublewordElement(30009 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 3 current
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new FloatDoublewordElement(30011 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// APPARENT POWER
						// phase 1 VA
						m(ChannelId.APPARENT_POWER_L1,
								new FloatDoublewordElement(30013 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 VA
						m(ChannelId.APPARENT_POWER_L2,
								new FloatDoublewordElement(30015 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 VA
						m(ChannelId.APPARENT_POWER_L3,
								new FloatDoublewordElement(30017 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// ACTIVE POWER
						// phase 1 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new FloatDoublewordElement(30019 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(30021 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(30023 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30025 - OFFSET, 30030 - OFFSET),
						// REACTIVE POWER
						// phase 1 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new FloatDoublewordElement(30031 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new FloatDoublewordElement(30033 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new FloatDoublewordElement(30035 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30037 - OFFSET, 30048 - OFFSET),
						// Overall Current
						m(SymmetricMeter.ChannelId.CURRENT,
								new FloatDoublewordElement(30049 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(30051 - OFFSET, 30052 - OFFSET),
						// total system VA
						m(ChannelId.APPARENT_POWER,
								new FloatDoublewordElement(30053 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30055 - OFFSET, 30056 - OFFSET),
						// total system active power
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(30057 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30059 - OFFSET, 30060 - OFFSET),
						// total system VAr
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new FloatDoublewordElement(30061 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC4ReadInputRegistersTask(30071 - OFFSET, Priority.LOW,
						// frequency
						m(SymmetricMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(30071 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// active energy import/export
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30073 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30075 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// reactive energy import/export
						m(ChannelId.REACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30077 - OFFSET).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChannelId.REACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(30079 - OFFSET)
								.wordOrder(WordOrder.MSWLSW).byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.DIRECT_1_TO_1)));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

}
