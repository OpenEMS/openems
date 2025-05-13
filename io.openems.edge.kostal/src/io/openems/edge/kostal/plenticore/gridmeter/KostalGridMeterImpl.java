package io.openems.edge.kostal.plenticore.gridmeter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "Grid-Meter.Kostal.KSEM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID", //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class KostalGridMeterImpl extends AbstractOpenemsModbusComponent
		implements
			KostalGridMeter,
			ElectricityMeter,
			ModbusComponent,
			OpenemsComponent,
			TimedataProvider,
			EventHandler,
			ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(
			this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(
			this, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	public KostalGridMeterImpl() {
		super(
				//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				KostalGridMeter.ChannelId.values() //
		);

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		this.config = config;
		if (super.activate(context, this.config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// read directly or read via Inverter?
		if (!this.config.viaInverter()) {
			// DEFAULT ("big endian"), direct read (from KSEM)
			// i.e. word-wrapped encoding: LSWMSW vs. MWSLSW
			if (!this.config.wordwrap()) {
				var modbusProtocol = new ModbusProtocol(this, //
						new FC3ReadRegistersTask(0, Priority.HIGH, //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER,
										new UnsignedDoublewordElement(0),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER,
										new UnsignedDoublewordElement(2),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER,
										new UnsignedDoublewordElement(4),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER,
										new UnsignedDoublewordElement(6),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(8, 25), //
								m(ElectricityMeter.ChannelId.FREQUENCY,
										new SignedDoublewordElement(26)), //
								new DummyRegisterElement(28, 39), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L1,
										new UnsignedDoublewordElement(40),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L1,
										new UnsignedDoublewordElement(42),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L1,
										new UnsignedDoublewordElement(44),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L1,
										new UnsignedDoublewordElement(46),
										SCALE_FACTOR_MINUS_1)),
						// new DummyRegisterElement(48, 59), //
						new FC3ReadRegistersTask(60, Priority.HIGH, //
								m(ElectricityMeter.ChannelId.CURRENT_L1,
										new UnsignedDoublewordElement(60)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L1,
										new UnsignedDoublewordElement(62)),
								new DummyRegisterElement(64, 79), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L2,
										new UnsignedDoublewordElement(80),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L2,
										new UnsignedDoublewordElement(82),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L2,
										new UnsignedDoublewordElement(84),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L2,
										new UnsignedDoublewordElement(86),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(88, 99), //
								m(ElectricityMeter.ChannelId.CURRENT_L2,
										new UnsignedDoublewordElement(100)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L2,
										new UnsignedDoublewordElement(102)), //
								new DummyRegisterElement(104, 119), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L3,
										new UnsignedDoublewordElement(120),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L3,
										new UnsignedDoublewordElement(122),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L3,
										new UnsignedDoublewordElement(124),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L3,
										new UnsignedDoublewordElement(126),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(128, 139), //
								m(ElectricityMeter.ChannelId.CURRENT_L3,
										new UnsignedDoublewordElement(140)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L3,
										new UnsignedDoublewordElement(142)) //
						));
				// Calculates required Channels from other existing Channels.
				this.addCalculateChannelListeners();

				return modbusProtocol;
			} else {
				var modbusProtocol = new ModbusProtocol(this, //
						new FC3ReadRegistersTask(0, Priority.HIGH, //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER,
										new UnsignedDoublewordElement(0)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER,
										new UnsignedDoublewordElement(2)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER,
										new UnsignedDoublewordElement(4)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER,
										new UnsignedDoublewordElement(6)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(8, 25), //
								m(ElectricityMeter.ChannelId.FREQUENCY,
										new SignedDoublewordElement(26)
												.wordOrder(LSWMSW)), //
								new DummyRegisterElement(28, 39), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L1,
										new UnsignedDoublewordElement(40)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L1,
										new UnsignedDoublewordElement(42)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L1,
										new UnsignedDoublewordElement(44)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L1,
										new UnsignedDoublewordElement(46)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1)),
						// new DummyRegisterElement(48, 59), //
						new FC3ReadRegistersTask(60, Priority.HIGH, //
								m(ElectricityMeter.ChannelId.CURRENT_L1,
										new UnsignedDoublewordElement(60)
												.wordOrder(LSWMSW)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L1,
										new UnsignedDoublewordElement(62)
												.wordOrder(LSWMSW)),
								new DummyRegisterElement(64, 79), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L2,
										new UnsignedDoublewordElement(80)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L2,
										new UnsignedDoublewordElement(82)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L2,
										new UnsignedDoublewordElement(84)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L2,
										new UnsignedDoublewordElement(86)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(88, 99), //
								m(ElectricityMeter.ChannelId.CURRENT_L2,
										new UnsignedDoublewordElement(100)
												.wordOrder(LSWMSW)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L2,
										new UnsignedDoublewordElement(102)
												.wordOrder(LSWMSW)), //
								new DummyRegisterElement(104, 119), //
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L3,
										new UnsignedDoublewordElement(120)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L3,
										new UnsignedDoublewordElement(122)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L3,
										new UnsignedDoublewordElement(124)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								m(KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L3,
										new UnsignedDoublewordElement(126)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_MINUS_1),
								new DummyRegisterElement(128, 139), //
								m(ElectricityMeter.ChannelId.CURRENT_L3,
										new UnsignedDoublewordElement(140)
												.wordOrder(LSWMSW)), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L3,
										new UnsignedDoublewordElement(142)
												.wordOrder(LSWMSW)) //
						));
				// Calculates required Channels from other existing Channels.
				this.addCalculateChannelListeners();

				return modbusProtocol;
			}
		} else {
			// read via inverter
			if (!this.config.wordwrap()) {
				var modbusProtocol = new ModbusProtocol(this, //
						new FC3ReadRegistersTask(220, Priority.HIGH, //
								m(ElectricityMeter.ChannelId.FREQUENCY,
										new FloatDoublewordElement(220),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L1,
										new FloatDoublewordElement(222),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
										new FloatDoublewordElement(224)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
										new FloatDoublewordElement(226)),
								new DummyRegisterElement(228, 229), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L1,
										new FloatDoublewordElement(230),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L2,
										new FloatDoublewordElement(232),
										SCALE_FACTOR_3),
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
										new FloatDoublewordElement(234)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
										new FloatDoublewordElement(236)),
								new DummyRegisterElement(238, 239), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L2,
										new FloatDoublewordElement(240),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L3,
										new FloatDoublewordElement(242),
										SCALE_FACTOR_3),
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
										new FloatDoublewordElement(244)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
										new FloatDoublewordElement(246)),
								new DummyRegisterElement(248, 249), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L3,
										new FloatDoublewordElement(250),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.ACTIVE_POWER,
										new FloatDoublewordElement(252)), //
								m(ElectricityMeter.ChannelId.REACTIVE_POWER,
										new FloatDoublewordElement(254)) //
						));
				// Calculates required Channels from other existing Channels.
				// this.addCalculateChannelListeners();

				return modbusProtocol;
			} else {
				var modbusProtocol = new ModbusProtocol(this, //
						new FC3ReadRegistersTask(220, Priority.HIGH, //
								m(ElectricityMeter.ChannelId.FREQUENCY,
										new FloatDoublewordElement(220)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L1,
										new FloatDoublewordElement(222)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
										new FloatDoublewordElement(224)
												.wordOrder(LSWMSW)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
										new FloatDoublewordElement(226)
												.wordOrder(LSWMSW)),
								new DummyRegisterElement(228, 229), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L1,
										new FloatDoublewordElement(230)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L2,
										new FloatDoublewordElement(232)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3),
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
										new FloatDoublewordElement(234)
												.wordOrder(LSWMSW)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
										new FloatDoublewordElement(236)
												.wordOrder(LSWMSW)),
								new DummyRegisterElement(238, 239), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L2,
										new FloatDoublewordElement(240)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.CURRENT_L3,
										new FloatDoublewordElement(242)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3),
								m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
										new FloatDoublewordElement(244)
												.wordOrder(LSWMSW)),
								m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
										new FloatDoublewordElement(246)
												.wordOrder(LSWMSW)),
								new DummyRegisterElement(248, 249), //
								m(ElectricityMeter.ChannelId.VOLTAGE_L3,
										new FloatDoublewordElement(250)
												.wordOrder(LSWMSW),
										SCALE_FACTOR_3), //
								m(ElectricityMeter.ChannelId.ACTIVE_POWER,
										new FloatDoublewordElement(252)
												.wordOrder(LSWMSW)), //
								m(ElectricityMeter.ChannelId.REACTIVE_POWER,
										new FloatDoublewordElement(254)
												.wordOrder(LSWMSW)) //
						));
				// Calculates required Channels from other existing Channels.
				// this.addCalculateChannelListeners();

				return modbusProtocol;
			}
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	private void addCalculateChannelListeners() {
		// Active Power
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER,
				ElectricityMeter.ChannelId.ACTIVE_POWER);

		// Phase 1
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L1,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L1,
				ElectricityMeter.ChannelId.ACTIVE_POWER_L1);
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L1,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L1,
				ElectricityMeter.ChannelId.REACTIVE_POWER_L1);

		// Phase 2
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L2,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L2,
				ElectricityMeter.ChannelId.ACTIVE_POWER_L2);
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L2,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L2,
				ElectricityMeter.ChannelId.REACTIVE_POWER_L2);

		// Phase 3
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L3,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L3,
				ElectricityMeter.ChannelId.ACTIVE_POWER_L3);
		CalculatePower.of(this,
				KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L3,
				KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L3,
				ElectricityMeter.ChannelId.REACTIVE_POWER_L3);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				this.calculateEnergy();
				break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable
						.of(KostalGridMeter.class, accessMode, 100).build() //
		);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	private static class CalculatePower implements Consumer<Value<Integer>> {

		private final IntegerReadChannel consChannel;
		private final IntegerReadChannel prodChannel;
		private final IntegerReadChannel targetChannel;

		public static CalculatePower of(KostalGridMeter parent,
				io.openems.edge.common.channel.ChannelId consChannelId,
				io.openems.edge.common.channel.ChannelId prodChannelId,
				io.openems.edge.common.channel.ChannelId targetChannelId) {
			return new CalculatePower(parent, consChannelId, prodChannelId,
					targetChannelId);
		}

		private CalculatePower(KostalGridMeter parent,
				io.openems.edge.common.channel.ChannelId consChannelId,
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
				// result = prodValue.get() - consValue.get();
				result = consValue.get() - prodValue.get();
			} else {
				result = null;
			}
			this.targetChannel.setNextValue(result);
		}
	}
}
