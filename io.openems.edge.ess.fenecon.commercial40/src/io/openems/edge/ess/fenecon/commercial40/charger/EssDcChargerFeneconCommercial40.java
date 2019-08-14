package io.openems.edge.ess.fenecon.commercial40.charger;

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
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40;

/**
 * Implements the FENECON Commercial 40 Charger
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "EssDcCharger.Fenecon.Commercial40", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EssDcChargerFeneconCommercial40 extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(EssDcChargerFeneconCommercial40.class);

	@Reference
	protected ConfigurationAdmin cm;

	public EssDcChargerFeneconCommercial40() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ChannelId.values() //
		);

		/*
		 * Merge PV_DCDC0_INPUT_POWER and PV_DCDC1_INPUT_POWER to ACTUAL_POWER
		 */
		final Channel<Integer> dc0Power = this.channel(ChannelId.PV_DCDC0_INPUT_POWER);
		final Channel<Integer> dc1Power = this.channel(ChannelId.PV_DCDC1_INPUT_POWER);
		final Consumer<Value<Integer>> actualPowerSum = ignore -> {
			this.getActualPower().setNextValue(TypeUtils.sum(dc0Power.value().get(), dc1Power.value().get()));
		};
		dc0Power.onSetNextValue(actualPowerSum);
		dc1Power.onSetNextValue(actualPowerSum);

		/*
		 * Merge PV_DCDC0_OUTPUT_DISCHARGE_ENERGY and PV_DCDC1_OUTPUT_DISCHARGE_ENERGY
		 * to ACTUAL_ENERGY
		 */
		final Channel<Long> dc0Energy = this.channel(ChannelId.PV_DCDC0_OUTPUT_DISCHARGE_ENERGY);
		final Channel<Long> dc1Energy = this.channel(ChannelId.PV_DCDC1_OUTPUT_DISCHARGE_ENERGY);
		final Consumer<Value<Long>> actualEnergySum = ignore -> {
			this.getActualEnergy().setNextValue(TypeUtils.sum(dc0Energy.value().get(), dc1Energy.value().get()));
		};
		dc0Energy.onSetNextValue(actualEnergySum);
		dc1Energy.onSetNextValue(actualEnergySum);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private EssFeneconCommercial40 ess;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm, "Modbus",
				this.ess.getModbusBridgeId());

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		this.ess.setCharger(this);
	}

	@Deactivate
	protected void deactivate() {
		this.ess.setCharger(null);
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT))), //

		// LongReadChannel
		BMS_DCDC0_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //

		// IntegerReadChannel
		BMS_DCDC0_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BMS_DCDC0_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BMS_DCDC0_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC0_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC0_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC0_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC0_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC0_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC1_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BMS_DCDC1_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BMS_DCDC1_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC1_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC1_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC1_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC1_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC1_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC0_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV_DCDC0_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PV_DCDC0_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC0_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC0_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC0_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC0_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC0_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC1_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV_DCDC1_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PV_DCDC1_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC1_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC1_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC1_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC1_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC1_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(0x0503, //
						m(ChannelId.SET_PV_POWER_LIMIT, new UnsignedWordElement(0x0503),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA130, Priority.LOW, //
						m(ChannelId.BMS_DCDC0_OUTPUT_VOLTAGE, new SignedWordElement(0xA130),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_OUTPUT_CURRENT, new SignedWordElement(0xA131),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_OUTPUT_POWER, new SignedWordElement(0xA132),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_INPUT_VOLTAGE, new SignedWordElement(0xA133),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_INPUT_CURRENT, new SignedWordElement(0xA134),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_INPUT_POWER, new SignedWordElement(0xA135),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_INPUT_ENERGY, new SignedWordElement(0xA136),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_OUTPUT_ENERGY, new SignedWordElement(0xA137),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA138, 0xA13F), //
						m(ChannelId.BMS_DCDC0_REACTOR_TEMPERATURE, new SignedWordElement(0xA140)), //
						m(ChannelId.BMS_DCDC0_IGBT_TEMPERATURE, new SignedWordElement(0xA141)), //
						new DummyRegisterElement(0xA142, 0xA14F), //
						m(ChannelId.BMS_DCDC0_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA150).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA152).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA154).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC0_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA156).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA430, Priority.LOW, //
						m(ChannelId.BMS_DCDC1_OUTPUT_VOLTAGE, new SignedWordElement(0xA430),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_OUTPUT_CURRENT, new SignedWordElement(0xA431),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_OUTPUT_POWER, new SignedWordElement(0xA432),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_INPUT_VOLTAGE, new SignedWordElement(0xA433),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_INPUT_CURRENT, new SignedWordElement(0xA434),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_INPUT_POWER, new SignedWordElement(0xA435),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_INPUT_ENERGY, new SignedWordElement(0xA436),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_OUTPUT_ENERGY, new SignedWordElement(0xA437),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA438, 0xA43F), //
						m(ChannelId.BMS_DCDC1_REACTOR_TEMPERATURE, new SignedWordElement(0xA440)), //
						m(ChannelId.BMS_DCDC1_IGBT_TEMPERATURE, new SignedWordElement(0xA441)), //
						new DummyRegisterElement(0xA442, 0xA44F), //
						m(ChannelId.BMS_DCDC1_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA450).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA452).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA454).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.BMS_DCDC1_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA456).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA730, Priority.LOW, //
						m(ChannelId.PV_DCDC0_OUTPUT_VOLTAGE, new SignedWordElement(0xA730),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_OUTPUT_CURRENT, new SignedWordElement(0xA731),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_OUTPUT_POWER, new SignedWordElement(0xA732),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_INPUT_VOLTAGE, new SignedWordElement(0xA733),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_INPUT_CURRENT, new SignedWordElement(0xA734),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_INPUT_POWER, new SignedWordElement(0xA735),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_INPUT_ENERGY, new SignedWordElement(0xA736),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_OUTPUT_ENERGY, new SignedWordElement(0xA737),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA738, 0xA73F), //
						m(ChannelId.PV_DCDC0_REACTOR_TEMPERATURE, new SignedWordElement(0xA740)), //
						m(ChannelId.PV_DCDC0_IGBT_TEMPERATURE, new SignedWordElement(0xA741)), //
						new DummyRegisterElement(0xA742, 0xA74F), //
						m(ChannelId.PV_DCDC0_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA750).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA752).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA754).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC0_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA756).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xAA30, Priority.LOW, //
						m(ChannelId.PV_DCDC1_OUTPUT_VOLTAGE, new SignedWordElement(0xAA30),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_OUTPUT_CURRENT, new SignedWordElement(0xAA31),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_OUTPUT_POWER, new SignedWordElement(0xAA32),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_INPUT_VOLTAGE, new SignedWordElement(0xAA33),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_INPUT_CURRENT, new SignedWordElement(0xAA34),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_INPUT_POWER, new SignedWordElement(0xAA35),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_INPUT_ENERGY, new SignedWordElement(0xAA36),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_OUTPUT_ENERGY, new SignedWordElement(0xAA37),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xAA38, 0xAA3F), //
						m(ChannelId.PV_DCDC1_REACTOR_TEMPERATURE, new SignedWordElement(0xAA40)), //
						m(ChannelId.PV_DCDC1_IGBT_TEMPERATURE, new SignedWordElement(0xAA41)), //
						new DummyRegisterElement(0xAA42, 0xAA4F), //
						m(ChannelId.PV_DCDC1_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA50).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA52).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA54).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.PV_DCDC1_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA56).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2))); //
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().value().asString();
	}
}
