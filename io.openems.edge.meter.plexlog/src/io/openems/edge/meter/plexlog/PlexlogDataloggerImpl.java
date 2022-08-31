package io.openems.edge.meter.plexlog;

import java.util.function.BiConsumer;

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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Plexlog.Datalogger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PlexlogDataloggerImpl extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, PlexlogDatalogger, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public PlexlogDataloggerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				PlexlogDatalogger.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
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
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var modbusProtocol = new ModbusProtocol(this, new FC4ReadInputRegistersTask(0, Priority.HIGH, //
				this.m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0)), //
				new DummyRegisterElement(2, 7), //
				this.m(PlexlogDatalogger.ChannelId.TOTAL_PRODUCTION, new SignedDoublewordElement(8)), //
				this.m(PlexlogDatalogger.ChannelId.PRODUCTION_EXPONENT, new SignedWordElement(10)), //
				this.m(PlexlogDatalogger.ChannelId.TOTAL_CONSUMPTION, new SignedDoublewordElement(11)), //
				this.m(PlexlogDatalogger.ChannelId.CONSUMPTION_EXPONENT, new SignedWordElement(13)) //
		));

		this.getTotalProductionChannel().onSetNextValue(this::onTotalProductionNextValue);
		this.getProductionExponentChannel().onSetNextValue(this::onProductionExponentNextValue);

		this.getTotalConsumptionChannel().onSetNextValue(this::onTotalConsumptionNextValue);
		this.getConsumptionExponentChannel().onSetNextValue(this::onConsumptionExponentNextValue);

		return modbusProtocol;
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	private void onConsumptionExponentNextValue(Value<Integer> value) {
		this.doIfBothPresent(this.getTotalConsumption(), value, this::calculateActiveConsumptionEnergy);
	}

	private void onProductionExponentNextValue(Value<Integer> value) {
		this.doIfBothPresent(this.getTotalProduction(), value, this::calculateActiveProductionEnergy);
	}

	private void onTotalConsumptionNextValue(Value<Integer> value) {
		this.doIfBothPresent(value, this.getTotalConsumption(), this::calculateActiveConsumptionEnergy);
	}

	private void onTotalProductionNextValue(Value<Integer> value) {
		this.doIfBothPresent(value, this.getProductionExponent(), this::calculateActiveProductionEnergy);
	}

	private void doIfBothPresent(Value<Integer> total, Value<Integer> exponent, BiConsumer<Integer, Integer> consumer) {
		if (total.asOptional().isEmpty()) {
			return;
		}
		if (exponent.asOptional().isEmpty()) {
			return;
		}
		consumer.accept(total.get(), exponent.get());
	}

	private void calculateActiveConsumptionEnergy(int totalConsumption, int exponent) {
		this._setActiveConsumptionEnergy(totalConsumption * (long) Math.pow(10, exponent));
	}

	private void calculateActiveProductionEnergy(int totalProduction, int exponent) {
		this._setActiveProductionEnergy(totalProduction * (long) Math.pow(10, exponent));
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
}
