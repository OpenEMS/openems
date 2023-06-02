package io.openems.edge.fenecon.pro.pvmeter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SUBTRACT;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.Pro.PvMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
})
public class FeneconProPvMeter extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent {

	private static final int UNIT_ID = 4;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	private String modbusBridgeId;

	public FeneconProPvMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				FeneconProPvMeter.ChannelId.values() //
		);
		AsymmetricMeter.initializePowerSumChannels(this);

		// Active Energy
		final Consumer<Value<Long>> activeEnergySum = ignore -> {
			this._setActiveProductionEnergy(TypeUtils.sum(//
					this.getActiveProductionEnergyL1Channel().value().get(), //
					this.getActiveProductionEnergyL2Channel().value().get(), //
					this.getActiveProductionEnergyL3Channel().value().get()));
		};
		this.getActiveProductionEnergyL1Channel().onSetNextValue(activeEnergySum);
		this.getActiveProductionEnergyL2Channel().onSetNextValue(activeEnergySum);
		this.getActiveProductionEnergyL3Channel().onSetNextValue(activeEnergySum);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.modbusBridgeId = config.modbus_id();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return this.modbusBridgeId;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(121, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(121), SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(122), SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(123), SCALE_FACTOR_2)), //

				new FC3ReadRegistersTask(2035, Priority.LOW, // //
						m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(2035),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(2037, 2065), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(2066), SUBTRACT(10000))), //
				new FC3ReadRegistersTask(2135, Priority.LOW, // //
						m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(2135),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(2137, 2165), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(2166), SUBTRACT(10000))), //
				new FC3ReadRegistersTask(2235, Priority.LOW, // //
						m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(2235),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(2237, 2265), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(2266), SUBTRACT(10000)))//

		);
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
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
