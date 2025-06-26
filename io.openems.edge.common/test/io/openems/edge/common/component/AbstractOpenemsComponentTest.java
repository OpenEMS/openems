package io.openems.edge.common.component;

import org.junit.Test;

public class AbstractOpenemsComponentTest {

	private static class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values() //
			);
			super.activate(null, id, "", true);
		}

	}

	@Test(expected = IllegalArgumentException.class)
	public void test() {
		new DummyComponent(null);
	}

}
