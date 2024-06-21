package io.openems.edge.meter.victron.grid;

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
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Victron.Grid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class VictronMeterImpl extends AbstractOpenemsModbusComponent
		implements VictronMeter, AsymmetricMeter, SymmetricMeter, OpenemsComponent {

//	private final Logger log = LoggerFactory.getLogger(VictronMeterImpl.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	protected Config config;

	public VictronMeterImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				VictronMeter.ChannelId.values() //
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
		this.installListeners();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	private void installListeners() {
		final Consumer<Value<Integer>> consumptionEnergySum = ignore -> {
			this._setActiveConsumptionEnergy(
					TypeUtils.sum(this.getActiveConsumptionEnergyL1Channel().getNextValue().get(),
							this.getActiveConsumptionEnergyL2Channel().getNextValue().get(),
							this.getActiveConsumptionEnergyL3Channel().getNextValue().get()));
		};
		this.getActiveConsumptionEnergyL1Channel().onSetNextValue(consumptionEnergySum);
		this.getActiveConsumptionEnergyL2Channel().onSetNextValue(consumptionEnergySum);
		this.getActiveConsumptionEnergyL3Channel().onSetNextValue(consumptionEnergySum);

		final Consumer<Value<Integer>> productionEnergySum = ignore -> {
			this._setActiveProductionEnergy(
					TypeUtils.sum(this.getActiveProductionEnergyL1Channel().getNextValue().get(),
							this.getActiveProductionEnergyL2Channel().getNextValue().get(),
							this.getActiveProductionEnergyL3Channel().getNextValue().get()));
		};
		this.getActiveProductionEnergyL1Channel().onSetNextValue(productionEnergySum);
		this.getActiveProductionEnergyL2Channel().onSetNextValue(productionEnergySum);
		this.getActiveProductionEnergyL3Channel().onSetNextValue(productionEnergySum);
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(2600, Priority.LOW,
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(2600)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(2601)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(2602))),
				new FC3ReadRegistersTask(2609, Priority.LOW,
						m(VictronMeter.ChannelId.SERIAL_NUMBER, new StringWordElement(2609, 7)),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(2616),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedWordElement(2617),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(2618),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedWordElement(2619),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(2620),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedWordElement(2621),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(VictronMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, new UnsignedDoublewordElement(2622),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(2624),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(2626),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(2628),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(2630),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(VictronMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(2632),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)
				// ILLEGAL DATA ADRESS
//						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(2634),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
//						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(2636),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)
				)); //

	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

}
