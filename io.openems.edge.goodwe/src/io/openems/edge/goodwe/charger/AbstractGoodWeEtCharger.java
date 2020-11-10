package io.openems.edge.goodwe.charger;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public abstract class AbstractGoodWeEtCharger extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	protected AbstractGoodWeEtCharger() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				PvChannelId.values() //
		);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		int startAddress = this.getStartAddress();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(startAddress, Priority.HIGH, //
						m(PvChannelId.V, new UnsignedWordElement(startAddress), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PvChannelId.I, new UnsignedWordElement(startAddress + 1),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(startAddress + 2, Priority.HIGH, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedDoublewordElement(startAddress + 2))));
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
		// Calculate Energy
		Integer activePower = this.getActualPower().get();
		if (activePower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
			this.calculateActualEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateActualEnergy.update(activePower);
			this.calculateActualEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateActualEnergy.update(0);
			this.calculateActualEnergy.update(activePower * -1);
		}
	}

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	protected abstract int getStartAddress();
}
