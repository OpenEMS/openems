package io.openems.edge.controller.byd.alarm;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple, simulated ManagedSymmetricEss component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Dummy state channels for testing
		 */
		STATE_0(new Doc().level(Level.WARNING).text("State 1")),
		STATE_1(new Doc().level(Level.WARNING).text("State 2")),
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
	
	public DummyComponent(String id) {
		Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_0:
					case STATE_1:	
						return new StateChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> {
					channel.nextProcessImage();
					this.addChannel(channel);
				});
		super.activate(null, id, true);
	}

}
