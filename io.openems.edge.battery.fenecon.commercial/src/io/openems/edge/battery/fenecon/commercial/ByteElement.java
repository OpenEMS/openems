package io.openems.edge.battery.fenecon.commercial;

import java.util.function.Consumer;

public class ByteElement implements Consumer<Integer> {

	private final BatteryFeneconCommercialImpl parent;
	private final ByteElement.Shifter shifter;
	private final io.openems.edge.common.channel.ChannelId[] channels;

	public ByteElement(BatteryFeneconCommercialImpl parent, Shifter shifter,
			io.openems.edge.common.channel.ChannelId... channels) {
		this.parent = parent;
		this.shifter = shifter;
		this.channels = channels;
	}

	public static enum Shifter {
		SEPARATE_BITS_AS_6_AND_10_FOR_TWO_CHANNELS, SEPERATE_TO_TWO_8_BIT_CHANNELS, ONLY_FIRST_CHANNEL,
		ONLY_SECOND_CHANNEL
	}

	@Override
	public void accept(Integer t) {
		if (t == null) {
			return;
		}

		switch (this.shifter) {
		case ONLY_FIRST_CHANNEL:
			this.parent.channel(this.channels[0].id()).setNextValue(t & 0xff);
			break;
		case ONLY_SECOND_CHANNEL:
			this.parent.channel(this.channels[0].id()).setNextValue((t & 0xff00) >> 8);
			break;
		case SEPARATE_BITS_AS_6_AND_10_FOR_TWO_CHANNELS:
			this.parent.channel(this.channels[0].id()).setNextValue(t & 0x3f);
			this.parent.channel(this.channels[1].id()).setNextValue((t & 0xffc0) >> 6);
			break;
		case SEPERATE_TO_TWO_8_BIT_CHANNELS:
			this.parent.channel(this.channels[0].id()).setNextValue(t & 0xff);
			this.parent.channel(this.channels[1].id()).setNextValue((t & 0xff00) >> 8);
			break;
		}
	}
}