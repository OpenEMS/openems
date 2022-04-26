package io.openems.backend.alerting.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.openems.backend.alerting.Message;
import io.openems.backend.alerting.ScheduleMessageService;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeUser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScheduleMessageServiceTest {

	private static final AtomicBoolean actionHasTriggered = new AtomicBoolean(false);
	
	private static final Consumer<Message> action = (message) -> {
		actionHasTriggered.set(true);
		
		long sek = ChronoUnit.SECONDS.between(
				ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault()), 
				message.getTimeStamp());
		// within one second of tolerance
		assertTrue("the test was " + sek + "sek off", sek < 1 && sek > -1);
	};
	private static final ScheduleMessageService service = new ScheduleMessageService(null, action);

	public ScheduleMessageServiceTest() {
		ScheduleMessageServiceTest.service.start();
	}

	private final String edge1Id = "test1";
	private final String edge2Id = "test2";

	@Test
	public void testAScheduleTask() {
		Edge test1 = this.getEdge(this.edge1Id);
		test1.addUser(new EdgeUser(100, test1.getId(), "user1", 100, null));
		test1.addUser(new EdgeUser(101, test1.getId(), "user2", 100, null));
		test1.addUser(new EdgeUser(102, test1.getId(), "user3",  90, null));
		test1.addUser(new EdgeUser(103, test1.getId(), "user4",  70, null));
		// Test schedule test1
		this.scheduleEdge(test1, service);
		assertTrue(service.contains(test1.getId()));
		assertEquals(3, service.size());
	}

	@Test
	public void testBScheduleTask() {
		Edge test2 = this.getEdge(this.edge2Id);
		test2.addUser(new EdgeUser(200, test2.getId(), "user1",  0, null));
		test2.addUser(new EdgeUser(201, test2.getId(), "user2", 90, null));
		test2.addUser(new EdgeUser(202, test2.getId(), "user5",  1, null));
		// Test that action of EdgeUser-200 doesn't auto trigger
		// because a timeToWait=0 equals 'OFF'
		assertFalse(actionHasTriggered.get());

		// Test schedule test2
		this.scheduleEdge(test2, service);
		assertTrue(service.contains(test2.getId()));
		
		// Test that action of EdgeUser-202 does trigger
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(actionHasTriggered.get());
		
		assertEquals(4, service.size());
	}

	@Test
	public void testCRemoveAll() {
		assertTrue(service.contains(this.edge1Id));
		service.removeAll(this.edge1Id);
		assertFalse(service.contains(this.edge1Id));
		assertTrue(service.contains(this.edge2Id));
	}

	@Test
	public void testDStop() {
		service.stop();
		assertFalse(service.contains(this.edge2Id));
		assertTrue(service.isEmpty());
	}

	private void scheduleEdge(Edge edge, ScheduleMessageService service) {
		service.createTask(this.getMap(edge, ZonedDateTime.now()), edge.getId());
	}

	private Map<ZonedDateTime, List<EdgeUser>> getMap(Edge edge, ZonedDateTime now) {
		Map<ZonedDateTime, List<EdgeUser>> edgeUsers = new TreeMap<>();
		edge.getUser().forEach(user -> {
			ZonedDateTime notifyStamp = now.plusSeconds(user.getTimeToWait());

			edgeUsers.putIfAbsent(notifyStamp, new ArrayList<>());
			edgeUsers.get(notifyStamp).add(user);
		});
		return edgeUsers;
	}

	private Edge getEdge(String id) {
		return new Edge(null, id, "", null, "", "", null, null, null, null);
	}

}
