package io.openems.edge.io.gpio.hardware;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.gpio.api.AbstractGpioChannel;
import io.openems.edge.io.gpio.api.WriteChannelId;
import io.openems.edge.io.gpio.linuxfs.Gpio;
import io.openems.edge.io.gpio.linuxfs.HardwareFactory;
import io.openems.edge.io.gpio.linuxfs.LinuxFsDigitalOut;

public abstract class ModBerryX500CM4 implements HardwarePlatform {

	private HardwareFactory context;
	private HashMap<Integer, Gpio> ios;

	public ModBerryX500CM4(HardwareFactory context) {
		this.context = context;
		this.ios = new HashMap<>();
	}

	@Override
	public void createPinObjects(List<ChannelId> channelIds) {
		this.getReadChannelIds().stream().forEach(readChannel -> {
			this.ios.put(readChannel.gpio, this.context.fabricateIn(readChannel.gpio));
		});

		this.getWriteChannelIds().stream().forEach(writeChannel -> {
			this.ios.put(writeChannel.gpio, this.context.fabricateOut(writeChannel.gpio));
		});
	}

	@Override
	public Optional<Boolean> getGpioValueByChannelId(AbstractGpioChannel channelId) {
		return this.ios.get(channelId.gpio).getValue();
	}

	@Override
	public void setGpio(WriteChannelId channelId, boolean value) throws OpenemsException {
		((LinuxFsDigitalOut) this.ios.get(channelId.gpio)).setValue(value);
	}
}
