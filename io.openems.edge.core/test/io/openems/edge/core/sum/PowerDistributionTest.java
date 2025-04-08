package io.openems.edge.core.sum;

import static io.openems.edge.core.sum.PowerDistribution.EMPTY;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PowerDistributionTest {

	@Test
	public void test() {
		assertEquals(EMPTY, PowerDistribution.of(null, null, null));
		assertEquals(EMPTY, PowerDistribution.of(-1, null, null));
		assertEquals(EMPTY, PowerDistribution.of(0, -1, 0));
		{
			var sut = PowerDistribution.of(1000, null, null);
			assertEquals(1000, sut.gridToConsumption().intValue());
		}
		{
			var sut = PowerDistribution.of(-1000, 2000, 500);
			assertEquals(1000, sut.productionToConsumption().intValue());
			assertEquals(1000, sut.productionToGrid().intValue());
			assertEquals(0, sut.productionToEss().intValue());
			assertEquals(0, sut.gridToConsumption().intValue());
			assertEquals(500, sut.essToConsumption().intValue());
			assertEquals(0, sut.gridToEss().intValue());
		}
		{
			var sut = PowerDistribution.of(1000, 2000, 500);
			assertEquals(2000, sut.productionToConsumption().intValue());
			assertEquals(0, sut.productionToGrid().intValue());
			assertEquals(0, sut.productionToEss().intValue());
			assertEquals(1000, sut.gridToConsumption().intValue());
			assertEquals(500, sut.essToConsumption().intValue());
			assertEquals(0, sut.gridToEss().intValue());
		}
		{
			var sut = PowerDistribution.of(1000, 2000, -500);
			assertEquals(1500, sut.productionToConsumption().intValue());
			assertEquals(0, sut.productionToGrid().intValue());
			assertEquals(500, sut.productionToEss().intValue());
			assertEquals(1000, sut.gridToConsumption().intValue());
			assertEquals(0, sut.essToConsumption().intValue());
			assertEquals(0, sut.gridToEss().intValue());
		}
	}
}
