package io.openems.edge.meter.carlo.gavazzi.em300;

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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.CarloGavazzi.EM300", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterCarloGavazziEm300 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterCarloGavazziEm300() {
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
		REACTIVE_ENERGY_POSITIVE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		REACTIVE_ENERGY_NEGATIVE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS));

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
		final int OFFSET = 300000 + 1;
		/**
		 * See Modbus definition PDF-file in doc directory and
		 * https://www.galoz.co.il/wp-content/uploads/2014/11/EM341-Modbus.pdf
		 */
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(300001 - OFFSET, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
								new SignedDoublewordElement(300001 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2,
								new SignedDoublewordElement(300003 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3,
								new SignedDoublewordElement(300005 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC4ReadInputRegistersTask(300013 - OFFSET, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new SignedDoublewordElement(300013 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new SignedDoublewordElement(300015 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new SignedDoublewordElement(300017 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new SignedDoublewordElement(300019 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new SignedDoublewordElement(300021 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new SignedDoublewordElement(300023 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L1,
								new SignedDoublewordElement(300025 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L2,
								new SignedDoublewordElement(300027 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L3,
								new SignedDoublewordElement(300029 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new SignedDoublewordElement(300031 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new SignedDoublewordElement(300033 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new SignedDoublewordElement(300035 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(300037 - OFFSET, 300040 - OFFSET), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(300041 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER,
								new SignedDoublewordElement(300043 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(300045 - OFFSET).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)));
//				new FC4ReadInputRegistersTask(300052 - OFFSET, Priority.LOW, //
//						m(MeterCarloGavazziEm300.ChannelId.FREQUENCY, new SignedWordElement(300052 - OFFSET),
//								ElementToChannelConverter.SCALE_FACTOR_2),
//						m(MeterCarloGavazziEm300.ChannelId.ACTIVE_ENERGY_POSITIVE,
//								new UnsignedDoublewordElement(300053 - OFFSET),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(MeterCarloGavazziEm300.ChannelId.REACTIVE_ENERGY_POSITIVE,
//								new UnsignedDoublewordElement(300055 - OFFSET),
//								ElementToChannelConverter.SCALE_FACTOR_1)),
//				new FC4ReadInputRegistersTask(300079 - OFFSET, Priority.LOW, //
//						m(MeterCarloGavazziEm300.ChannelId.ACTIVE_ENERGY_NEGATIVE,
//								new UnsignedDoublewordElement(300079 - OFFSET),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(MeterCarloGavazziEm300.ChannelId.REACTIVE_ENERGY_NEGATIVE,
//								new UnsignedDoublewordElement(300081 - OFFSET),
//								ElementToChannelConverter.SCALE_FACTOR_1)));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
