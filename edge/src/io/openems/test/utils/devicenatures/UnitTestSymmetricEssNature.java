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
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.impl.device.simulator.SimulatorTools;
import io.openems.test.utils.channel.UnitTestConfigChannel;
import io.openems.test.utils.channel.UnitTestReadChannel;
import io.openems.test.utils.channel.UnitTestWriteChannel;

public class UnitTestSymmetricEssNature implements SymmetricEssNature {

	public UnitTestConfigChannel<Integer> minSoc = new UnitTestConfigChannel<>("minSoc", this);
	public UnitTestConfigChannel<Integer> chargeSoc = new UnitTestConfigChannel<>("chargeSoc", this);
	public UnitTestReadChannel<Long> gridMode = new UnitTestReadChannel<Long>("gridMode", this)
			.label(0L, EssNature.OFF_GRID).label(1L, EssNature.ON_GRID);
	public UnitTestReadChannel<Long> soc = new UnitTestReadChannel<>("Soc", this);
	public UnitTestReadChannel<Long> systemState = new UnitTestReadChannel<>("SystemState", this);
	public UnitTestReadChannel<Long> allowedCharge = new UnitTestReadChannel<>("AllowedCharge", this);
	public UnitTestReadChannel<Long> allowedDischarge = new UnitTestReadChannel<>("AllowedDischarge", this);
	public UnitTestReadChannel<Long> allowedApparent = new UnitTestReadChannel<>("AllowedApparent", this);
	public UnitTestReadChannel<Long> activePower = new UnitTestReadChannel<>("ActivePower", this);
	public UnitTestReadChannel<Long> reactivePower = new UnitTestReadChannel<>("ReactivePower", this);
	public UnitTestReadChannel<Long> apparentPower = new UnitTestReadChannel<>("ApparentPower", this);
	public UnitTestReadChannel<Long> maxNominalPower = new UnitTestReadChannel<>("MaxNominalPower", this);
	public UnitTestWriteChannel<Long> setWorkState = new UnitTestWriteChannel<>("SetWorkState", this);
	public UnitTestWriteChannel<Long> setActivePower = new UnitTestWriteChannel<>("SetActivePower", this);
	public UnitTestWriteChannel<Long> setReactivePower = new UnitTestWriteChannel<>("SetReactivePower", this);
	public StaticValueChannel<Long> capacity = new StaticValueChannel<Long>("Capacity", this,
			SimulatorTools.getRandomLong(3000, 50000));
	private final String id;

	public UnitTestSymmetricEssNature(String id) {
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
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower();
	}

	@Override
	public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override
	public WriteChannel<Long> setReactivePower() {
		return setReactivePower;
	}

	@Override
	public ReadChannel<Long> capacity() {
		return capacity;
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
