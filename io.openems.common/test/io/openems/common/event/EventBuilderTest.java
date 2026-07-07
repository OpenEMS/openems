package io.openems.common.event;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventBuilderTest {

	private static Event testEvent;
	private static TestClass testObject;

	@Test
	public void testABuild() {
		testObject = new TestClass();

		assertDoesNotThrow(() -> //
		EventBuilder.from(new EventAdminTest(), "Test") //
				.addArg("arg1", 10) //
				.addArg("arg2", "String") //
				.addArg("arg3", testObject) //
				.send());
	}

	@Test
	public void testBRead() {
		assertEquals(10, (int) testEvent.getProperty("arg1"));
		assertEquals("String", testEvent.getProperty("arg2"));
		assertEquals(testObject, testEvent.getProperty("arg3"));
	}

	private static class EventAdminTest implements EventAdmin {
		@Override
		public void postEvent(Event event) {
			testEvent = event;
		}

		@Override
		public void sendEvent(Event event) {
			testEvent = event;
		}
	}

	private static class TestClass {

	}
}
