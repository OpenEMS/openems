package io.openems.edge.controller.api.rest;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple OpenEMS Component which can be used for testing.
 */
public class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DUMMY_CHANNEL(Doc.of(OpenemsType.INTEGER)); //

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
