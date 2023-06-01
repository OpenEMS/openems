package io.openems.edge.meter.ziehl.efr4001ip;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

import java.util.function.Consumer;

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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Ziehl.EFR4001IP", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterZiehlEfr4001IpImpl extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, MeterZiehlEfr4001Ip, ModbusComponent, OpenemsComponent {

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterZiehlEfr4001IpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				MeterZiehlEfr4001Ip.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var startingRegister = 0x00B0; // 0x0000 for EFR4000IP
		final var startingRegisterFeedIn = 0x0156; // 0x0084 for EFR4000IP
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(startingRegister, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, //
								new UnsignedDoublewordElement(startingRegister) //
										.wordOrder(LSWMSW), //
								SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, //
								new UnsignedDoublewordElement(startingRegister + 2) // 0x00B2
										.wordOrder(LSWMSW), //
								SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, //
								new UnsignedDoublewordElement(startingRegister + 4) // 0x00B4
										.wordOrder(LSWMSW), //
								SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, //
								new UnsignedDoublewordElement(startingRegister + 6) // 0x00B6
										.wordOrder(LSWMSW)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, //
								new UnsignedDoublewordElement(startingRegister + 8) // 0x00B8
										.wordOrder(LSWMSW)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, //
								new UnsignedDoublewordElement(startingRegister + 10) // 0x00BA
										.wordOrder(LSWMSW)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, //
								new SignedDoublewordElement(startingRegister + 12) // 0x00BC
										.wordOrder(LSWMSW)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, //
								new SignedDoublewordElement(startingRegister + 14) // 0x00BE
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, //
								new SignedDoublewordElement(startingRegister + 16) // 0x00C0
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, //
								new SignedDoublewordElement(startingRegister + 18) // 0x00C2
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, //
								new SignedDoublewordElement(startingRegister + 20) // 0x00C4
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, //
								new SignedDoublewordElement(startingRegister + 22) // 0x00C6
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, //
								new SignedDoublewordElement(startingRegister + 24) // 0x00C8
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, //
								new SignedDoublewordElement(startingRegister + 26) // 0x00CA
										.wordOrder(LSWMSW),
								INVERT_IF_TRUE(this.config.invert())),
						new DummyRegisterElement(startingRegister + 28, startingRegister + 41), // 0x00CC, 0x00D9
						m(SymmetricMeter.ChannelId.FREQUENCY, //
								new SignedDoublewordElement(startingRegister + 42) // 0x00DA
										.wordOrder(LSWMSW),
								SCALE_FACTOR_1)));
		if (this.config.invert()) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(startingRegisterFeedIn, Priority.LOW, //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new SignedDoublewordElement(startingRegisterFeedIn) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new SignedDoublewordElement(startingRegisterFeedIn + 2) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new SignedDoublewordElement(startingRegisterFeedIn + 4) //
									.wordOrder(LSWMSW)), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new SignedDoublewordElement(startingRegisterFeedIn + 6) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new SignedDoublewordElement(startingRegisterFeedIn + 8) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new SignedDoublewordElement(startingRegisterFeedIn + 10) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new SignedDoublewordElement(startingRegisterFeedIn + 12) //
									.wordOrder(LSWMSW)), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new SignedDoublewordElement(startingRegisterFeedIn + 14) //
									.wordOrder(LSWMSW))) //
			);
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(startingRegisterFeedIn, Priority.LOW, //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new SignedDoublewordElement(startingRegisterFeedIn) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new SignedDoublewordElement(startingRegisterFeedIn + 2) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new SignedDoublewordElement(startingRegisterFeedIn + 4) //
									.wordOrder(LSWMSW)), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new SignedDoublewordElement(startingRegisterFeedIn + 6) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new SignedDoublewordElement(startingRegisterFeedIn + 8) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new SignedDoublewordElement(startingRegisterFeedIn + 10) //
									.wordOrder(LSWMSW)), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new SignedDoublewordElement(startingRegisterFeedIn + 12) //
									.wordOrder(LSWMSW)), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new SignedDoublewordElement(startingRegisterFeedIn + 14) //
									.wordOrder(LSWMSW))) //
			);
		}
		// Calculates required Channels from other existing Channels.
		this.calculateSumCurrent();
		this.calculateAverageVoltage();
		return modbusProtocol;
	}

	/**
	 * Calculate Sum Current from Current L1, L2 and L3.
	 */
	private void calculateSumCurrent() {
		final Consumer<Value<Integer>> calculateSumCurrent = ignore -> {
			this._setCurrent(TypeUtils.sum(//
					this.getCurrentL1Channel().getNextValue().get(), //
					this.getCurrentL2Channel().getNextValue().get(), //
					this.getCurrentL3Channel().getNextValue().get() //
			));
		};
		this.getCurrentL1Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL2Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL3Channel().onSetNextValue(calculateSumCurrent);
	}

	/**
	 * Calculate Average Voltage from Current L1, L2 and L3.
	 */
	private void calculateAverageVoltage() {
		final Consumer<Value<Integer>> calculateAverageVoltage = ignore -> {
			this._setVoltage(TypeUtils.averageRounded(//
					this.getVoltageL1Channel().getNextValue().get(), //
					this.getVoltageL2Channel().getNextValue().get(), //
					this.getVoltageL3Channel().getNextValue().get() //
			));
		};
		this.getVoltageL1Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL2Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL3Channel().onSetNextValue(calculateAverageVoltage);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.meterType();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MeterZiehlEfr4001Ip.class, accessMode, 100) //
						.build());
	}
}
