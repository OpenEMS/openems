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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.carlo.gavazzi.common.AbstractMeterCarloGavazziEmSeries;
import io.openems.edge.meter.carlo.gavazzi.common.MeterCarloGavazziEmSeries;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.CarloGavazzi.EM300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterCarloGavazziEm300Impl extends AbstractMeterCarloGavazziEmSeries
		implements MeterCarloGavazziEmSeries, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	private Config config;

	public MeterCarloGavazziEm300Impl() {
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

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), config.invert());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
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

		
		this.modbusProtocol.addTask(
				new FC4ReadInputRegistersTask(300052 - offset, Priority.LOW, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedWordElement(300052 - offset),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(energyChannelA,
								new SignedDoublewordElement(300053 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						new DummyRegisterElement(300055 - offset, 300078 - offset), //
						m(energyChannelB,
								new SignedDoublewordElement(300079 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2)));
		
		return this.modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
