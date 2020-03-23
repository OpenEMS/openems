package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public class DummyIo extends AbstractOpenemsComponent implements DigitalOutput, DigitalInput, OpenemsComponent {

	private final BooleanReadChannel[] digitalInputChannels;
	private final BooleanWriteChannel[] digitalOutputChannels;

	public DummyIo() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values()
		);
		String channelNameOutSync = Creator.OUTPUT_SYNC_DEVICE_BRIDGE.substring(Creator.OUTPUT_SYNC_DEVICE_BRIDGE.indexOf("/") + 1);
		ChannelId id_output_sync = new ChannelId(channelNameOutSync, new BooleanDoc().accessMode(AccessMode.READ_WRITE));		
		BooleanWriteChannel output_sync_channel = (BooleanWriteChannel) addChannel(id_output_sync);
		
		String channelNameOutHard = Creator.OUTPUT_HARD_RESET.substring(Creator.OUTPUT_HARD_RESET.indexOf("/") + 1);
		ChannelId id_output_hard = new ChannelId(channelNameOutHard, new BooleanDoc().accessMode(AccessMode.READ_WRITE));		
		BooleanWriteChannel output_hard_channel = (BooleanWriteChannel) addChannel(id_output_hard);
		
		digitalOutputChannels = new BooleanWriteChannel[] {output_sync_channel, output_hard_channel };
		
		String channelNameInNA1 = Creator.INPUT_NA_PROTECTION_1.substring(Creator.INPUT_NA_PROTECTION_1.indexOf("/") + 1);
		ChannelId id_input_na1 = new ChannelId(channelNameInNA1, new BooleanDoc().accessMode(AccessMode.READ_ONLY));		
		BooleanReadChannel input_na1_channel = (BooleanReadChannel) addChannel(id_input_na1);
		
		String channelNameInNA2 = Creator.INPUT_NA_PROTECTION_2.substring(Creator.INPUT_NA_PROTECTION_2.indexOf("/") + 1);
		ChannelId id_input_na2 = new ChannelId(channelNameInNA2, new BooleanDoc().accessMode(AccessMode.READ_ONLY));		
		BooleanReadChannel input_na2_channel = (BooleanReadChannel) addChannel(id_input_na2);
		
		String channelNameInSync = Creator.INPUT_SYNC_DEVICE_BRIDGE.substring(Creator.INPUT_SYNC_DEVICE_BRIDGE.indexOf("/") + 1);
		ChannelId id_input_sync = new ChannelId(channelNameInSync, new BooleanDoc().accessMode(AccessMode.READ_ONLY));		
		BooleanReadChannel input_sync_channel = (BooleanReadChannel) addChannel(id_input_sync);
		
		digitalInputChannels = new BooleanReadChannel[] {input_na1_channel, input_na2_channel, input_sync_channel };
	}

	public static String adaptChannelAdress(String adress) {
		String separator = "/";
		String s = adress.toLowerCase();
		StringBuilder b = new StringBuilder();
		String parts[] = s.split(separator);
		b.append(parts[0]);
		b.append(separator);
		String p2 = parts[1];
		b.append(p2.substring(0, 1).toUpperCase());
		b.append(p2.substring(1));		
		return b.toString();
	}
	
	public class ChannelId implements io.openems.edge.common.channel.ChannelId {

		private final String name;
		private final OpenemsTypeDoc<Boolean> doc;

		public ChannelId(String name, OpenemsTypeDoc<Boolean> doc) {
			this.name = name;
			this.doc = doc;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return digitalInputChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return digitalOutputChannels;
	}
}
