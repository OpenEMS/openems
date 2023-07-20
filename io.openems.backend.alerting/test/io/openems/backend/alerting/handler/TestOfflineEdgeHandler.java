package io.openems.backend.alerting.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.backend.alerting.Dummy.MailerImpl;
import io.openems.backend.alerting.Dummy.MessageSchedulerServiceImpl;
import io.openems.backend.alerting.Dummy.OfflineEdgeMetadataImpl;
import io.openems.backend.alerting.Dummy.SimpleMetadataImpl;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.test.DummyMetadata;

public class TestOfflineEdgeHandler {

	@Test
	public void testGetterSetter() {
		final var mss = new MessageSchedulerServiceImpl();
		final var meta = new SimpleMetadataImpl();
		final var handler = new OfflineEdgeHandler(mss, null, meta, 1);
		assertEquals(OfflineEdgeMessage.class, handler.getGeneric());
	}

	@Test
	public void testActivate() {
		var service = new MessageSchedulerServiceImpl();
		var handler = new OfflineEdgeHandler(service, null, new SimpleMetadataImpl(), 1);
		assertEquals(1, service.msgScheduler.size());
		assertTrue(service.msgScheduler.get(0).isFor(handler));
		handler.stop();
		assertEquals(0, service.msgScheduler.size());
	}

	@Test
	public void send() {
		final var mailer = new MailerImpl();
		final var msgsch = new MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(msgsch, mailer, Utility.getTestMetadata(), 1);
		final var msg_1 = new OfflineEdgeMessage("1", ZonedDateTime.now().minusSeconds(1));
		msg_1.addRecipient(new OfflineEdgeAlertingSetting(1, 1, 1, null));
		msg_1.addRecipient(new OfflineEdgeAlertingSetting(1, 2, 2, null));

		final var msg_2 = new OfflineEdgeMessage("Fail", ZonedDateTime.now().minusSeconds(2));
		msg_2.addRecipient(new OfflineEdgeAlertingSetting(2, 1, 1, null));
		msg_2.addRecipient(new OfflineEdgeAlertingSetting(2, 2, 2, null));

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
		final var metadata = Utility.getTestMetadata();
		final var msgsch = new MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(msgsch, null, metadata, 0);

		final var expected = (int) metadata.getSettings().values().stream() //
				.filter(setting -> setting.stream() //
						.filter(s -> s.delay() > 0) //
						.filter(s -> s.lastNotification().isBefore(Utility.now)) //
						.count() != 0) //
				.count();
		assertEquals(expected, msgsch.find(handler).size());

		var yesterday = Utility.now.minusDays(1);
		var edge = Utility.getTestEdge(metadata, "edgeTest", yesterday, false);
		handler.tryAddEdge(edge);

		assertEquals(expected, msgsch.find(handler).size());

		handler.tryRemoveEdge(edge);

		assertEquals(expected, msgsch.find(handler).size());
	}

	@Test
	public void checkMetadataEmergencyStop() {
		final var msgMeta = new Utility.ToManyMsgsMetadata();
		final var msgMsgsch = new MessageSchedulerServiceImpl();
		final var msgCount = new AtomicInteger();
		msgMeta.getAllOfflineEdges().stream().map(e -> msgMeta.getEdgeOfflineAlertingSettings(e.getId())).forEach(e -> {
			e.forEach(s -> {
				if (s.lastNotification().plusMinutes(s.delay()).isBefore(Utility.now)) {
					msgCount.getAndIncrement();
				}
			});
		});

		assertTrue(msgCount.get() + " are Not enought mails to trigger",
				msgCount.get() > OfflineEdgeHandler.MAX_SIMULTANEOUS_MSGS);

		final var handler = new OfflineEdgeHandler(msgMsgsch, null, msgMeta, 0);

		assertEquals(0, msgMsgsch.find(handler).size());

		//

		final var edgeMeta = new Utility.ToManyEdgesMetadata();
		final var edgeMsgsch = new MessageSchedulerServiceImpl();
		final var edgeCount = new AtomicInteger();
		edgeMeta.getAllOfflineEdges().stream().map(e -> edgeMeta.getEdgeOfflineAlertingSettings(e.getId()))
				.forEach(e -> {
					edgeCount.getAndIncrement();
				});

		assertTrue("Not enought mails to trigger", edgeCount.get() > OfflineEdgeHandler.MAX_SIMULTANEOUS_EDGES);

		final var edgeHandler = new OfflineEdgeHandler(edgeMsgsch, null, edgeMeta, 0);

		assertEquals(0, edgeMsgsch.find(edgeHandler).size());
	}

	@Test
	public void getEdgeTest() {
		final var metadata = Utility.getTestMetadata();
		final var mss = new MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(mss, null, metadata, 1);
		final var illegalEdge = new Edge(null, null, null, null, null, null);
		assertNull("Message should be null if edge is null", handler.getEdgeMessage(null));
		assertNull("Message should be null if edge.id is null", handler.getEdgeMessage(illegalEdge));
	}

	@Test
	public void getEventHandlerTest() {
		final var metadata = Utility.getTestMetadata();
		final var mss = new MessageSchedulerServiceImpl();
		final var handler = new OfflineEdgeHandler(mss, null, metadata, 1);
		var offlineEdgeHandler = handler.getEventHandler(Edge.Events.ON_SET_ONLINE);
		var metadataInializedHandler = handler.getEventHandler(Metadata.Events.AFTER_IS_INITIALIZED);
		var noHandler = handler.getEventHandler("NO_EVENT");

		assertNotNull("Should have handler for Edge.ON_SET_ONLINE event", offlineEdgeHandler);
		assertNotNull("Should have handler for Metadata.AFTER_IS_INITIALIZED event", metadataInializedHandler);
		assertNull(noHandler);
	}

	private static class Utility {

		private static final ZonedDateTime now = ZonedDateTime.now();

		private static OfflineEdgeMetadataImpl getTestMetadata() {
			final var metadata = new OfflineEdgeMetadataImpl();

			final List<Edge> edges = List.of(//
					Utility.getTestEdge(metadata, "1", Utility.now, false), //
					Utility.getTestEdge(metadata, "2", Utility.now, true), //
					Utility.getTestEdge(metadata, "3", Utility.now, false), //
					Utility.getTestEdge(metadata, "4", Utility.now, false),
					Utility.getTestEdge(metadata, "5", Utility.now.minusMonths(1), false), //
					Utility.getTestEdge(metadata, "6", null, false)); //

			final Map<String, List<OfflineEdgeAlertingSetting>> settings = Map.of(//
					"1", List.of(//
							new OfflineEdgeAlertingSetting(1, 1, 1, Utility.now.minusDays(1))), //
					"2", List.of(), //
					"3", List.of(//
							new OfflineEdgeAlertingSetting(3, 1, 1, Utility.now.plusDays(1)), //
							new OfflineEdgeAlertingSetting(3, 2, 0, Utility.now)), //
					"4", List.of(//
							new OfflineEdgeAlertingSetting(4, 2, 1, Utility.now.minusDays(1)), //
							new OfflineEdgeAlertingSetting(4, 3, 2, Utility.now.minusDays(1))) //
			);

			metadata.initialize(edges, settings);
			return metadata;
		}

		private static Edge getTestEdge(Metadata metadata, String id, ZonedDateTime LastMessage, boolean isOnline) {
			final var edge = new Edge(metadata, id, "comment", "version", "producttype", LastMessage);
			edge.setOnline(isOnline);
			return edge;
		}

		private static class ToManyMsgsMetadata extends DummyMetadata {
			private final ZonedDateTime now = ZonedDateTime.now();
			private final ZonedDateTime yesterday = this.now.minusDays(1);

			private List<OfflineEdgeAlertingSetting> userList = List.of(
					new OfflineEdgeAlertingSetting(1, 0, 60, this.yesterday),
					new OfflineEdgeAlertingSetting(1, 1, 15, this.yesterday),
					new OfflineEdgeAlertingSetting(1, 2, 10, this.yesterday),
					new OfflineEdgeAlertingSetting(1, 3, 30, this.yesterday),
					new OfflineEdgeAlertingSetting(1, 4, 30, this.yesterday),
					new OfflineEdgeAlertingSetting(1, 5, 1440, this.yesterday));

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
			public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) {
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
			public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) {
				return List.of(new OfflineEdgeAlertingSetting(0, 0, 60, this.yesterday),
						new OfflineEdgeAlertingSetting(0, 1, 15, this.yesterday),
						new OfflineEdgeAlertingSetting(0, 2, 0, this.yesterday));
			}
		}
	}
}
