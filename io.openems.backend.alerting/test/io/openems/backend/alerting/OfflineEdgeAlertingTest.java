package io.openems.backend.alerting;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.openems.backend.alerting.Dummy.AlertingMetadataImpl;
import io.openems.backend.alerting.Dummy.MailerImpl;
import io.openems.backend.alerting.Dummy.TimeLeapMinuteTimer;
import io.openems.backend.alerting.scheduler.Scheduler;
import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.common.event.EventBuilder;

public class OfflineEdgeAlertingTest {

	private static class TestEnvironment {
		record SimpleAlertingSetting(String user, int delay) {
		}

		private AlertingMetadataImpl meta;
		private MailerImpl mailer;
		private TimeLeapMinuteTimer timer;

		private Alerting alerting;
		private Scheduler scheduler;

		private HashMap<String, Edge> edges;
		private Map<String, List<OfflineEdgeAlertingSetting>> settings;

		public TestEnvironment() {
			final var instant = Instant.now();

			this.timer = new TimeLeapMinuteTimer(instant);
			final var now = this.timer.now();

			this.mailer = new MailerImpl();
			this.meta = new AlertingMetadataImpl();

			this.settings = new HashMap<>(5);
			this.edges = new HashMap<>(5);

			this.createEdge("edge01", false, null);
			this.createEdge("edge02", true, now);
			this.createEdge("edge03", true, now, //
					new SimpleAlertingSetting("user01", 0), //
					new SimpleAlertingSetting("user02", 0));
			this.createEdge("edge04", true, now, //
					new SimpleAlertingSetting("user01", 30), //
					new SimpleAlertingSetting("user02", 60));
			this.createEdge("edge05", false, now.minusHours(12), //
					new SimpleAlertingSetting("user02", 60), //
					new SimpleAlertingSetting("user03", 1440));
			this.createEdge("edge06", false, now.minusMonths(1), //
					new SimpleAlertingSetting("user01", 30));
			this.createEdge("edge07", true, now, //
					new SimpleAlertingSetting("user04", 60));

			this.meta.initializeOffline(this.edges.values(), this.settings);
			this.scheduler = new Scheduler(this.timer);

			this.alerting = new Alerting(this.scheduler, Dummy.executor());
			this.alerting.mailer = this.mailer;
			this.alerting.metadata = this.meta;
		}

		public void createEdge(String id, boolean online, ZonedDateTime lastMessage,
				SimpleAlertingSetting... settings) {
			var edge = new Edge(this.meta, id, null, null, null, lastMessage);
			edge.setOnline(online);
			this.edges.put(id, edge);

			var list = new ArrayList<OfflineEdgeAlertingSetting>(5);
			for (var set : settings) {
				list.add(new OfflineEdgeAlertingSetting(id, set.user, set.delay, lastMessage));
			}

			this.settings.put(edge.getId(), list);
		}

		public void setOnline(String edgeId, boolean value) {
			var edge = this.edges.get(edgeId);
			edge.setOnline(value);
			edge.setLastmessage(this.timer.now());

			var event = EventBuilder.from(null, Edge.Events.ON_SET_ONLINE)//
					.addArg(Edge.Events.OnSetOnline.EDGE_ID, edge.getId())//
					.addArg(Edge.Events.OnSetOnline.IS_ONLINE, value)//
					.build();

			this.alerting.handleEvent(event);
		}
	}

	@Test
	public void integrationTest() {
		var env = new TestEnvironment();

		var config = Dummy.testConfig(15, true, false);
		env.alerting.activate(config);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		/* Wait long enough to trigger delayed Initialization. */
		env.timer.leap(config.initialDelay());
		env.timer.leap(3); /* inaccuracy + initial mails on next cycle */

		/* edge05[user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] */
		assertEquals(1, env.mailer.getMailsCount());

		env.setOnline("edge02", false);

		/* edge05[user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] */
		assertEquals(1, env.mailer.getMailsCount());

		env.setOnline("edge03", false);
		env.setOnline("edge04", false);
		env.setOnline("edge07", false);
		env.timer.leap(1); /* inaccuracy */

		/* edge05[user03], edge03[user01,user02], edge07[user04] */
		assertEquals(3, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] */
		assertEquals(1, env.mailer.getMailsCount());

		env.timer.leap(30);

		/* edge05[user03], edge03[user01], edge07[user04] */
		assertEquals(3, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] edge03[user02] */
		assertEquals(2, env.mailer.getMailsCount());

		env.setOnline("edge07", true);
		env.timer.leap(30);

		/* edge05[user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge05.user02, edge03.user01, edge03.user02 */
		assertEquals(3, env.mailer.getMailsCount());

		env.alerting.deactivate();

		/* empty */
		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		/* edge05.user02, edge03.user01, edge03.user02 */
		assertEquals(3, env.mailer.getMailsCount());

		env.timer.leap(1440);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(3, env.mailer.getMailsCount());
	}

	@Test
	public void deactivateTest() {
		var env = new TestEnvironment();
		/* All off */
		var config = Dummy.testConfig(5, true, false);
		env.alerting.activate(config);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		/* Wait long enough to trigger delayed Initialization. */
		env.timer.leap(config.initialDelay());
		env.timer.leap(3); /* inaccuracy + initial mails on next cycle */

		/* edge05[user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] */
		assertEquals(1, env.mailer.getMailsCount());

		env.alerting.deactivate();

		env.timer.leap(1440);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(1, env.mailer.getMailsCount());
	}

}
