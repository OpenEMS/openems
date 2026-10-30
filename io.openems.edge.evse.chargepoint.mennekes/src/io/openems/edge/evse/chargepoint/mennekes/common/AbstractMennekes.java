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

	private static final int LEGACY_PROFESSIONAL_PHASE_POWER_PLACEHOLDER = Integer.MAX_VALUE;

	private boolean mennekesSpecificTasksConfigured = false;
	private final FC16WriteRegistersTask setPowerLimitTask = new FC16WriteRegistersTask(2002,
			m(Mennekes.ChannelId.SET_POWER_LIMIT, new UnsignedWordElement(2002)));
	private final FC3ReadRegistersTask hemsPowerTask = new FC3ReadRegistersTask(2012, Priority.HIGH,
			m(Mennekes.ChannelId.HEMS_MIN_POWER, new UnsignedWordElement(2012)),
			m(Mennekes.ChannelId.HEMS_MAX_POWER, new UnsignedWordElement(2013)));
	private final FC3ReadRegistersTask phaseSwitchTask = new FC3ReadRegistersTask(2020, Priority.HIGH,
			m(Mennekes.ChannelId.PHASE_SWITCH_MODE, new UnsignedWordElement(2020)),
			m(Mennekes.ChannelId.PHASE_SWITCH_PAUSE, new UnsignedWordElement(2021)),
			m(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, new UnsignedWordElement(2022)));

	protected AbstractMennekes(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.addLegacyProfessionalTotalPowerFallback();
	}

	private void addLegacyProfessionalTotalPowerFallback() {
		this.getActivePowerL1Channel().onSetNextValue(ignore -> this.normalizeLegacyProfessionalPhasePowerL1());
		this.getActivePowerL2Channel().onSetNextValue(ignore -> this.normalizeLegacyProfessionalPhasePowerL2());
		this.getActivePowerL3Channel().onSetNextValue(ignore -> this.normalizeLegacyProfessionalPhasePowerL3());
	}

	private void normalizeLegacyProfessionalPhasePowerL1() {
		this.normalizeLegacyProfessionalPhasePower(ElectricityMeter.ChannelId.ACTIVE_POWER_L1);
	}

	private void normalizeLegacyProfessionalPhasePowerL2() {
		this.normalizeLegacyProfessionalPhasePower(ElectricityMeter.ChannelId.ACTIVE_POWER_L2);
	}

	private void normalizeLegacyProfessionalPhasePowerL3() {
		this.normalizeLegacyProfessionalPhasePower(ElectricityMeter.ChannelId.ACTIVE_POWER_L3);
	}

	private void normalizeLegacyProfessionalPhasePower(ElectricityMeter.ChannelId channelId) {
		if (this.getMennekesDeviceId() != DeviceID.PROFESSIONAL) {
			return;
		}
		final Integer value = switch (channelId) {
		case ACTIVE_POWER_L1 -> this.getActivePowerL1Channel().getNextValue().get();
		case ACTIVE_POWER_L2 -> this.getActivePowerL2Channel().getNextValue().get();
		case ACTIVE_POWER_L3 -> this.getActivePowerL3Channel().getNextValue().get();
		default -> null;
		};
		if (value != null && value == LEGACY_PROFESSIONAL_PHASE_POWER_PLACEHOLDER) {
			setValue(this, channelId, 0);
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = super.defineModbusProtocol();
		modbusProtocol.addTask(//
				new FC3ReadRegistersTask(1000, Priority.LOW,
						m(Mennekes.ChannelId.EMS_CURRENT_LIMIT, new UnsignedWordElement(1000))));

		if (!this.isReadOnly()) {
			modbusProtocol.addTask(new FC16WriteRegistersTask(1000,
					m(Mennekes.ChannelId.SET_CURRENT_LIMIT, new UnsignedWordElement(1000))));
		}

		return modbusProtocol;
	}

	@Override
	protected void onDeviceIdUpdate(Value<Integer> deviceId) {
		final var resolvedDeviceId = this.resolveDeviceId(deviceId.orElse(null));
		setValue(this, Mennekes.ChannelId.DEVICE_ID, resolvedDeviceId);
		this.configureMennekesSpecificTasks(resolvedDeviceId);
	}

	private void configureMennekesSpecificTasks(DeviceID deviceId) {
		if (deviceId == null || deviceId == DeviceID.UNDEFINED || this.isReadOnly()) {
			return;
		}

		if (deviceId == DeviceID.PROFESSIONAL) {
			this.removeMennekesSpecificTasks();
			return;
		}
		this.addMennekesSpecificTasks();
	}

	private void addMennekesSpecificTasks() {
		if (this.mennekesSpecificTasksConfigured) {
			return;
		}
		this.getModbusProtocol().addTasks(this.setPowerLimitTask, this.hemsPowerTask, this.phaseSwitchTask);
		this.mennekesSpecificTasksConfigured = true;
	}

	private void removeMennekesSpecificTasks() {
		if (!this.mennekesSpecificTasksConfigured) {
			return;
		}
		this.getModbusProtocol().removeTask(this.setPowerLimitTask);
		this.getModbusProtocol().removeTask(this.hemsPowerTask);
		this.getModbusProtocol().removeTask(this.phaseSwitchTask);
		this.mennekesSpecificTasksConfigured = false;
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
