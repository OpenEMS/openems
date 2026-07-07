package io.openems.edge.timeofusetariff.manual.octopus.heat;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static org.junit.Assert.assertFalse;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TouOctopusHeatImplTest {

	@Test
	public void testWithEmptyAncillaryCosts() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2025-10-31T00:00:00Z"), ZoneId.systemDefault());
		var sut = new TouOctopusHeatImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setHighPrice(0.35) //
						.setStandardPrice(0.30) //
						.setLowPrice(0.20) //
						.setAncillaryCosts("") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();

		final var prices = sut.getPrices();
		assertFalse("Prices should not be empty for DSO ", prices.isEmpty());
	}

	@Test
	public void testWithDifferentProviders() throws Exception {
		testWithProvider("BAYERNWERK");
		testWithProvider("NETZE_BW");
		testWithProvider("EWE_NETZ");
		testWithProvider("WEST_NETZ");
	}

	private static void testWithProvider(String dso) throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2025-10-31T00:00:00Z"), ZoneId.systemDefault());
		var sut = new TouOctopusHeatImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setHighPrice(0.35) //
						.setStandardPrice(0.30) //
						.setLowPrice(0.20) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", dso) //
								.build() //
								.toString())
						.build()) //
				.next(new TestCase())//
				.deactivate();

		var prices = sut.getPrices();
		assertFalse("Prices should not be empty for DSO " + dso, prices.isEmpty());
	}

}
