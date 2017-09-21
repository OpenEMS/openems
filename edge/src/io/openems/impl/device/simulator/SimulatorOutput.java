package io.openems.impl.device.simulator;

import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.io.OutputNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorWriteChannel;

@ThingInfo(title = "Simulator Output")
public class SimulatorOutput extends SimulatorDeviceNature implements OutputNature {

	private SimulatorWriteChannel<Boolean> do1 = new SimulatorWriteChannel<>("DO1", this, false);
	private SimulatorWriteChannel<Boolean> do2 = new SimulatorWriteChannel<>("DO2", this, false);
	private SimulatorWriteChannel<Boolean> do3 = new SimulatorWriteChannel<>("DO3", this, false);
	private SimulatorWriteChannel<Boolean> do4 = new SimulatorWriteChannel<>("DO4", this, false);
	private SimulatorWriteChannel<Boolean> do5 = new SimulatorWriteChannel<>("DO5", this, false);
	private SimulatorWriteChannel<Boolean> do6 = new SimulatorWriteChannel<>("DO6", this, false);
	private SimulatorWriteChannel<Boolean> do7 = new SimulatorWriteChannel<>("DO7", this, false);
	private SimulatorWriteChannel<Boolean> do8 = new SimulatorWriteChannel<>("DO8", this, false);
	private SimulatorWriteChannel<Boolean> do9 = new SimulatorWriteChannel<>("DO9", this, false);
	private SimulatorWriteChannel<Boolean> do10 = new SimulatorWriteChannel<>("DO10", this, false);
	private SimulatorWriteChannel<Boolean>[] array;

	public SimulatorOutput(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		@SuppressWarnings("unchecked") SimulatorWriteChannel<Boolean>[] array = new SimulatorWriteChannel[] { do1, do2,
				do3, do4, do5, do6, do7, do8, do9, do10 };
		this.array = array;
	}

	@Override
	public WriteChannel<Boolean>[] setOutput() {
		return array;
	}

	@Override
	protected void update() {

	}

}
