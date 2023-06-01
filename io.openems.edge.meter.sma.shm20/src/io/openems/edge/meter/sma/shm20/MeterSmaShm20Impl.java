package io.openems.edge.meter.sma.shm20;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.SMA.SHM20", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSmaShm20Impl extends AbstractOpenemsModbusComponent
		implements MeterSmaShm20, AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSmaShm20Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterSmaShm20.ChannelId.values() //
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
		var modbusProtocol = new ModbusProtocol(this,
				// Consumption and Production Energy
				new FC3ReadRegistersTask(30581, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(30581)),
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(30583))),
				// Power Readings
				new FC3ReadRegistersTask(30865, Priority.HIGH, //
						m(MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER, new SignedDoublewordElement(30865)),
						m(MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER, new SignedDoublewordElement(30867))),
				// Voltage, Power and Reactive Power
				new FC3ReadRegistersTask(31253, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(31253), SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(31255), SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(31257), SCALE_FACTOR_1),
						m(MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L1, new UnsignedDoublewordElement(31259)),
						m(MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L2, new UnsignedDoublewordElement(31261)),
						m(MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L3, new UnsignedDoublewordElement(31263)),
						m(MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L1, new UnsignedDoublewordElement(31265)),
						m(MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L2, new UnsignedDoublewordElement(31267)),
						m(MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L3, new UnsignedDoublewordElement(31269)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(31271)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(31273)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(31275)),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(31277))),
				// Current
				new FC3ReadRegistersTask(31435, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(31435)),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(31437)),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedDoublewordElement(31439))),
				// Frequency
				new FC3ReadRegistersTask(31447, Priority.LOW, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(31447), SCALE_FACTOR_1)));

		// Calculates required Channels from other existing Channels.
		this.addCalculateChannelListeners();

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	private void addCalculateChannelListeners() {
		// Active Power
		CalculatePower.of(this, MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER,
				MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER, SymmetricMeter.ChannelId.ACTIVE_POWER);
		CalculatePower.of(this, MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L1,
				MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L1, AsymmetricMeter.ChannelId.ACTIVE_POWER_L1);
		CalculatePower.of(this, MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L2,
				MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L2, AsymmetricMeter.ChannelId.ACTIVE_POWER_L2);
		CalculatePower.of(this, MeterSmaShm20.ChannelId.ACTIVE_CONSUMPTION_POWER_L3,
				MeterSmaShm20.ChannelId.ACTIVE_PRODUCTION_POWER_L3, AsymmetricMeter.ChannelId.ACTIVE_POWER_L3);

		// Average Voltage from current L1, L2 and L3
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

		// Sum Current from Current L1, L2 and L3
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

	private static class CalculatePower implements Consumer<Value<Integer>> {

		private final IntegerReadChannel consChannel;
		private final IntegerReadChannel prodChannel;
		private final IntegerReadChannel targetChannel;

		public static CalculatePower of(MeterSmaShm20Impl parent,
				io.openems.edge.common.channel.ChannelId consChannelId,
				io.openems.edge.common.channel.ChannelId prodChannelId,
				io.openems.edge.common.channel.ChannelId targetChannelId) {
			return new CalculatePower(parent, consChannelId, prodChannelId, targetChannelId);
		}

		private CalculatePower(MeterSmaShm20Impl parent, io.openems.edge.common.channel.ChannelId consChannelId,
				io.openems.edge.common.channel.ChannelId prodChannelId,
				io.openems.edge.common.channel.ChannelId targetChannelId) {

			// Get actual Channels
			this.consChannel = parent.channel(consChannelId);
			this.prodChannel = parent.channel(prodChannelId);
			this.targetChannel = parent.channel(targetChannelId);

			// Add Listeners
			this.prodChannel.onSetNextValue(this);
			this.consChannel.onSetNextValue(this);
		}

		@Override
		public void accept(Value<Integer> ignore) {
			var prodValue = this.prodChannel.getNextValue();
			var consValue = this.consChannel.getNextValue();
			final Integer result;
			if (prodValue.isDefined() && consValue.isDefined()) {
				result = prodValue.get() - consValue.get();
			} else {
				result = null;
			}
			this.targetChannel.setNextValue(result);
		}
	}

}
