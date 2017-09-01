package io.openems.test.utils.devicenatures;

import java.util.List;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.test.utils.channel.UnitTestReadChannel;

public class UnitTestSymmetricMeterNature implements SymmetricMeterNature {

	private final String id;
	public UnitTestReadChannel<Long> activePower = new UnitTestReadChannel<>("ActivePower", this);
	public UnitTestReadChannel<Long> reactivePower = new UnitTestReadChannel<>("ReactivePower", this);
	public UnitTestReadChannel<Long> apparentPower = new UnitTestReadChannel<>("ApparentPower", this);
	public UnitTestReadChannel<Long> frequency = new UnitTestReadChannel<>("frequency", this);
	public UnitTestReadChannel<Long> voltage = new UnitTestReadChannel<>("voltage", this);

	public UnitTestSymmetricMeterNature(String id) {
		this.id = id;
	}

	@Override
	public ConfigChannel<String> type() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAsRequired(Channel channel) {
		// No implementation required
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public ReadChannel<Long> activePower() {
		return activePower;
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
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltage;
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
