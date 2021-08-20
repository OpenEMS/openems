package io.openems.edge.goodwe.charger;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusChannelSource;
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

	private final PvChannelId pvChannelId;

	protected AbstractGoodWeEtCharger(PvChannelId[] channelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				channelIds //
		);
		this.pvChannelId = channelIds[0];
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		int startAddress = this.getStartAddress();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(startAddress, Priority.LOW, //
						m(this.pvChannelId.getV(), new UnsignedWordElement(startAddress), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(this.pvChannelId.getI(), new UnsignedWordElement(startAddress + 1),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(startAddress + 2, Priority.HIGH, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedDoublewordElement(startAddress + 2),
								ModbusChannelSource.IGNORE_DUPLICATED_SOURCE)));
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

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	protected abstract int getStartAddress();
}
