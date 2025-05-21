package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class EntsoeApiTest {

	@Test
	@Ignore
	public void testQuery() throws IOException, ParserConfigurationException, SAXException {
		var token = ""; // Fill personal security token and remove 'Ignore' tag while testing.
		var areaCode = BiddingZone.SWEDEN_SE3.code;
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withZoneSameLocal(ZoneId.systemDefault());
		var toDate = fromDate.plusDays(1);
		EntsoeApi.query(token, areaCode, fromDate, toDate);
	}
}
