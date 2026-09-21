package io.openems.edge.evse.chargepoint.mennekes.common;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.common.types.OptionsEnum;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.chargepoint.bender.AbstractEvseChargePointBender;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class AbstractMennekes extends AbstractEvseChargePointBender implements Mennekes, ElectricityMeter {

	protected AbstractMennekes(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = super.defineModbusProtocol();
		modbusProtocol.addTask(//
				new FC3ReadRegistersTask(1000, Priority.LOW,
						m(Mennekes.ChannelId.EMS_CURRENT_LIMIT, new UnsignedWordElement(1000))));

		if (!this.isReadOnly()) {
			modbusProtocol.addTasks(
					new FC16WriteRegistersTask(1000,
							m(Mennekes.ChannelId.SET_CURRENT_LIMIT, new UnsignedWordElement(1000))),
					new FC16WriteRegistersTask(2002,
							m(Mennekes.ChannelId.SET_POWER_LIMIT, new UnsignedWordElement(2002))),
					new FC3ReadRegistersTask(2012, Priority.HIGH,
							m(Mennekes.ChannelId.HEMS_MIN_POWER, new UnsignedWordElement(2012)),
							m(Mennekes.ChannelId.HEMS_MAX_POWER, new UnsignedWordElement(2013))),
					new FC3ReadRegistersTask(2020, Priority.HIGH,
							m(Mennekes.ChannelId.PHASE_SWITCH_MODE, new UnsignedWordElement(2020)),
							m(Mennekes.ChannelId.PHASE_SWITCH_PAUSE, new UnsignedWordElement(2021)),
							m(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, new UnsignedWordElement(2022))));
		}

		return modbusProtocol;
	}

	@Override
	protected void onDeviceIdUpdate(Value<Integer> deviceId) {
		setValue(this, Mennekes.ChannelId.DEVICE_ID, this.resolveDeviceId(deviceId.orElse(null)));
	}

	@Override
	public SemanticVersion getOutdatedVersion() {
		final DeviceID deviceId = this.getMennekesDeviceId();
		if (deviceId == null || deviceId == DeviceID.UNDEFINED) {
			return SemanticVersion.ZERO;
		}
		return deviceId.minVersion;
	}

	private DeviceID resolveDeviceId(Integer deviceId) {
		return deviceId == null //
				? DeviceID.UNDEFINED
				: OptionsEnum.getOptionOrUndefined(DeviceID.class, deviceId);
	}

	/**
	 * Is the chargePoint readOnly.
	 * 
	 * @return config readOnly value
	 */
	public abstract boolean isReadOnly();
}
