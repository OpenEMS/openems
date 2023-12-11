package io.openems.backend.alerting;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.openems.backend.alerting.Dummy.AlertingMetadataImpl;
import io.openems.backend.alerting.Dummy.MailerImpl;
import io.openems.backend.alerting.Dummy.TimeLeapMinuteTimer;
import io.openems.backend.alerting.scheduler.Scheduler;
import io.openems.backend.common.metadata.UserAlertingSettings;
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

		private HashMap<Integer, Edge> edges;
		private Map<String, List<UserAlertingSettings>> settings;

		public TestEnvironment() {
			final var instant = Instant.now();

			this.timer = new TimeLeapMinuteTimer(instant);
			final var now = this.timer.now();

			this.mailer = new MailerImpl();
			this.meta = new AlertingMetadataImpl();

			this.settings = new HashMap<>(5);
			this.edges = new HashMap<>(5);

			this.createEdge(1, false, null);
			this.createEdge(2, true, now);
			this.createEdge(3, true, now, //
					new SimpleAlertingSetting("user01", 0), // never
					new SimpleAlertingSetting("user02", 0)); // never
			this.createEdge(4, true, now, //
					new SimpleAlertingSetting("user01", 30), // 30m after offline
					new SimpleAlertingSetting("user02", 60)); // 60m after offline
			this.createEdge(5, false, now.minusHours(12), //
					new SimpleAlertingSetting("user02", 60), // after initialization
					new SimpleAlertingSetting("user03", 1440));// 12h after initialization
			this.createEdge(6, false, now.minusMonths(1), //
					new SimpleAlertingSetting("user01", 30)); // never (to long offline)
			this.createEdge(7, true, now, //
					new SimpleAlertingSetting("user04", 60)); // 60m after offline

			this.meta.initialize(this.edges.values(), this.settings);
			this.scheduler = new Scheduler(this.timer);

			var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>()) {
				@Override
				public void execute(Runnable command) {
					command.run();
				}
			};
			this.alerting = new Alerting(this.scheduler, executor);
			this.alerting.mailer = this.mailer;
			this.alerting.metadata = this.meta;
		}

		public void createEdge(int id, boolean online, ZonedDateTime lastMessage, SimpleAlertingSetting... settings) {
			var edgeName = "edge%d".formatted(id);
			var edge = new Edge(this.meta, edgeName, null, null, null, lastMessage);
			edge.setOnline(online);
			this.edges.put(id, edge);

			var list = new ArrayList<UserAlertingSettings>(5);
			for (var set : settings) {
				list.add(new UserAlertingSettings(id, set.user, null, lastMessage, set.delay));
			}

			this.settings.put(edge.getId(), list);
		}

		public void setOnline(int edgeId, boolean value) {
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

		var config = Dummy.testConfig(15);
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

		env.setOnline(2, false);

		/* edge05[user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge05[user02] */
		assertEquals(1, env.mailer.getMailsCount());

		env.setOnline(3, false);
		env.setOnline(4, false);
		env.setOnline(7, false);
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

		env.setOnline(7, true);
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
	public void deactiveTest() {
		var env = new TestEnvironment();
		/* All off */
		var config = Dummy.testConfig(5);
		env.alerting.activate(config);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		/* Wait long enough to trigger delayed Initialization. */
		env.timer.leap(config.initialDelay());
		env.timer.leap(3); /* inaccuracy + initial mails on next cycle */

		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		assertEquals(1, env.mailer.getMailsCount());

		env.alerting.deactivate();

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(1, env.mailer.getMailsCount());
	}

}
