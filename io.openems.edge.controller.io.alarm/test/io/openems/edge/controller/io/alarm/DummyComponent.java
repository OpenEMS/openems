package io.openems.edge.controller.io.alarm;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple OpenEMS Component which can be used for testing.
 */
public class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Dummy state channels for testing.
		 */
		STATE_0(Doc.of(Level.WARNING).text("State 0")), //
		STATE_1(Doc.of(Level.WARNING).text("State 1")),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DummyComponent(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
		super.activate(null, id, "", true);
	}

}
