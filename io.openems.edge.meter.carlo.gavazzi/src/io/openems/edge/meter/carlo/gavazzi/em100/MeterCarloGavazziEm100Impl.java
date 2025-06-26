package io.openems.edge.meter.carlo.gavazzi.em100;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE;

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
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.CarloGavazzi.EM100", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterCarloGavazziEm100Impl extends AbstractOpenemsModbusComponent
		implements MeterCarloGavazziEm100, SinglePhaseMeter, ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public MeterCarloGavazziEm100Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				MeterCarloGavazziEm100.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromReactivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
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
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var offset = 300000 + 1;
		/**
		 * See Modbus definition PDF-file in doc directory and
		 * https://www.gavazziautomation.com/fileadmin/images/PIM/OTHERSTUFF/COMPRO/EM111_EM112_ET112_CP.pdf
		 */

		final ElectricityMeter.ChannelId energyChannelId300017;
		final ElectricityMeter.ChannelId energyChannelId300033;
		if (this.config.invert()) {
			energyChannelId300017 = ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
			energyChannelId300033 = ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
		} else {
			energyChannelId300017 = ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
			energyChannelId300033 = ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
		}

		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(300001 - offset, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE, new SignedDoublewordElement(300001 - offset)
								.wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_2)),
				new FC4ReadInputRegistersTask(300003 - offset, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT,
								new SignedDoublewordElement(300003 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(300005 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(MeterCarloGavazziEm100.ChannelId.APPARENT_POWER,
								new SignedDoublewordElement(300007 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(300009 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert()))),

				new FC4ReadInputRegistersTask(300016 - offset, Priority.LOW, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(300016 - offset),
								SCALE_FACTOR_2),
						m(energyChannelId300017,
								new SignedDoublewordElement(300017 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_2),
						new DummyRegisterElement(300019 - offset, 300032 - offset),
						m(energyChannelId300033,
								new SignedDoublewordElement(300033 - offset).wordOrder(WordOrder.LSWMSW),
								SCALE_FACTOR_2)));
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	public String debugLog() {
		return this.getPhase() + ":" + this.getActivePower().asString();
	}
}
