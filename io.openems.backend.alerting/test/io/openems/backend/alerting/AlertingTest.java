package io.openems.backend.alerting;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

import com.google.gson.JsonElement;

import io.openems.backend.alerting.Alerting.StartParameter;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.test.DummyMetadata;

public class AlertingTest {

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
	};

	private DummyAlerting alerting;

	@Before
	public void test() {
		this.alerting = new DummyAlerting(conf);

		var params = new StartParameter(new DummyMetadata(), new DummyMailer());
		this.alerting.bindStartParameter(params);
	}

	@Test
	public void testActivateAndDeactivate() {
		assertEquals(1, this.alerting.handlerCount());

		this.alerting.unbindStartParameter(null);

		assertEquals(0, this.alerting.handlerCount());
	}

	@Test
	public void testHandleEvent() {
		assertEquals(null, this.alerting.lastEvent);

		final var event = new Event("TestEvent", Map.of());
		this.alerting.handleEvent(event);

		assertEquals(event, this.alerting.lastEvent);
	}

	static class DummyMailer implements Mailer {

		@Override
		public void sendMail(ZonedDateTime sendAt, String template, JsonElement params) {
			throw new UnsupportedOperationException();
		}

	}

	/* ********** */
	static class DummyAlerting extends Alerting {
		Event lastEvent;

		public DummyAlerting(Config config) {
			super(config);
		}

		private int handlerCount() {
			if (super.handler == null) {
				return 0;
			}
			return super.handler.length;
		}

		@Override
		public void handleEvent(Event event) {
			this.lastEvent = event;
			super.handleEvent(event);
		}
	}
}
