package io.openems.backend.alerting;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.Test;
import org.osgi.service.event.Event;

import io.openems.backend.common.test.DummyMetadata;

public class AlertingTest {

	private static final int HANDLER_COUNT = 2;

	private static final Config testConf = new Config() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return null;
		}

		@Override
		public String webconsole_configurationFactory_nameHint() {
			return null;
		}

		@Override
		public int initialDelay() {
			return 15;
		}

		@Override
		public boolean notifyOnOffline() {
			return true;
		}

		@Override
		public boolean notifyOnSumStateChange() {
			return true;
		}
	};

	private static Config conf = new Config() {
		@Override
		public Class<? extends Annotation> annotationType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String webconsole_configurationFactory_nameHint() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int initialDelay() {
			return 15;
		}

		@Override
		public boolean notifyOnOffline() {
			return true;
		}

		@Override
		public boolean notifyOnSumStateChange() {
			return true;
		}
	};

	@Test
	public void testActivateAndDeactivate() {
		var alerting = new DummyAlerting();
		alerting.metadata = new DummyMetadata();

		// Activate
		alerting.activate(conf);

		assertEquals(HANDLER_COUNT, alerting.handlerCount());

		// Deactivate
		alerting.deactivate();

		assertEquals(0, alerting.handlerCount());
	}

	@Test
	public void testHandleEvent() {
		final var alerting = new DummyAlerting();
		final var event = new Event("TestEvent", Map.of());

		alerting.activate(testConf);

		assertEquals(null, alerting.lastEvent);

		alerting.handleEvent(event);

		assertEquals(event, alerting.lastEvent);
	}

	/* ********** */
	static class DummyAlerting extends Alerting {
		private Event lastEvent;

		public DummyAlerting() {
			super.metadata = new DummyMetadata();
		}

		private int handlerCount() {
			return super.handler.size();
		}

		@Override
		public void handleEvent(Event event) {
			this.lastEvent = event;
			super.handleEvent(event);
		}
	}
}
