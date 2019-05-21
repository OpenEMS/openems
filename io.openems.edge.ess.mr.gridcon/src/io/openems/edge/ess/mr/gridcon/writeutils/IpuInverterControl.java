package io.openems.edge.ess.mr.gridcon.writeutils;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

public class IpuInverterControl {

	public enum Inverter {
		ONE, //
		TWO, //
		THREE;
	}

	// 32592
	private float dcVoltageSetpoint = 0f;
	private float dcCurrentSetpoint = 0f;
	private float u0OffsetToCcu = 0f;
	private float f0OffsetToCcu = 0f;
	private float qRefOffsetToCcu = 0f;
	private float pRefOffsetToCcu = 0f;
	private float pMaxDischarge = 0f;
	private float pMaxCharge = 0f;

	public IpuInverterControl pMaxDischarge(float value) {
		this.pMaxDischarge = value;
		return this;
	}

	public IpuInverterControl pMaxCharge(float value) {
		this.pMaxCharge = value;
		return this;
	}

	public IpuInverterControl writeToChannels(GridconPCS parent, Inverter inverter)
			throws IllegalArgumentException, OpenemsNamedException {
		switch (inverter) {
		case ONE:
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
					this.dcVoltageSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
					this.dcCurrentSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
					this.u0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
					this.f0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
					this.qRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
					this.pRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE, this.pMaxDischarge);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE, this.pMaxCharge);
			break;

		case TWO:
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
					this.dcVoltageSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
					this.dcCurrentSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
					this.u0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
					this.f0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
					this.qRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
					this.pRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE, this.pMaxDischarge);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE, this.pMaxCharge);
			break;

		case THREE:
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
					this.dcVoltageSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
					this.dcCurrentSetpoint);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
					this.u0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
					this.f0OffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
					this.qRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
					this.pRefOffsetToCcu);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE, this.pMaxDischarge);
			this.writeValueToChannel(parent, GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE, this.pMaxCharge);
			break;
		}
		return this;
	}

	private <T> void writeValueToChannel(GridconPCS parent, GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) parent.channel(channelId)).setNextWriteValueFromObject(value);
	}
}
