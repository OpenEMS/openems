package io.openems.edge.timeofusetariff.manual.fixed.gridsell;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.DateUtils.QUARTERS_PER_DAY;
import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

public class TariffManualFixedGridSellImplTest {

	@Test
	public void testGetGridSellPrices_ShouldReturnFixedPrices() throws Exception {
		final var clock = createDummyClock();
		final var sut = new TariffManualFixedGridSellImpl();
		new ComponentTest(sut) //
				.addReference("meta", new DummyMeta()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tariffGridSell0") //
						.setFixedGridSellPrice(75.0) //
						.build());

		final var prices = sut.getGridSellPrices();
		assertEquals(Instant.now(clock), prices.getFirstTime());
		assertEquals(QUARTERS_PER_DAY * 2, prices.asArray().length);
		for (var price : prices.asArray()) {
			assertEquals(75.0, price, 0.0);
		}
	}
}
