package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.Helper;
import io.openems.edge.ess.mr.gridcon.WeightingHelper;

public abstract class BaseState implements GridconStateObject {

	private ComponentManager manager;
	protected String gridconPcsId;
	protected String battery1Id;
	protected String battery2Id;
	protected String battery3Id;
	protected String hardRestartRelayAdress;

	public BaseState(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id,
			String hardRestartRelayAdress) {
		this.manager = manager;
		this.gridconPcsId = gridconPcsId;
		this.battery1Id = b1Id;
		this.battery2Id = b2Id;
		this.battery3Id = b3Id;
		this.hardRestartRelayAdress = hardRestartRelayAdress;
	}

	protected boolean isNextStateUndefined() {
		if (!this.getGridconPcs().isCommunicationBroken() && !this.isGridconDefined()) {
			System.out.println("Gridcon is undefined!");
			return true;
		}
		if (!this.isAtLeastOneBatteryDefined()) {
			System.out.println("All Batteries are undefined!");
			return true;
		}
		return false;
	}

	private boolean isAtLeastOneBatteryDefined() {
		boolean undefined = true;

		if (this.getBattery1() != null) {
			undefined = undefined && Helper.isUndefined(this.getBattery1());
		}
		if (this.getBattery2() != null) {
			undefined = undefined && Helper.isUndefined(this.getBattery2());
		}
		if (this.getBattery3() != null) {
			undefined = undefined && Helper.isUndefined(this.getBattery3());
		}

		return !undefined;
	}

	private boolean isGridconDefined() {
		boolean defined = false;

		if (this.getGridconPcs() != null) {
			defined = !this.getGridconPcs().isUndefined();
		}

		return defined;
	}

	protected boolean isNextStateError() {
		if (this.getGridconPcs() != null
				&& (this.getGridconPcs().isError() || this.getGridconPcs().isCommunicationBroken())) {
			return true;
		}

		if (this.getBattery1() != null && this.getBattery1().hasFaults()) {
			return true;
		}

		if (this.getBattery2() != null && this.getBattery2().hasFaults()) {
			return true;
		}

		if (this.getBattery3() != null && this.getBattery3().hasFaults()) {
			return true;
		}

		return false;
	}

	protected boolean isNextStateStopped() {
		return this.getGridconPcs() != null && this.getGridconPcs().isStopped();
	}

	protected boolean isNextStateRunning() {
		return (this.getGridconPcs() != null && this.getGridconPcs().isRunning());
	}

	protected void startBatteries() {
		if (this.getBattery1() != null) {
			if (!this.getBattery1().isStarted()) {
				try {
					this.getBattery1().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery1().id() + "!\n" + e.getMessage());
				}
			}
		}
		if (this.getBattery2() != null) {
			if (!this.getBattery2().isStarted()) {
				try {
					this.getBattery2().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery2().id() + "!\n" + e.getMessage());
				}
			}
		}
		if (this.getBattery3() != null) {
			if (!this.getBattery3().isStarted()) {
				try {
					this.getBattery3().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery3().id() + "!\n" + e.getMessage());
				}
			}
		}
	}

	protected boolean isBatteriesStarted() {
		boolean running = true;
		if (this.getBattery1() != null) {
			running = running && this.getBattery1().isStarted();
		}
		if (this.getBattery2() != null) {
			running = running && this.getBattery2().isStarted();
		}
		if (this.getBattery3() != null) {
			running = running && this.getBattery3().isStarted();
		}
		return running;
	}

	protected void setStringControlMode() {
		int weightingMode = WeightingHelper.getStringControlMode(this.getBattery1(), this.getBattery2(),
				this.getBattery3());
		this.getGridconPcs().setStringControlMode(weightingMode);
	}

	protected void setStringWeighting() {
		float activePower = this.getGridconPcs().getActivePowerPreset();

		Float[] weightings = WeightingHelper.getWeighting(activePower, this.getBattery1(), this.getBattery2(),
				this.getBattery3());

		this.getGridconPcs().setWeightStringA(weightings[0]);
		this.getGridconPcs().setWeightStringB(weightings[1]);
		this.getGridconPcs().setWeightStringC(weightings[2]);
	}

	protected void setDateAndTime() {
		int date = this.convertToInteger(this.generateDate(LocalDateTime.now()));
		this.getGridconPcs().setSyncDate(date);
		int time = this.convertToInteger(this.generateTime(LocalDateTime.now()));
		this.getGridconPcs().setSyncTime(time);
	}

	private BitSet generateDate(LocalDateTime time) {
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000); // 0 == year 2000 in the protocol

		return BitSet.valueOf(new byte[] { day, dayOfWeek, year, month });
	}

	private BitSet generateTime(LocalDateTime time) {
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

	GridconPcs getGridconPcs() {
		return this.getComponent(this.gridconPcsId);
	}

	protected void setHardRestartRelay(boolean val) {
		try {
			ChannelAddress address = ChannelAddress.fromString(this.hardRestartRelayAdress);
			BooleanWriteChannel outputHardResetChannel = this.manager.getChannel(address);
			outputHardResetChannel.setNextWriteValue(val);
		} catch (OpenemsNamedException e) {
			System.out.println("Failed to set the hard reset");
		}
	}

	Battery getBattery1() {
		return this.getComponent(this.battery1Id);
	}

	Battery getBattery2() {
		return this.getComponent(this.battery2Id);
	}

	Battery getBattery3() {
		return this.getComponent(this.battery3Id);
	}

	<T> T getComponent(String id) {
		T component = null;
		try {
			component = this.manager.getComponent(id);
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return component;
	}

}
