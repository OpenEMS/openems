package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.ChannelCategory.OPENEMS_TYPE;
import static io.openems.common.channel.PersistencePriority.VERY_LOW;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.modbus.sunspec.Point.ChannelIdPoint;

public class PointTest {

	@Test
	public void testChannelIdPoint() {
		var point = (ChannelIdPoint) DefaultSunSpecModel.S111.A.get();
		var channelId = point.channelId;
		var doc = channelId.doc();
		assertEquals(READ_ONLY, doc.getAccessMode());
		assertEquals(OPENEMS_TYPE, doc.getChannelCategory());
		assertEquals(VERY_LOW, doc.getPersistencePriority());
		assertEquals("Amps. AC Current", doc.getText());
		assertEquals(INTEGER, doc.getType());
		assertEquals(AMPERE, doc.getUnit());
	}

}
