package io.openems.edge.common.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

class OpenemsComponentTest {

	private static class MyComponent extends AbstractOpenemsComponent {

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			INTEGER_CHANNEL(Doc.of(OpenemsType.INTEGER)), //
			;

			private final Doc doc;

			ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		protected MyComponent() {
			super(MyComponent.ChannelId.values());
		}
	}

	@Test
	void testGetComponentIdentifier() {

		// Component is 'null'
		assertNull(OpenemsComponent.getComponentIdentifier(null));

		var sut = new MyComponent();
		// 'id' is null
		assertEquals(MyComponent.class.getSimpleName(), OpenemsComponent.getComponentIdentifier(sut));

		sut.activate(null, "foo", null, true);
		// 'id' is set
		assertEquals("foo", OpenemsComponent.getComponentIdentifier(sut));
	}

	@Test
	void testChannelOrNull() {
		var sut = new MyComponent();

		assertNull(sut.channelOrNull(new ChannelId.ChannelIdImpl("NOT_EXISTING", Doc.of(OpenemsType.BOOLEAN))));
		assertNotNull(sut.channelOrNull(MyComponent.ChannelId.INTEGER_CHANNEL));
	}

}
