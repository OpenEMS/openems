package io.openems.edge.ess.mr.gridcon.writeutils;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class CcuControlParameters {

	// 32592
	private float uByQDroopMain = 0f;
	private float uByQDroopT1Main = 0f;
	private float fByPDroopMain = 0f;
	private float fByPDroopT1Main = 0f;
	private float qByUDroopMain = 0f;
	private float qByUDeadBand = 0f;
	private float qLimit = 0f;
	private float pByFDroopMain = 0f;
	private float pByFDeadBand = 0f;
	private float pByUDroop = 0f;
	private float pByUDeadBand = 0f;
	private float pByUMaxCharge = 0f;
	private float pByUMaxDischarge = 0f;
	private PControlMode pControlMode = PControlMode.ACTIVE_POWER_CONTROL;
	private float pControlLimTwo = 0f;
	private float pControlLimOne = 0f;

	public CcuControlParameters pControlMode(PControlMode value) {
		this.pControlMode = value;
		return this;
	}

	/**
	 * 0 -> limits Q to zero, 1 -> to max Q.
	 * 
	 * @param value
	 * @return
	 * @throws OpenemsException
	 */
	public CcuControlParameters qLimit(float value) throws OpenemsException {
		if (value < 0 || value > 1) {
			throw new OpenemsException("Q-Limit needs to be within 0 and 1.");
		}
		this.qLimit = value;
		return this;
	}

	public void writeToChannels(GridconPCS parent) throws IllegalArgumentException, OpenemsNamedException {
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN, this.uByQDroopMain);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN, this.uByQDroopT1Main);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN, this.fByPDroopMain);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN, this.fByPDroopT1Main);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN, this.qByUDroopMain);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND, this.qByUDeadBand);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_Q_LIMIT, this.qLimit);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN, this.pByFDroopMain);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND, this.pByFDeadBand);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_U_DROOP, this.pByUDroop);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND, this.pByUDeadBand);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE, this.pByUMaxCharge);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE, this.pByUMaxDischarge);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
				this.pControlMode.getFloatValue()); //
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO, this.pControlLimTwo);
		this.writeValueToChannel(parent, GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE, this.pControlLimOne);
	}

	private <T> void writeValueToChannel(GridconPCS parent, GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) parent.channel(channelId)).setNextWriteValueFromObject(value);
	}
}
