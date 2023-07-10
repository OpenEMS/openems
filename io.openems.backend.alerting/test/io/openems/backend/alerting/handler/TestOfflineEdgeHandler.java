package io.openems.backend.alerting.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.backend.alerting.Dummy;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.session.Role;

public class TestOfflineEdgeHandler {
	private final ZonedDateTime now = ZonedDateTime.now();
	private final Dummy.MetadataImpl metadata = new Dummy.MetadataImpl();

	public TestOfflineEdgeHandler() {
		this.metadata.initialize(//
				List.of(//
						this.getTestEdge(this.metadata, "1", ZonedDateTime.now(), false), //
						this.getTestEdge(this.metadata, "2", ZonedDateTime.now(), true), //
						this.getTestEdge(this.metadata, "3", ZonedDateTime.now(), false), //
						this.getTestEdge(this.metadata, "4", ZonedDateTime.now(), false)),
				Map.of(//
						"1", List.of(//
								new AlertingSetting(1, "1", Role.GUEST, this.now.minusDays(1), 1)), //
						"2", List.of(), //
						"3", List.of(//
								new AlertingSetting(2, "2", Role.GUEST, this.now.plusDays(1), 1), //
								new AlertingSetting(3, "3", Role.GUEST, this.now, 0)), //
						"4", List.of(//
								new AlertingSetting(4, "2", Role.GUEST, this.now.minusDays(1), 1), //
								new AlertingSetting(5, "3", Role.GUEST, this.now.minusDays(1), 2)) //
				));
	}

	private Edge getTestEdge(Metadata metadata, String id, ZonedDateTime LastMessage, boolean isOnline) {
		final var edge = new Edge(metadata, id, "comment", "version", "producttype", LastMessage);
		edge.setOnline(isOnline);
		return edge;
	}

	@Test
	public void testGetterSetter() {
		final var mss = new Dummy.MessageSchedulerServiceImpl();
		final var meta = new Dummy.SimpleMetadataImpl();
		final var handler = new OfflineEdgeHandler(mss, null, meta, 1);
		assertEquals(OfflineEdgeMessage.class, handler.getGeneric());
	}

	@Test
	public void testActivate() {
		var service = new Dummy.MessageSchedulerServiceImpl();
		var handler = new OfflineEdgeHandler(service, null, new Dummy.SimpleMetadataImpl(), 1);
		assertEquals(1, service.msgScheduler.size());
		assertTrue(service.msgScheduler.get(0).isFor(handler));
		handler.stop();
		assertEquals(0, service.msgScheduler.size());
	}

	@Test
	public void send() {
		final var mailer = new Dummy.MailerImpl();
		final var msgsch = new Dummy.MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(msgsch, mailer, this.metadata, 1);
		final var msg_1 = new OfflineEdgeMessage("1", ZonedDateTime.now().minusSeconds(1));
		msg_1.addRecipient(new AlertingSetting(0, null, null, null, 1));
		msg_1.addRecipient(new AlertingSetting(0, null, null, null, 2));

		final var msg_2 = new OfflineEdgeMessage("Fail", ZonedDateTime.now().minusSeconds(2));
		msg_2.addRecipient(new AlertingSetting(0, null, null, null, 1));
		msg_2.addRecipient(new AlertingSetting(0, null, null, null, 2));

		assertEquals(mailer.sentMails.size(), 0);
		var msgs = new ArrayList<>(List.of(msg_1, msg_2));
		handler.send(ZonedDateTime.now(), msgs);
		assertEquals(1, mailer.sentMails.size());

		// check if correctly rescheduled
		final var sc = msgsch.find(handler);
		assertTrue(sc.isScheduled(msg_1));
		assertFalse(sc.isScheduled(msg_2));
	}

	@Test
	public void checkmetadata() {
		final var msgsch = new Dummy.MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(msgsch, null, this.metadata, 0);

		final var expected = (int) this.metadata.getSettings().values().stream().filter(e -> e.stream()
				.filter(s -> s.getDelayTime() > 0).filter(s -> s.getLastNotification().isBefore(this.now)).count() != 0)
				.count();
		assertEquals(expected, msgsch.find(handler).size());
	}

	private static class ToManyMsgsMetadata extends DummyMetadata {
		private final ZonedDateTime now = ZonedDateTime.now();
		private final ZonedDateTime yesterday = this.now.minusDays(1);

		private List<AlertingSetting> userList = List.of(
				new AlertingSetting(0, "user0", Role.OWNER, this.yesterday, 60),
				new AlertingSetting(1, "user1", Role.OWNER, this.yesterday, 15),
				new AlertingSetting(2, "user2", Role.OWNER, this.yesterday, 10),
				new AlertingSetting(3, "user3", Role.OWNER, this.yesterday, 30),
				new AlertingSetting(4, "user4", Role.OWNER, this.yesterday, 30),
				new AlertingSetting(5, "user5", Role.OWNER, this.yesterday, 1440));

		@Override
		public boolean isInitialized() {
			return true;
		}

		@Override
		public Collection<Edge> getAllOfflineEdges() {
			var toMany = OfflineEdgeHandler.MAX_SIMULTANEOUS_MSGS / (this.userList.size() - 2);

			var edges = new ArrayList<Edge>(toMany);
			for (var i = 0; i < toMany; i++) {
				edges.add(new Edge(null, "edge" + i, null, null, null, this.now));
			}
			return edges;
		}

		@Override
		public List<AlertingSetting> getUserAlertingSettings(String edgeId) {
			return this.userList;
		}
	}

	private static class ToManyEdgesMetadata extends DummyMetadata {
		private final ZonedDateTime now = ZonedDateTime.now();
		private final ZonedDateTime yesterday = this.now.minusDays(1);

		@Override
		public boolean isInitialized() {
			return true;
		}

		@Override
		public Collection<Edge> getAllOfflineEdges() {
			var toMany = OfflineEdgeHandler.MAX_SIMULTANEOUS_EDGES + 20;
			var edges = new ArrayList<Edge>(toMany);
			for (var i = 0; i < toMany; i++) {
				edges.add(new Edge(null, "edge" + i, null, null, null, this.now));
			}
			return edges;
		}

		@Override
		public List<AlertingSetting> getUserAlertingSettings(String edgeId) {
			return List.of(new AlertingSetting(0, "user0", Role.OWNER, this.yesterday, 60),
					new AlertingSetting(1, "user1", Role.OWNER, this.yesterday, 15),
					new AlertingSetting(2, "user2", Role.OWNER, this.yesterday, 0));
		}
	}

	@Test
	public void checkMetadataEmergencyStop() {
		final var msgMeta = new ToManyMsgsMetadata();
		final var msgMsgsch = new Dummy.MessageSchedulerServiceImpl();
		final var msgCount = new AtomicInteger();
		msgMeta.getAllOfflineEdges().stream().map(e -> msgMeta.getUserAlertingSettings(e.getId())).forEach(e -> {
			e.forEach(s -> {
				if (s.getLastNotification().plusMinutes(s.getDelayTime()).isBefore(this.now)) {
					msgCount.getAndIncrement();
				}
			});
		});

		assertTrue(msgCount.get() + " are Not enought mails to trigger",
				msgCount.get() > OfflineEdgeHandler.MAX_SIMULTANEOUS_MSGS);

		final var handler = new OfflineEdgeHandler(msgMsgsch, null, msgMeta, 0);

		assertEquals(0, msgMsgsch.find(handler).size());

		//

		final var edgeMeta = new ToManyEdgesMetadata();
		final var edgeMsgsch = new Dummy.MessageSchedulerServiceImpl();
		final var edgeCount = new AtomicInteger();
		edgeMeta.getAllOfflineEdges().stream().map(e -> edgeMeta.getUserAlertingSettings(e.getId())).forEach(e -> {
			edgeCount.getAndIncrement();
		});

		assertTrue("Not enought mails to trigger", edgeCount.get() > OfflineEdgeHandler.MAX_SIMULTANEOUS_EDGES);

		final var edgeHandler = new OfflineEdgeHandler(edgeMsgsch, null, edgeMeta, 0);

		assertEquals(0, edgeMsgsch.find(edgeHandler).size());
	}
}
