package io.openems.edge.goodwe.charger;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public abstract class AbstractGoodWeEtCharger extends AbstractOpenemsModbusComponent implements GoodWeCharger,
		EssDcCharger, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	protected abstract GoodWe getEssOrBatteryInverter();

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	protected AbstractGoodWeEtCharger() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				GoodWeCharger.ChannelId.values() //
		);
	}

	protected AbstractGoodWeEtCharger(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var startAddress = this.getStartAddress();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(startAddress, Priority.HIGH, //
						m(EssDcCharger.ChannelId.VOLTAGE, new UnsignedWordElement(startAddress), //
								SCALE_FACTOR_2), //
						m(EssDcCharger.ChannelId.CURRENT, new UnsignedWordElement(startAddress + 1), SCALE_FACTOR_2),
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedDoublewordElement(startAddress + 2))));
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			this.updateState();
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

	/**
	 * Updates the 'Has-No-DC-PV' State Channel.
	 */
	private void updateState() {
		var goodWe = this.getEssOrBatteryInverter();
		Boolean hasNoDcPv = null;
		if (goodWe != null) {
			switch (goodWe.getGoodweType().getSeries()) {
			case BT:
				hasNoDcPv = true;
				break;
			case ET:
				hasNoDcPv = false;
				break;
			case UNDEFINED:
				hasNoDcPv = null;
				break;
			}
		}
		this._setHasNoDcPv(hasNoDcPv);
	}

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	protected abstract int getStartAddress();

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GoodWeCharger.class, accessMode, 100) //
						.build());
	}
}
