package io.openems.edge.io.gpio.hardware;

import java.util.List;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.gpio.api.ReadChannelId;
import io.openems.edge.io.gpio.api.WriteChannelId;
import io.openems.edge.io.gpio.linuxfs.HardwareFactory;

public final class ModberryX500M40804Max extends ModBerryX500CM4 {

	private final List<ChannelId> channelIds = List.of(//
			new ReadChannelId(18, "DIGITAL_INPUT_1"), //
			new ReadChannelId(19, "DIGITAL_INPUT_2"), //
			new ReadChannelId(20, "DIGITAL_INPUT_3"), //
			new ReadChannelId(21, "DIGITAL_INPUT_4"), //
			new WriteChannelId(22, "DIGITAL_OUTPUT_1"), //
			new WriteChannelId(23, "DIGITAL_OUTPUT_2"), //
			new WriteChannelId(24, "DIGITAL_OUTPUT_3"), //
			new WriteChannelId(25, "DIGITAL_OUTPUT_4"), //
			new ReadChannelId(500, "DIGITAL_INPUT_OUTPUT_1"), //
			new ReadChannelId(501, "DIGITAL_INPUT_OUTPUT_2"), //
			new ReadChannelId(502, "DIGITAL_INPUT_OUTPUT_3"), //
			new ReadChannelId(503, "DIGITAL_INPUT_OUTPUT_4") //
	);

	public ModberryX500M40804Max(HardwareFactory context) {
		super(context);
	}

	@Override
	public List<ChannelId> getAllChannelIds() {
		return this.channelIds;
	}
}
