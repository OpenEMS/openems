package io.openems.edge.evse.chargepoint.bender;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;
import static io.openems.edge.common.type.Phase.SinglePhase.L2;
import static io.openems.edge.common.type.Phase.SinglePhase.L3;
import static io.openems.edge.meter.api.PhaseRotation.mapLongToPhaseRotatedActivePowerChannel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class AbstractEvseChargePointBender extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, EvseChargePointBender {

	private static final SemanticVersion OUTDATED_VERSION = new SemanticVersion(1, 5, 0);

	protected AbstractEvseChargePointBender(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotated = this.getPhaseRotation();
		var modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(104, Priority.HIGH,
						m(EvseChargePointBender.ChannelId.OCPP_CP_STATUS, new UnsignedWordElement(104))),
				new FC3ReadRegistersTask(111, Priority.LOW, //
						m(new BitsWordElement(111, this)) //
								.bit(0, EvseChargePointBender.ChannelId.ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING) //
								.bit(1, EvseChargePointBender.ChannelId.ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT) //
								.bit(2, EvseChargePointBender.ChannelId.ERR_PIC24) //
								.bit(3, EvseChargePointBender.ChannelId.ERR_USB_STICK_HANDLING) //
								.bit(4, EvseChargePointBender.ChannelId.ERR_INCORRECT_PHASE_INSTALLATION) //
								.bit(5, EvseChargePointBender.ChannelId.ERR_NO_POWER),
						m(new BitsWordElement(112, this)) //
								.bit(0, EvseChargePointBender.ChannelId.ERR_RCMB_TRIGGERED) //
								.bit(1, EvseChargePointBender.ChannelId.ERR_VEHICLE_STATE_E) //
								.bit(2, EvseChargePointBender.ChannelId.ERR_MODE3_DIODE_CHECK) //
								.bit(3, EvseChargePointBender.ChannelId.ERR_MCB_TYPE2_TRIGGERED) //
								.bit(4, EvseChargePointBender.ChannelId.ERR_MCB_SCHUKO_TRIGGERED) //
								.bit(5, EvseChargePointBender.ChannelId.ERR_RCD_TRIGGERED) //
								.bit(6, EvseChargePointBender.ChannelId.ERR_CONTACTOR_WELD) //
								.bit(7, EvseChargePointBender.ChannelId.ERR_BACKEND_DISCONNECTED) //
								.bit(8, EvseChargePointBender.ChannelId.ERR_ACTUATOR_LOCKING_FAILED) //
								.bit(9, EvseChargePointBender.ChannelId.ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED) //
								.bit(10, EvseChargePointBender.ChannelId.ERR_ACTUATOR_STUCK) //
								.bit(11, EvseChargePointBender.ChannelId.ERR_ACTUATOR_DETECTION_FAILED) //
								.bit(12, EvseChargePointBender.ChannelId.ERR_FW_UPDATE_RUNNING) //
								.bit(13, EvseChargePointBender.ChannelId.ERR_TILT) //
								.bit(14, EvseChargePointBender.ChannelId.ERR_WRONG_CP_PR_WIRING) //
								.bit(15, EvseChargePointBender.ChannelId.ERR_TYPE2_OVERLOAD_THR_2)),
				new FC3ReadRegistersTask(122, Priority.HIGH,
						m(EvseChargePointBender.ChannelId.VEHICLE_STATE, new UnsignedWordElement(122))),
				new FC3ReadRegistersTask(131, Priority.LOW,
						m(EvseChargePointBender.ChannelId.SAFE_CURRENT, new UnsignedWordElement(131)),
						new DummyRegisterElement(132, 152),
						m(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MAJOR, new UnsignedWordElement(153)),
						m(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MINOR, new UnsignedWordElement(154)),
						m(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_PATCH, new UnsignedWordElement(155))),
				new FC3ReadRegistersTask(200, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(200))),
				new FC3ReadRegistersTask(206, Priority.HIGH, //
						m(new UnsignedDoublewordElement(206)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L1)), //
						m(new UnsignedDoublewordElement(208)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L2)), //
						m(new UnsignedDoublewordElement(210)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L3)), //
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(212),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(214),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(216),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						new DummyRegisterElement(218, 221),
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(222),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(224),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(226),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(712, Priority.HIGH, //
						m(EvseChargePointBender.ChannelId.MIN_CURRENT, new UnsignedWordElement(712)), //
						new DummyRegisterElement(713, 714), //
						m(EvseChargePointBender.ChannelId.MAX_CURRENT, new UnsignedWordElement(715))));

		return modbusProtocol;
	}

	/**
	 * Calculates if ev is connected from the {@link VehicleState}.
	 * 
	 * @return is the vehicle connected
	 */
	public boolean isEvConnected() {
		var rawState = this.getVehicleState();
		return rawState.isEvConnected();
	}

	/**
	 * Calculates if the cp isReadyForCharging.
	 * 
	 * @return is the vehicle connected
	 */
	public boolean isReadyForCharging() {
		var rawState = this.getVehicleState();
		return rawState.isReadyForCharging();
	}

	/**
	 * Helper Method that handles a Softwareversion check. Sets
	 * {@link EvseChargePoint.ChannelId.FIRMWARE_OUTDATED}
	 *
	 */
	public void updateSoftwareVersionOutdated() {
		setValue(this, EvseChargePointBender.ChannelId.FIRMWARE_OUTDATED, isSoftwareOutdated(//
				this::getSoftwareVersionMajor, //
				this::getSoftwareVersionMinor, //
				this::getSoftwareVersionPatch, //
				this));
	}

	/**
	 * Checks if the Software Version provided by Suppliers is Outdated.
	 *
	 * @param valMajor {@link Supplier} for major Software Version
	 * @param valMinor {@link Supplier} for minor Software Version
	 * @param valPatch {@link Supplier} for patch Software Version
	 * @param cp       the {@link AbstractEvseChargePointBender}
	 * @return is software outdated
	 */
	public static boolean isSoftwareOutdated(Supplier<Value<Integer>> valMajor, Supplier<Value<Integer>> valMinor,
			Supplier<Value<Integer>> valPatch, AbstractEvseChargePointBender cp) {
		final var result = new AtomicBoolean(false);
		valMajor.get().ifPresent(major -> {
			valMinor.get().ifPresent(minor -> {
				valPatch.get().ifPresent(patch -> {
					var version = new SemanticVersion(major, minor, patch);
					setValue(cp, EvseChargePointBender.ChannelId.FIRMWARE_VERSION, version.toString());
					result.set(!version.isAtLeast(OUTDATED_VERSION));
				});
			});
		});
		return result.get();
	}

}
