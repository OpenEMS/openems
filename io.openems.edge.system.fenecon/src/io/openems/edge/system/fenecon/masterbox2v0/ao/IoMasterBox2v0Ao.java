package io.openems.edge.system.fenecon.masterbox2v0.ao;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.io.api.AnalogVoltageOutput;

public interface IoMasterBox2v0Ao extends OpenemsComponent, AnalogVoltageOutput, AnalogOutput {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DEBUG_ANALOG_OUT_CONTROL(Doc.of(OpenemsType.BOOLEAN)),

		ANALOG_OUT_CONTROL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_ANALOG_OUT_CONTROL)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link IoMasterBox2v0Ao.ChannelId#ANALOG_OUT_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getAnalogOutControlChannel() {
		return this.channel(ChannelId.ANALOG_OUT_CONTROL);
	}

}
