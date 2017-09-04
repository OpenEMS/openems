package io.openems.test.utils.devicenatures;

import java.util.List;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.test.utils.channel.UnitTestReadChannel;

public class UnitTestAsymmetricMeterNature implements AsymmetricMeterNature {

	public UnitTestReadChannel<Long> activePowerL1 = new UnitTestReadChannel<>("ActivePowerL1", this);
	public UnitTestReadChannel<Long> activePowerL2 = new UnitTestReadChannel<>("ActivePowerL2", this);
	public UnitTestReadChannel<Long> activePowerL3 = new UnitTestReadChannel<>("ActivePowerL3", this);
	public UnitTestReadChannel<Long> reactivePowerL1 = new UnitTestReadChannel<>("ReactivePowerL1", this);
	public UnitTestReadChannel<Long> reactivePowerL2 = new UnitTestReadChannel<>("ReactivePowerL2", this);
	public UnitTestReadChannel<Long> reactivePowerL3 = new UnitTestReadChannel<>("ReactivePowerL3", this);
	public UnitTestReadChannel<Long> voltageL1 = new UnitTestReadChannel<>("VoltageL1", this);
	public UnitTestReadChannel<Long> voltageL2 = new UnitTestReadChannel<>("VoltageL2", this);
	public UnitTestReadChannel<Long> voltageL3 = new UnitTestReadChannel<>("VoltageL3", this);
	public UnitTestReadChannel<Long> currentL1 = new UnitTestReadChannel<>("CurrentL1", this);
	public UnitTestReadChannel<Long> currentL2 = new UnitTestReadChannel<>("CurrentL2", this);
	public UnitTestReadChannel<Long> currentL3 = new UnitTestReadChannel<>("CurrentL3", this);

	private final String id;

	public UnitTestAsymmetricMeterNature(String id) {
		super();
		this.id = id;
	}

	@Override
	public ConfigChannel<String> type() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAsRequired(Channel channel) {
		// Not required in Tests
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public ReadChannel<Long> activePowerL1() {
		return activePowerL1;
	}

	@Override
	public ReadChannel<Long> activePowerL2() {
		return activePowerL2;
	}

	@Override
	public ReadChannel<Long> activePowerL3() {
		return activePowerL3;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return voltageL3;
	}

	@Override
	public ConfigChannel<Long> maxActivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConfigChannel<Long> minActivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Device getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BridgeReadTask> getRequiredReadTasks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BridgeReadTask> getReadTasks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		// TODO Auto-generated method stub
		return null;
	}

}
