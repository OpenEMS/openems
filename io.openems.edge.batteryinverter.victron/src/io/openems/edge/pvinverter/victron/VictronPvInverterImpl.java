package io.openems.edge.pvinverter.victron;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		}) //
public class VictronPvInverterImpl extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, ManagedSymmetricPvInverter, VictronPvInverter, OpenemsComponent {

//	private final Logger log = LoggerFactory.getLogger(VictronPvInverterImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public VictronPvInverterImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				VictronPvInverter.ChannelId.values() //
		);

		AsymmetricMeter.initializePowerSumChannels(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setMaxApparentPower(10000);
		this.installListener();
	}

	private void installListener() {
		final Consumer<Value<Integer>> productionEnergySum = ignore -> {
			this._setActiveProductionEnergy(1000 * TypeUtils.sum( // converting from kWh to Wh
					this.getActiveProductionEnergyL1Channel().getNextValue().get(),
					this.getActiveProductionEnergyL2Channel().getNextValue().get(),
					this.getActiveProductionEnergyL3Channel().getNextValue().get()));
		};
		this.getActiveProductionEnergyL1Channel().onSetNextValue(productionEnergySum);
		this.getActiveProductionEnergyL2Channel().onSetNextValue(productionEnergySum);
		this.getActiveProductionEnergyL3Channel().onSetNextValue(productionEnergySum);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1026, Priority.HIGH,
						m(VictronPvInverter.ChannelId.POSITION, new UnsignedWordElement(1026)),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(1027),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedWordElement(1028),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(1029)),
						new DummyRegisterElement(1030),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(1031),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedWordElement(1032),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(1033)),
						new DummyRegisterElement(1034),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(1035),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedWordElement(1036),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(1037)),
						new DummyRegisterElement(1038),
						m(VictronPvInverter.ChannelId.SERIAL_NUMBER, new StringWordElement(1039, 7)),
						m(VictronPvInverter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(1046),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronPvInverter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(1048),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronPvInverter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(1050),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)
				// ILLEGAL DATA ADRESS
//						m(VictronPvInverter.ChannelId.TOTAL_POWER, new SignedDoublewordElement(1052)),
//						m(VictronPvInverter.ChannelId.MAXIMUM_POWER_CAPACITY, new UnsignedDoublewordElement(1054)),
//						m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, new UnsignedDoublewordElement(1056))
				)); //

	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}
}
