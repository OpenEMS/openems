package io.openems.edge.ess.kostal.piko;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.openems.edge.ess.kostal.piko.EssKostalPiko.ChannelId;

public class ReadTasksManagerTest {

	@Test
	public void testNextReadTasks() {
		ReadTask o1 = new ReadTask(ChannelId.INVERTER_NAME, Priority.ONCE, FieldType.STRING, 0x01000300);
		ReadTask o2 = new ReadTask(ChannelId.ARTICLE_NUMBER, Priority.ONCE, FieldType.STRING, 0x01000100);
		ReadTask l1 = new ReadTask(ChannelId.FEED_IN_STATUS, Priority.LOW, FieldType.BOOLEAN, 0x01000A00);
		ReadTask l2 = new ReadTask(ChannelId.OVERALL_DC_CURRENT, Priority.LOW, FieldType.FLOAT, 0x02000100);
		ReadTask l3 = new ReadTask(ChannelId.DC_CURRENT_STRING_1, Priority.LOW, FieldType.FLOAT, 0x02000301);
		ReadTask h1 = new ReadTask(ChannelId.BATTERY_SOC, Priority.HIGH, FieldType.FLOAT, 0x02000704);
		ReadTask h2 = new ReadTask(ChannelId.AC_VOLTAGE_L1, Priority.HIGH, FieldType.FLOAT, 0x04000102);
		ReadTask h3 = new ReadTask(ChannelId.AC_VOLTAGE_L2, Priority.HIGH, FieldType.FLOAT, 0x04000302);

		ReadTasksManager m = new ReadTasksManager(o1, o2, l1, l2, l3, h1, h2, h3);
		
		List<ReadTask> t1 = m.getNextReadTasks();
		assertEquals(5, t1.size());
		assertTrue(t1.contains(h1));
		assertTrue(t1.contains(h2));
		assertTrue(t1.contains(h3));
		assertTrue(t1.contains(o1));
		assertTrue(t1.contains(l1));
		
		List<ReadTask> t2 = m.getNextReadTasks();
		assertEquals(5, t2.size());
		assertTrue(t2.contains(h1));
		assertTrue(t2.contains(h2));
		assertTrue(t2.contains(h3));
		assertTrue(t2.contains(o2));
		assertTrue(t2.contains(l2));
		
		List<ReadTask> t3 = m.getNextReadTasks();
		assertEquals(4, t3.size());
		assertTrue(t3.contains(h1));
		assertTrue(t3.contains(h2));
		assertTrue(t3.contains(h3));
		assertTrue(t3.contains(l3));
		
		List<ReadTask> t4 = m.getNextReadTasks();
		assertEquals(4, t4.size());
		assertTrue(t4.contains(h1));
		assertTrue(t4.contains(h2));
		assertTrue(t4.contains(h3));
		assertTrue(t4.contains(l1));

	}

}
