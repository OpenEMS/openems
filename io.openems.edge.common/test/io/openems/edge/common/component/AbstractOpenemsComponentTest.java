package io.openems.edge.common.component;

import static io.openems.edge.common.component.AbstractOpenemsComponent.propertyIdToMethodName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AbstractOpenemsComponentTest {

	@Test
	public void testPropertyIdToMethodName() {
		assertEquals("modbus_id", propertyIdToMethodName("modbus.id"));
		assertEquals("modbus_component_id", propertyIdToMethodName("modbus.component.id"));
		assertEquals("alias", propertyIdToMethodName("alias"));
	}

	private static class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values() //
			);
			super.activate(null, id, "", true);
		}

	}

	@Test
	public void test() {
		assertThrows(IllegalArgumentException.class, () -> new DummyComponent(null));
	}

}
