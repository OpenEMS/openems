package io.openems.edge.controller.evse.single.jsonrpc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TestUtils;

public class GetScheduleTest {

	@Test
	public void test() {
		var now = TestUtils.createDummyClock().now();
		var serializer = new GetSchedule().getResponseSerializer();
		var p0 = new GetSchedule.Response.Period(now, 0.1234, 5, 6, 7, 8, 9);
		var r = new GetSchedule.Response(ImmutableList.of(p0));
		var json = serializer.serialize(r);
		assertEquals(r, serializer.deserialize(json));
	}

}
