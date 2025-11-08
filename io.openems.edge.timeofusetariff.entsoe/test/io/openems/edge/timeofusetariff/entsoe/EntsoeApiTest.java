package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.edge.timeofusetariff.entsoe.Utils.parsePrices;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.exceptions.OpenemsException;

public class EntsoeApiTest {

	@Test
	@Ignore
	public void testQueryingEntsoeThroughBridgeHttp() throws OpenemsException, Exception {
		var token = ""; // Fill personal security token and remove 'Ignore' tag while testing.
		var biddingZone = BiddingZone.GERMANY;
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withZoneSameLocal(ZoneId.systemDefault());
		var toDate = fromDate.plusDays(1);

		var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::networkEndpointFetcher,
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true) // handle immediately
		);

		var bridgeHttp = bridgeHttpFactory.get();

		try {
			var endpoint = EntsoeApi.createEndPoint(biddingZone, token, fromDate, toDate);
			var response = bridgeHttp.request(endpoint).get();

			var xml = response.data();
			var quarterlyPrices = parsePrices(xml, Resolution.QUARTERLY, biddingZone);

			quarterlyPrices.entrySet().forEach(System.out::println);
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}
}
