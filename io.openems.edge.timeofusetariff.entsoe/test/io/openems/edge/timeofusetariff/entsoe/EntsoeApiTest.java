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
		var token = "";
		var areaCode = BiddingZone.SWEDEN_ZONE_3.getName();
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withZoneSameLocal(ZoneId.systemDefault());
		var toDate = fromDate.plusDays(1);
		var response = EntsoeApi.query(token, areaCode, fromDate, toDate);

		System.out.println(response);
	}
}
