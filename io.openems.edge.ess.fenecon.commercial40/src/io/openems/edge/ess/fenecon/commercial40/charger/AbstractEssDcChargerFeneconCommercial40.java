package io.openems.edge.ess.fenecon.commercial40.charger;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
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
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public abstract class AbstractEssDcChargerFeneconCommercial40 extends AbstractOpenemsModbusComponent
		implements EssDcChargerFeneconCommercial40, EssDcCharger, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	/**
	 * Is this PV1 or PV2 charger?
	 * 
	 * @return true for PV1, false for PV2
	 */
	protected abstract boolean isPV1();

	public AbstractEssDcChargerFeneconCommercial40() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				EssDcChargerFeneconCommercial40.ChannelId.values() //
		);

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(0x0503, //
						m(EssDcChargerFeneconCommercial40.ChannelId.SET_PV_POWER_LIMIT, new UnsignedWordElement(0x0503),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA130, Priority.LOW, //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_VOLTAGE,
								new SignedWordElement(0xA130), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_CURRENT,
								new SignedWordElement(0xA131), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_POWER,
								new SignedWordElement(0xA132), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_VOLTAGE,
								new SignedWordElement(0xA133), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_CURRENT,
								new SignedWordElement(0xA134), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_POWER,
								new SignedWordElement(0xA135), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_ENERGY,
								new SignedWordElement(0xA136), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_ENERGY,
								new SignedWordElement(0xA137), ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA138, 0xA13F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_REACTOR_TEMPERATURE,
								new SignedWordElement(0xA140)), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_IGBT_TEMPERATURE,
								new SignedWordElement(0xA141)), //
						new DummyRegisterElement(0xA142, 0xA14F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA150).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA152).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA154).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC0_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA156).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA430, Priority.LOW, //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_VOLTAGE,
								new SignedWordElement(0xA430), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_CURRENT,
								new SignedWordElement(0xA431), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_POWER,
								new SignedWordElement(0xA432), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_VOLTAGE,
								new SignedWordElement(0xA433), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_CURRENT,
								new SignedWordElement(0xA434), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_POWER,
								new SignedWordElement(0xA435), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_ENERGY,
								new SignedWordElement(0xA436), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_ENERGY,
								new SignedWordElement(0xA437), ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA438, 0xA43F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_REACTOR_TEMPERATURE,
								new SignedWordElement(0xA440)), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_IGBT_TEMPERATURE,
								new SignedWordElement(0xA441)), //
						new DummyRegisterElement(0xA442, 0xA44F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA450).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA452).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA454).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.BMS_DCDC1_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA456).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA730, Priority.LOW, //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_VOLTAGE,
								new SignedWordElement(0xA730), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_CURRENT,
								new SignedWordElement(0xA731), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_POWER,
								new SignedWordElement(0xA732), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_VOLTAGE,
								new SignedWordElement(0xA733), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_CURRENT,
								new SignedWordElement(0xA734), ElementToChannelConverter.SCALE_FACTOR_2), //

						(this.isPV1() ? //
								m(EssDcCharger.ChannelId.ACTUAL_POWER, new SignedWordElement(0xA735),
										ElementToChannelConverter.SCALE_FACTOR_2) //
								: //
								new DummyRegisterElement(0xA735)),

						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_ENERGY,
								new SignedWordElement(0xA736), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_ENERGY,
								new SignedWordElement(0xA737), ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xA738, 0xA73F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_REACTOR_TEMPERATURE,
								new SignedWordElement(0xA740)), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_IGBT_TEMPERATURE,
								new SignedWordElement(0xA741)), //
						new DummyRegisterElement(0xA742, 0xA74F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA750).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA752).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA754).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xA756).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xAA30, Priority.LOW, //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_VOLTAGE,
								new SignedWordElement(0xAA30), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_CURRENT,
								new SignedWordElement(0xAA31), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_POWER,
								new SignedWordElement(0xAA32), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_VOLTAGE,
								new SignedWordElement(0xAA33), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_CURRENT,
								new SignedWordElement(0xAA34), ElementToChannelConverter.SCALE_FACTOR_2), //

						(!this.isPV1() ? //
								m(EssDcCharger.ChannelId.ACTUAL_POWER, new SignedWordElement(0xAA35),
										ElementToChannelConverter.SCALE_FACTOR_2) //
								: //
								new DummyRegisterElement(0xAA35)),

						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_ENERGY,
								new SignedWordElement(0xAA36), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_ENERGY,
								new SignedWordElement(0xAA37), ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0xAA38, 0xAA3F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_REACTOR_TEMPERATURE,
								new SignedWordElement(0xAA40)), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_IGBT_TEMPERATURE,
								new SignedWordElement(0xAA41)), //
						new DummyRegisterElement(0xAA42, 0xAA4F), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA50).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA52).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA54).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_OUTPUT_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0xAA56).wordOrder(WordOrder.LSWMSW), //
								ElementToChannelConverter.SCALE_FACTOR_2))); //
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
		Integer actualPower = this.getActualPower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}
}
