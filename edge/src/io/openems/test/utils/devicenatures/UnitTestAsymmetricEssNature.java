package io.openems.test.utils.devicenatures;

import java.util.List;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.impl.device.simulator.SimulatorTools;
import io.openems.test.utils.channel.UnitTestConfigChannel;
import io.openems.test.utils.channel.UnitTestReadChannel;
import io.openems.test.utils.channel.UnitTestWriteChannel;

public class UnitTestAsymmetricEssNature implements AsymmetricEssNature {

	public UnitTestConfigChannel<Integer> minSoc = new UnitTestConfigChannel<>("MinSoc", this);
	public UnitTestConfigChannel<Integer> chargeSoc = new UnitTestConfigChannel<>("ChargeSoc", this);
	public UnitTestReadChannel<Long> gridMode = new UnitTestReadChannel<>("GridMode", this);
	public UnitTestReadChannel<Long> soc = new UnitTestReadChannel<>("Soc", this);
	public UnitTestReadChannel<Long> systemState = new UnitTestReadChannel<>("SystemState", this);
	public UnitTestReadChannel<Long> allowedCharge = new UnitTestReadChannel<>("AllowedCharge", this);
	public UnitTestReadChannel<Long> allowedDischarge = new UnitTestReadChannel<>("AllowedDischarge", this);
	public UnitTestReadChannel<Long> allowedApparent = new UnitTestReadChannel<>("AllowedApparent", this);
	public UnitTestReadChannel<Long> activePowerL1 = new UnitTestReadChannel<>("ActivePowerL1", this);
	public UnitTestReadChannel<Long> activePowerL2 = new UnitTestReadChannel<>("ActivePowerL2", this);
	public UnitTestReadChannel<Long> activePowerL3 = new UnitTestReadChannel<>("ActivePowerL3", this);
	public UnitTestReadChannel<Long> reactivePowerL1 = new UnitTestReadChannel<>("ReactivePowerL1", this);
	public UnitTestReadChannel<Long> reactivePowerL2 = new UnitTestReadChannel<>("ReactivePowerL2", this);
	public UnitTestReadChannel<Long> reactivePowerL3 = new UnitTestReadChannel<>("ReactivePowerL3", this);
	public UnitTestWriteChannel<Long> setActivePowerL1 = new UnitTestWriteChannel<>("SetActivePowerL1", this);
	public UnitTestWriteChannel<Long> setActivePowerL2 = new UnitTestWriteChannel<>("SetActivePowerL2", this);
	public UnitTestWriteChannel<Long> setActivePowerL3 = new UnitTestWriteChannel<>("SetActivePowerL3", this);
	public UnitTestWriteChannel<Long> setReactivePowerL1 = new UnitTestWriteChannel<>("SetReactivePowerL1", this);
	public UnitTestWriteChannel<Long> setReactivePowerL2 = new UnitTestWriteChannel<>("SetReactivePowerL2", this);
	public UnitTestWriteChannel<Long> setReactivePowerL3 = new UnitTestWriteChannel<>("SetReactivePowerL3", this);
	public UnitTestWriteChannel<Long> setWorkState = new UnitTestWriteChannel<>("SetWorkState", this);
	public StaticValueChannel<Long> capacity = new StaticValueChannel<Long>("Capacity", this,
			SimulatorTools.getRandomLong(3000, 50000));
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");

	private final String id;

	public UnitTestAsymmetricEssNature(String id) {
		super();
		this.id = id;
	}

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	@Override
	public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override
	public ReadChannel<Long> soc() {
		return soc;
	}

	@Override
	public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override
	public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override
	public StatusBitChannels warning() {
		return null;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override
	public void setAsRequired(Channel channel) {
		// Not required in Test mode
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
	public WriteChannel<Long> setActivePowerL1() {
		return setActivePowerL1;
	}

	@Override
	public WriteChannel<Long> setActivePowerL2() {
		return setActivePowerL2;
	}

	@Override
	public WriteChannel<Long> setActivePowerL3() {
		return setActivePowerL3;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL1() {
		return setReactivePowerL1;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL2() {
		return setReactivePowerL2;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL3() {
		return setReactivePowerL3;
	}

	@Override
	public ReadChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
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
