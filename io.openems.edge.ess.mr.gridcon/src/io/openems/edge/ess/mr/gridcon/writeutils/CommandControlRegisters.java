package io.openems.edge.ess.mr.gridcon.writeutils;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;

public class CommandControlRegisters {

	public static enum Mode {
		CURRENT_CONTROL(true), //
		VOLTAGE_CONTROL(false);

		private final boolean value;

		private Mode(boolean value) {
			this.value = value;
		}
	}

	// 32560
	private boolean disableIpu1 = true;
	private boolean disableIpu2 = true;
	private boolean disableIpu3 = true;
	private boolean disableIpu4 = true;

	// 32561
	private boolean play = false;
	private boolean ready = false;
	private boolean acknowledge = false;
	private boolean stop = false;
	private boolean blackstartApproval = false;
	private boolean syncApproval = false;
	private boolean shortCircuitHandling = false;
	private Mode modeSelection = Mode.VOLTAGE_CONTROL;
	private boolean triggerSia = false;
	private boolean harmonicCompensation = false;
	private boolean parameterSet1 = false;
	private boolean parameterSet2 = false;
	private boolean parameterSet3 = false;
	private boolean parameterSet4 = false;

	// 32562
	private int errorCodeFeedback = 0;
	private float parameterU0 = 0f;
	private float parameterF0 = 0f;
	private final float parameterQref = 0f; // is set in applyPower()
	private final float parameterPref = 0f; // is set in applyPower()

	public CommandControlRegisters play(boolean value) {
		this.play = value;
		return this;
	}

	public CommandControlRegisters ready(boolean value) {
		this.ready = value;
		return this;
	}

	public CommandControlRegisters acknowledge(boolean value) {
		this.acknowledge = value;
		return this;
	}

	public CommandControlRegisters stop(boolean value) {
		this.stop = value;
		return this;
	}

	public CommandControlRegisters blackstartApproval(boolean value) {
		this.blackstartApproval = value;
		return this;
	}

	public CommandControlRegisters syncApproval(boolean value) {
		this.syncApproval = value;
		return this;
	}

	public CommandControlRegisters shortCircuitHandling(boolean value) {
		this.shortCircuitHandling = value;
		return this;
	}

	public CommandControlRegisters modeSelection(Mode value) {
		this.modeSelection = value;
		return this;
	}

	public CommandControlRegisters triggerSia(boolean value) {
		this.triggerSia = value;
		return this;
	}

	public CommandControlRegisters harmonicCompensation(boolean value) {
		this.harmonicCompensation = value;
		return this;
	}

	public CommandControlRegisters parameterSet1(boolean value) {
		this.parameterSet1 = value;
		return this;
	}

	public CommandControlRegisters parameterSet2(boolean value) {
		this.parameterSet2 = value;
		return this;
	}

	public CommandControlRegisters parameterSet3(boolean value) {
		this.parameterSet3 = value;
		return this;
	}

	public CommandControlRegisters parameterSet4(boolean value) {
		this.parameterSet4 = value;
		return this;
	}

	public CommandControlRegisters enableIpus(InverterCount inverterCount, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3 ) {
		switch (inverterCount) {
		case ONE:
			this.disableIpu1 = !enableIPU1;
			this.disableIpu2 = false; // this is DC DC
			this.disableIpu3 = true;
			this.disableIpu4 = true;
			break;
		case TWO:
			this.disableIpu1 = !enableIPU1;
			this.disableIpu2 = !enableIPU2;
			this.disableIpu3 = false; // this is DC DC
			this.disableIpu4 = true;
			break;
		case THREE:
			this.disableIpu1 = !enableIPU1;
			this.disableIpu2 = !enableIPU2;
			this.disableIpu3 = !enableIPU3;
			this.disableIpu4 = false; // this is DC DC
			break;
		}
		return this;
	}

	public CommandControlRegisters disableIpu2(boolean value) {
		this.disableIpu2 = value;
		return this;
	}

	public CommandControlRegisters disableIpu3(boolean value) {
		this.disableIpu3 = value;
		return this;
	}

	public CommandControlRegisters disableIpu4(boolean value) {
		this.disableIpu4 = value;
		return this;
	}

	public CommandControlRegisters errorCodeFeedback(int value) {
		this.errorCodeFeedback = value;
		return this;
	}

	public CommandControlRegisters parameterU0(float value) {
		this.parameterU0 = value;
		return this;
	}

	public CommandControlRegisters parameterF0(float value) {
		this.parameterF0 = value;
		return this;
	}

	public void writeToChannels(GridconPCS parent) throws IllegalArgumentException, OpenemsNamedException {
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_PLAY, this.play);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_READY, this.ready);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE, this.acknowledge);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_STOP, this.stop);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL,
				this.blackstartApproval);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL, this.syncApproval);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING,
				this.shortCircuitHandling);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION,
				this.modeSelection.value);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA, this.triggerSia);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION,
				this.harmonicCompensation);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET,
				this.parameterSet1);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET,
				this.parameterSet2);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET,
				this.parameterSet3);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET,
				this.parameterSet4);

		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_1, this.disableIpu1);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_2, this.disableIpu2);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_3, this.disableIpu3);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_4, this.disableIpu4);

		this.writeValueToChannel(parent, GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK, this.errorCodeFeedback);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, this.parameterU0);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, this.parameterF0);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF, this.parameterQref);
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF, this.parameterPref);

		int date = this.convertToInteger(this.generateDate());
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_TIME_SYNC_DATE, date);
		int time = this.convertToInteger(this.generateTime());
		this.writeValueToChannel(parent, GridConChannelId.COMMAND_TIME_SYNC_TIME, time);
	}

	private <T> void writeValueToChannel(GridconPCS parent, GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) parent.channel(channelId)).setNextWriteValueFromObject(value);
	}

	private BitSet generateDate() {
		LocalDateTime time = LocalDateTime.now();
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000); // 0 == year 2000 in the protocol

		return BitSet.valueOf(new byte[] { day, dayOfWeek, year, month });
	}

	private BitSet generateTime() {
		LocalDateTime time = LocalDateTime.now();
		byte seconds = (byte) time.getSecond();
		byte minutes = (byte) time.getMinute();
		byte hours = (byte) time.getHour();

		// second byte is unused
		return BitSet.valueOf(new byte[] { seconds, 0, hours, minutes });
	}

	private int convertToInteger(BitSet bitSet) {
		long[] l = bitSet.toLongArray();
		if (l.length == 0) {
			return 0;
		}
		return (int) l[0];
	}
}
