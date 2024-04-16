package io.openems.edge.deye.common.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

public abstract class AbstractEssDeyePv extends AbstractOpenemsModbusComponent implements DeyeSunPv, EssDcCharger,
		ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	/**
	 * Is this PV1 or PV2 charger?.
	 *
	 * @return true for PV1, false for PV2
	 */
	protected abstract boolean isPV1();

	public AbstractEssDeyePv() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				DeyeSunPv.ChannelId.values() //
		);

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var protocol = new ModbusProtocol(this, //
				new FC16WriteRegistersTask(0x0503, //
						m(DeyeSunPv.ChannelId.SET_PV_POWER_LIMIT, new UnsignedWordElement(0x0503), SCALE_FACTOR_2))); //

		if (this.isPV1()) {
			protocol.addTasks(//
					new FC3ReadRegistersTask(0xA130, Priority.LOW, //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_VOLTAGE, new SignedWordElement(0xA130),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_CURRENT, new SignedWordElement(0xA131),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_POWER, new SignedWordElement(0xA132), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_VOLTAGE, new SignedWordElement(0xA133),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_CURRENT, new SignedWordElement(0xA134),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_POWER, new SignedWordElement(0xA135), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_ENERGY, new SignedWordElement(0xA136), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_ENERGY, new SignedWordElement(0xA137),
									SCALE_FACTOR_2), //
							new DummyRegisterElement(0xA138, 0xA13F), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_REACTOR_TEMPERATURE, new SignedWordElement(0xA140)), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_IGBT_TEMPERATURE, new SignedWordElement(0xA141)), //
							new DummyRegisterElement(0xA142, 0xA14F), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA150).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA152).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA154).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA156).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2)), //

					new FC3ReadRegistersTask(0xA730, Priority.LOW, //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_VOLTAGE, new SignedWordElement(0xA730),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_CURRENT, new SignedWordElement(0xA731),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_POWER, new SignedWordElement(0xA732), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_VOLTAGE, new SignedWordElement(0xA733), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_CURRENT, new SignedWordElement(0xA734), SCALE_FACTOR_2), //
							m(EssDcCharger.ChannelId.ACTUAL_POWER, new SignedWordElement(0xA735), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_ENERGY, new SignedWordElement(0xA736), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_ENERGY, new SignedWordElement(0xA737), SCALE_FACTOR_2), //
							new DummyRegisterElement(0xA738, 0xA73F), //
							m(DeyeSunPv.ChannelId.PV_DCDC_REACTOR_TEMPERATURE, new SignedWordElement(0xA740)), //
							m(DeyeSunPv.ChannelId.PV_DCDC_IGBT_TEMPERATURE, new SignedWordElement(0xA741)), //
							new DummyRegisterElement(0xA742, 0xA74F), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA750).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA752).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA754).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA756).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2)));

		} else {
			protocol.addTasks(//
					new FC3ReadRegistersTask(0xAA30, Priority.LOW, //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_VOLTAGE, new SignedWordElement(0xAA30),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_CURRENT, new SignedWordElement(0xAA31),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_POWER, new SignedWordElement(0xAA32), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_VOLTAGE, new SignedWordElement(0xAA33), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_CURRENT, new SignedWordElement(0xAA34), SCALE_FACTOR_2), //
							m(EssDcCharger.ChannelId.ACTUAL_POWER, new SignedWordElement(0xAA35), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_ENERGY, new SignedWordElement(0xAA36), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_ENERGY, new SignedWordElement(0xAA37), SCALE_FACTOR_2), //
							new DummyRegisterElement(0xAA38, 0xAA3F), //
							m(DeyeSunPv.ChannelId.PV_DCDC_REACTOR_TEMPERATURE, new SignedWordElement(0xAA40)), //
							m(DeyeSunPv.ChannelId.PV_DCDC_IGBT_TEMPERATURE, new SignedWordElement(0xAA41)), //
							new DummyRegisterElement(0xAA42, 0xAA4F), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xAA50).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_INPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xAA52).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xAA54).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.PV_DCDC_OUTPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xAA56).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2)),
					new FC3ReadRegistersTask(0xA430, Priority.LOW, //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_VOLTAGE, new SignedWordElement(0xA430),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_CURRENT, new SignedWordElement(0xA431),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_POWER, new SignedWordElement(0xA432), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_VOLTAGE, new SignedWordElement(0xA433),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_CURRENT, new SignedWordElement(0xA434),
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_POWER, new SignedWordElement(0xA435), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_ENERGY, new SignedWordElement(0xA436), SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_ENERGY, new SignedWordElement(0xA437),
									SCALE_FACTOR_2), //
							new DummyRegisterElement(0xA438, 0xA43F), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_REACTOR_TEMPERATURE, new SignedWordElement(0xA440)), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_IGBT_TEMPERATURE, new SignedWordElement(0xA441)), //
							new DummyRegisterElement(0xA442, 0xA44F), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA450).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_INPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA452).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_CHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA454).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2), //
							m(DeyeSunPv.ChannelId.BMS_DCDC_OUTPUT_DISCHARGE_ENERGY,
									new UnsignedDoublewordElement(0xA456).wordOrder(WordOrder.LSWMSW), //
									SCALE_FACTOR_2))); //
		}
		return protocol;
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		var actualPower = this.getActualPower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(DeyeSunPv.class, accessMode, 100) //
						.build());
	}
}
