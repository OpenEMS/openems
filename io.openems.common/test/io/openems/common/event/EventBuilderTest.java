package io.openems.common.event;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventBuilderTest {

	private static Event EVENT;
	private static TestClass TEST_OBJECT;

	@Test
	public void testBuild() {
		TEST_OBJECT = new TestClass();

		EventBuilder.from(new EventAdminTest(), "Test").addArg("arg1", 10).addArg("arg2", "String")
				.addArg("arg3", TEST_OBJECT).send();
	}

	@Test
	public void testRead() {
		var event = EVENT;

		assertEquals(10, (int) event.getProperty("arg1"));
		assertEquals("String", event.getProperty("arg2"));
		assertEquals(TEST_OBJECT, event.getProperty("arg3"));
	}

	private static class EventAdminTest implements EventAdmin {
		public EventAdminTest() {

		}

		@Override
		public void postEvent(Event event) {
			EVENT = event;
		}

		@Override
		public void sendEvent(Event event) {
			EVENT = event;
		}
	}

	private static class TestClass {

	}
}
