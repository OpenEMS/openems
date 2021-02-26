package io.openems.common.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChannelAddressTest {

	@Test
	public void test() {
		ChannelAddress ess0ActivePower = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress ess0ReactivePower = new ChannelAddress("ess0", "ReactivePower");
		ChannelAddress meter0ActivePower = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress meter1ActivePower = new ChannelAddress("meter1", "ActivePower");
		ChannelAddress meter1ReactivePower = new ChannelAddress("meter1", "ReactivePower");
		ChannelAddress anyActivePower = new ChannelAddress("*", "ActivePower");
		ChannelAddress anyMeterActivePower = new ChannelAddress("meter*", "ActivePower");
		ChannelAddress anyPower = new ChannelAddress("*", "*Power");

		assertEquals(0, ChannelAddress.match(ess0ActivePower, ess0ActivePower));
		assertEquals(-1, ChannelAddress.match(ess0ActivePower, ess0ReactivePower));
		assertEquals(Integer.MAX_VALUE / 2 + "*".length(), ChannelAddress.match(ess0ActivePower, anyActivePower));
		assertEquals(Integer.MAX_VALUE / 2 + "meter*".length(),
				ChannelAddress.match(meter0ActivePower, anyMeterActivePower));
		assertEquals(Integer.MAX_VALUE / 2 + "meter*".length(),
				ChannelAddress.match(meter1ActivePower, anyMeterActivePower));
		assertEquals("*".length() + "*Power".length(), ChannelAddress.match(meter1ActivePower, anyPower));
		assertEquals("*".length() + "*Power".length(), ChannelAddress.match(meter1ReactivePower, anyPower));
	}

}
