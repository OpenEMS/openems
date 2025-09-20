package io.openems.common.types;

import org.junit.Assert;
import org.junit.Test;

public class ChannelAddressTest {

	@Test
	public void test() {
		var ess0ActivePower = new ChannelAddress("ess0", "ActivePower");
		var ess0ReactivePower = new ChannelAddress("ess0", "ReactivePower");
		var meter0ActivePower = new ChannelAddress("meter0", "ActivePower");
		var meter1ActivePower = new ChannelAddress("meter1", "ActivePower");
		var meter1ReactivePower = new ChannelAddress("meter1", "ReactivePower");
		var anyActivePower = new ChannelAddress("*", "ActivePower");
		var anyMeterActivePower = new ChannelAddress("meter*", "ActivePower");
		var anyPower = new ChannelAddress("*", "*Power");

		Assert.assertEquals(0, ChannelAddress.match(ess0ActivePower, ess0ActivePower));
		Assert.assertEquals(-1, ChannelAddress.match(ess0ActivePower, ess0ReactivePower));
		Assert.assertEquals(Integer.MAX_VALUE / 2 + "*".length(),
				ChannelAddress.match(ess0ActivePower, anyActivePower));
		Assert.assertEquals(Integer.MAX_VALUE / 2 + "meter*".length(),
				ChannelAddress.match(meter0ActivePower, anyMeterActivePower));
		Assert.assertEquals(Integer.MAX_VALUE / 2 + "meter*".length(),
				ChannelAddress.match(meter1ActivePower, anyMeterActivePower));
		Assert.assertEquals("*".length() + "*Power".length(), ChannelAddress.match(meter1ActivePower, anyPower));
		Assert.assertEquals("*".length() + "*Power".length(), ChannelAddress.match(meter1ReactivePower, anyPower));
	}

}
