package io.openems.edge.ess.mr.gridcon.writeutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.Config;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

public class DcdcControl {

	private final Logger log = LoggerFactory.getLogger(DcdcControl.class);

	private float dcVoltageSetpoint = 0f;
	private float weightStringA = 0f; // is set in applyPower()
	private float weightStringB = 0f; // is set in applyPower()
	private float weightStringC = 0f; // is set in applyPower()
	private float iRefStringA = 0f;
	private float iRefStringB = 0f;
	private float iRefStringC = 0f;
	private float stringControlMode = 0f;

	public DcdcControl dcVoltageSetpoint(float value) {
		this.dcVoltageSetpoint = value;
		return this;
	}

	public DcdcControl iRefStringA(float value) {
		this.iRefStringA = value;
		return this;
	}

	public DcdcControl iRefStringB(float value) {
		this.iRefStringB = value;
		return this;
	}

	public DcdcControl iRefStringC(float value) {
		this.iRefStringC = value;
		return this;
	}

	public DcdcControl weightStringA(float value) {
		this.weightStringA = value;
		return this;
	}

	public DcdcControl weightStringB(float value) {
		this.weightStringB = value;
		return this;
	}

	public DcdcControl weightStringC(float value) {
		this.weightStringC = value;
		return this;
	}

	public DcdcControl stringControlMode(ComponentManager componentManager, Config config)
			throws OpenemsNamedException {
		int weightingMode = 0; // Depends on number of battery strings!!!

		// --- // TODO if battery is not ready for work, remove it from the weighting
		// mode!!

		boolean useBatteryStringA = this.useBatteryString(config.batteryStringA_id(), componentManager);
		if (useBatteryStringA) {
			weightingMode += 1; // battA = 1 (2^0)
		}
		boolean useBatteryStringB = this.useBatteryString(config.batteryStringB_id(), componentManager);
		if (useBatteryStringB) {
			weightingMode += 8; // battB = 8 (2^3)
		}
		boolean useBatteryStringC = this.useBatteryString(config.batteryStringC_id(), componentManager);
		if (useBatteryStringC) {
			weightingMode += 64; // battC = 64 (2^6)
		}

		this.log.info("Weighting: " + weightingMode //
				+ " A:" + (useBatteryStringA ? "x" : "-") //
				+ " B:" + (useBatteryStringB ? "x" : "-") //
				+ " C:" + (useBatteryStringC ? "x" : "-"));

		this.stringControlMode = weightingMode;

		return this;
	}

	/**
	 * Returns true if this battery string should be used for weighting.
	 * 
	 * @param batteryStringId
	 * @param componentManager
	 * @return
	 * @throws OpenemsNamedException
	 */
	private boolean useBatteryString(String batteryStringId, ComponentManager componentManager)
			throws OpenemsNamedException {
		// If the battery is connected and switched on and ready for working it can be
		// considered for weighting

		// TODO MR implementation is obsolete; commented this block after removing
		// battery READY_FOR_WORKING Channel

//		if (batteryStringId != null && batteryStringId.length() > 0) {
//			Battery battery = componentManager.getComponent(batteryStringId);
//			if (battery != null) {
//				Optional<Boolean> batteryReady = battery.getReadyForWorking().asOptional();
//				if (batteryReady.isPresent() && batteryReady.get()) {
//					return true;
//				}
//			}
//		}
		return false;
	}

	public void writeToChannels(GridconPCS parent) throws IllegalArgumentException, OpenemsNamedException {
//		 weighting is never allowed to be '0'
		if (this.stringControlMode == 0) {
			throw new OpenemsException("Calculated weight of '0' -> not allowed!");
		}

		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT, this.dcVoltageSetpoint);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, this.weightStringA);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, this.weightStringB);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, this.weightStringC);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_A, this.iRefStringA);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_B, this.iRefStringB);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_C, this.iRefStringC);
		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE, this.stringControlMode);
	}

	private <T> void writeValueToChannel(GridconPCS parent, GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) parent.channel(channelId)).setNextWriteValueFromObject(value);
	}
}
