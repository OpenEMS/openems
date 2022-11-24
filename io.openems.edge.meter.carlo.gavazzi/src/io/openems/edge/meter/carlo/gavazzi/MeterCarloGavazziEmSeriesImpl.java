package io.openems.edge.meter.carlo.gavazzi;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.CarloGavazzi.EM3X0", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterCarloGavazziEmSeriesImpl extends AbstractOpenemsModbusComponent
		implements MeterCarloGavazziEmSeries, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	private Config config;

	public MeterCarloGavazziEmSeriesImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterCarloGavazziEmSeries.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var offset = 300000 + 1;
		/**
		 * See Modbus definition PDF-file in doc directory and
		 * https://www.galoz.co.il/wp-content/uploads/2014/11/EM341-Modbus.pdf
		 */

		final SymmetricMeter.ChannelId energyChannelA;
		final SymmetricMeter.ChannelId energyChannelB;
		if (this.config.invert()) {
			energyChannelA = SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
			energyChannelB = SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
		} else {
			energyChannelA = SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
			energyChannelB = SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
		}

		ModbusProtocol modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(300001 - offset, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
								new SignedDoublewordElement(300001 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2,
								new SignedDoublewordElement(300003 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3,
								new SignedDoublewordElement(300005 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2)),

				new FC4ReadInputRegistersTask(300013 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new SignedDoublewordElement(300013 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new SignedDoublewordElement(300015 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new SignedDoublewordElement(300017 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(this.config.invert()))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300019 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new SignedDoublewordElement(300019 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new SignedDoublewordElement(300021 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new SignedDoublewordElement(300023 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300025 - offset, Priority.HIGH, //
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L1,
								new SignedDoublewordElement(300025 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L2,
								new SignedDoublewordElement(300027 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L3,
								new SignedDoublewordElement(300029 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300031 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new SignedDoublewordElement(300031 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new SignedDoublewordElement(300033 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new SignedDoublewordElement(300035 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300041 - offset, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(300041 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER,
								new SignedDoublewordElement(300043 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(300045 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))));

		switch (this.config.model()) {
		case EM_ET_300_SERIES: { // EM/ET300 SERIES
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(300052 - offset, Priority.LOW, //
					// 300052 ~ Hz
					m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedWordElement(300052 - offset),
							ElementToChannelConverter.SCALE_FACTOR_2),
					// 300053 ~ kWh (+) TOT
					m(energyChannelA, new SignedDoublewordElement(300053 - offset).wordOrder(WordOrder.LSWMSW),
							ElementToChannelConverter.SCALE_FACTOR_2)));
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(300079 - offset, Priority.LOW, //
					// 300079 ~ kWh (-) TOT
					m(energyChannelB, new SignedDoublewordElement(300079 - offset).wordOrder(WordOrder.LSWMSW),
							ElementToChannelConverter.SCALE_FACTOR_2)));
			break;
		}
		case EM_24_DIN: { // EM24-DIN
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(300056 - offset, Priority.LOW, //
					// 300056 ~ Hz
					m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedWordElement(300056 - offset),
							ElementToChannelConverter.SCALE_FACTOR_2)));
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(300063 - offset, Priority.LOW, //
					// 300063 ~ KWh(+) TOT
					m(energyChannelA, new SignedDoublewordElement(300063 - offset).wordOrder(WordOrder.LSWMSW),
							ElementToChannelConverter.SCALE_FACTOR_2)));
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(300093 - offset, Priority.LOW, //
					// 300093 ~ KWh(-) TOT
					m(energyChannelB, new SignedDoublewordElement(300093 - offset).wordOrder(WordOrder.LSWMSW),
							ElementToChannelConverter.SCALE_FACTOR_2)));
			break;
		}
		}

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
