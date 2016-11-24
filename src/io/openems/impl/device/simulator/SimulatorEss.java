package io.openems.impl.device.simulator;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

public class SimulatorEss extends SimulatorDeviceNature implements SymmetricEssNature, ChannelUpdateListener {

	public SimulatorEss(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this, Integer.class)
			.updateListener(this);

	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this, Integer.class).optional();

	@Override public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override public void channelUpdated(Channel channel, Optional<?> newValue) {
		// If chargeSoc was not set -> set it to minSoc minus 2
		if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
			chargeSoc.updateValue((Integer) newValue.get() - 2, false);
		}
	}

	/*
	 * Inherited Channels
	 */
	private StatusBitChannels warning = new StatusBitChannels("Warning", this);;
	private SimulatorReadChannel soc = new SimulatorReadChannel("Soc", this).unit("%");
	private SimulatorReadChannel activePower = new SimulatorReadChannel("ActivePower", this);
	private SimulatorReadChannel allowedApparent = new SimulatorReadChannel("AllowedApparent", this);
	private SimulatorReadChannel allowedCharge = new SimulatorReadChannel("AllowedCharge", this);
	private SimulatorReadChannel allowedDischarge = new SimulatorReadChannel("AllowedDischarge", this);
	private SimulatorReadChannel apparentPower = new SimulatorReadChannel("ApparentPower", this);
	private SimulatorReadChannel gridMode = new SimulatorReadChannel("GridMode", this);
	private SimulatorReadChannel reactivePower = new SimulatorReadChannel("ReactivePower", this);
	private SimulatorReadChannel systemState = new SimulatorReadChannel("SystemState", this) //
			.label(1, START).label(2, STOP);
	private ModbusWriteChannel setActivePower = new ModbusWriteChannel("SetActivePower", this);
	private ModbusWriteChannel setReactivePower = new ModbusWriteChannel("SetReactivePower", this);
	private ModbusWriteChannel setWorkState = new ModbusWriteChannel("SetWorkState", this);
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");

	@Override public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override public ReadChannel<Long> soc() {
		return soc;
	}

	@Override public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override public WriteChannel<Long> setReactivePower() {
		return setReactivePower;
	}

	@Override public StatusBitChannels warning() {
		return warning;
	}

	@Override public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	@Override protected void update() {
		soc.updateValue(getRandom(0, 100));
		activePower.updateValue(getRandom(-10000, 10000));
		reactivePower.updateValue(getRandom(-1800, 1800));
		allowedCharge.updateValue(9000L);
		allowedDischarge.updateValue(3000L);
		systemState.updateValue(1L);
	}

	private long getRandom(int min, int max) {
		return ThreadLocalRandom.current().nextLong(min, max + 1);
	}

	@Override public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}
}
