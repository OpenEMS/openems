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
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;

public class SumStateAlertingTest {

	private static class TestEnvironment {
		record SimpleAlertingSetting(String user, int faultDelay, int warningDelay) {
		}

		private AlertingMetadataImpl meta;
		private MailerImpl mailer;
		private TimeLeapMinuteTimer timer;

		private Alerting alerting;
		private Scheduler scheduler;

		private HashMap<String, Edge> edges;
		private Map<String, List<SumStateAlertingSetting>> settings;

		public TestEnvironment() {
			var instant = Instant.ofEpochMilli(System.currentTimeMillis());
			this.timer = new TimeLeapMinuteTimer(instant);
			this.mailer = new MailerImpl();
			this.meta = new AlertingMetadataImpl();

			this.settings = new HashMap<>(5);
			this.edges = new HashMap<>(5);

			this.createEdge("edge01", true, null);
			this.createEdge("edge02", true, this.timer.now());
			this.createEdge("edge03", true, this.timer.now(), //
					new SimpleAlertingSetting("user01", 0, 0), //
					new SimpleAlertingSetting("user02", 0, 0));
			this.createEdge("edge04", true, this.timer.now(), //
					new SimpleAlertingSetting("user01", 30, 120), //
					new SimpleAlertingSetting("user02", 60, 120));
			this.createEdge("edge05", true, this.timer.now().minusHours(12), //
					new SimpleAlertingSetting("user02", 60, 120), //
					new SimpleAlertingSetting("user03", 1440, 1440));
			this.createEdge("edge06", true, this.timer.now().minusMonths(1), //
					new SimpleAlertingSetting("user01", 30, 60));
			this.createEdge("edge07", true, this.timer.now(), //
					new SimpleAlertingSetting("user04", 60, 60));

			this.meta.initializeSumState(this.edges.values(), this.settings);
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

			var list = new ArrayList<SumStateAlertingSetting>(5);
			for (var set : settings) {
				list.add(new SumStateAlertingSetting(id, set.user, set.faultDelay, set.warningDelay, lastMessage));
			}

			this.settings.put(edge.getId(), list);
		}

		public void setState(Level state, String... edges) {
			for (String edgeId : edges) {
				var edge = this.edges.get(edgeId);
				edge.setLastmessage(this.timer.now());
				this.meta.setSumState(edgeId, state);

				var event = EventBuilder.from(null, Edge.Events.ON_SET_SUM_STATE)//
						.addArg(Edge.Events.OnSetSumState.EDGE_ID, edge.getId())//
						.addArg(Edge.Events.OnSetSumState.SUM_STATE, state)//
						.build();

				this.alerting.handleEvent(event);
			}
		}

		public void setOnline(boolean value, String... edges) {
			for (String edgeId : edges) {
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
	}

	@Test
	public void integrationTest() {
		var env = new TestEnvironment();

		var config = Dummy.testConfig(15, false, true);
		env.alerting.activate(config);

		env.setState(Level.OK, "edge01", "edge02", "edge03", "edge04", "edge05", "edge06", "edge07");

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.timer.leap(2);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.setState(Level.WARNING, "edge03", "edge04");

		env.timer.leap(5);

		/* edge04[user01, user02] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.timer.leap(16);

		env.setState(Level.FAULT, "edge04");

		/* edge04[user01, user02] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.timer.leap(16);

		/* edge04[user02, user01] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.timer.leap(16);

		/* edge04[user02] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge04[user01] */
		assertEquals(1, env.mailer.getMailsCount());

		env.setState(Level.FAULT, "edge05", "edge06");

		/* edge04[user02] edge05[user02, user03] edge06[user01] */
		assertEquals(3, env.scheduler.getScheduledMsgsCount());
		/* edge04[user01] */
		assertEquals(1, env.mailer.getMailsCount());

		env.setOnline(false, "edge06");
		env.timer.leap(31);

		/* edge05[user02, user03] */
		assertEquals(1, env.scheduler.getScheduledMsgsCount());
		/* edge04[user01], edge04[user02] */
		assertEquals(2, env.mailer.getMailsCount());

		env.alerting.deactivate();

		env.timer.leap(1440);

		/* empty */
		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		/* edge04[user01], edge04[user02] */
		assertEquals(2, env.mailer.getMailsCount());
	}

	@Test
	public void deactivateTest() {
		var env = new TestEnvironment();
		/* All off */
		var config = Dummy.testConfig(5, false, true);
		env.alerting.activate(config);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		/* Wait long enough to trigger delayed Initialization. */
		env.timer.leap(config.initialDelay());
		env.timer.leap(10);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());

		env.alerting.deactivate();

		env.timer.leap(1440);

		assertEquals(0, env.scheduler.getScheduledMsgsCount());
		assertEquals(0, env.mailer.getMailsCount());
	}

}
