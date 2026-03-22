package io.openems.edge.bosch.bpts5hybrid.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class BoschBpts5HybridApiClientTest {

	private BoschBpts5HybridApiClient sut;

	@Before
	public void setUp() {
		this.sut = new BoschBpts5HybridApiClient();
	}

	@Test
	public void testInitialState() {
		assertFalse(this.sut.isConnected());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentStromAusNetz());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentVerbrauchVonPv());
	}

	@Test
	public void testProcessConnectResponse() throws OpenemsException {
		var body = "some html content WUI_SID='123456789012345' more content";
		this.sut.processConnectResponse(body);
		assertTrue(this.sut.isConnected());
	}

	@Test(expected = OpenemsException.class)
	public void testProcessConnectResponseMissingWuiSid() throws OpenemsException {
		this.sut.processConnectResponse("some html without the session id");
	}

	@Test
	public void testGetValuesUrl() throws OpenemsException {
		this.sut.processConnectResponse("prefix WUI_SID='123456789012345' suffix");
		var url = this.sut.getValuesUrl("http://192.168.1.1");
		assertEquals("http://192.168.1.1" + BoschBpts5HybridApiClient.GET_VALUES_URL_PART + "123456789012345", url);
	}

	@Test
	public void testGetBatteryStatusUrl() throws OpenemsException {
		this.sut.processConnectResponse("prefix WUI_SID='123456789012345' suffix");
		var url = this.sut.getBatteryStatusUrl("http://192.168.1.1");
		assertTrue(url.startsWith("http://192.168.1.1" + BoschBpts5HybridApiClient.GET_VALUES_URL_PART));
		assertTrue(url.contains("action=get.logbookview"));
	}

	@Test
	public void testGetPostRequestData() {
		assertEquals("action=get.hyb.overview&flow=1", BoschBpts5HybridApiClient.getPostRequestData());
	}

	@Test
	public void testProcessValuesResponseWithAllFields() throws Exception {
		// Format: pipe-separated values, indices: [2]=pvPower, [3]=soc, [10]=chargePower,
		// [11]=einspeisung, [12]=verbrauchVonPv, [13]=verbrauchVonBatterie, [14]=stromAusNetz
		var body = "x|x|1.5kW|85|x|x|x|x|x|x|0.8kW|0.3kW|0.5kW|0.2kW|0.1kW";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(1500), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(85), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(800), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(300), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(500), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(200), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(100), this.sut.getCurrentStromAusNetz());
	}

	@Test
	public void testProcessValuesResponseWithLessThan15Fields() throws Exception {
		// Only 14 fields (index 0-13), no stromAusNetz
		var body = "x|x|2.0kW|50|x|x|x|x|x|x|1.0kW|0.5kW|0.3kW|0.4kW";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(2000), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(50), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(1000), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(500), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(300), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(400), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentStromAusNetz());
	}

	@Test
	public void testProcessValuesResponseWithNbsp() throws Exception {
		// Fields containing nbsp; should be treated as 0
		var body = "x|x|&nbsp;|42|x|x|x|x|x|x|&nbsp;|&nbsp;|&nbsp;|&nbsp;|&nbsp;";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(0), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(42), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentStromAusNetz());
	}

	@Test
	public void testProcessValuesResponseWithKwSuffix() throws Exception {
		var body = "x|x|3.5kW|100|x|x|x|x|x|x|2.0kW|1.0kW|0.7kW|0.6kW|0.4kW";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(3500), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(100), this.sut.getCurrentSoc());
	}

	@Test
	public void testProcessValuesResponseSessionInvalid() throws Exception {
		// First connect
		this.sut.processConnectResponse("prefix WUI_SID='123456789012345' suffix");
		assertTrue(this.sut.isConnected());

		// Session invalid should force reconnect
		this.sut.processValuesResponse("session invalid");
		assertFalse(this.sut.isConnected());
	}

	@Test
	public void testProcessValuesResponseZeroValues() throws Exception {
		var body = "x|x|0.0kW|0|x|x|x|x|x|x|0.0kW|0.0kW|0.0kW|0.0kW|0.0kW";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(0), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentStromAusNetz());
	}

	@Test
	public void testProcessBatteryStatusResponseOk() {
		var body = "<html><body><table><tr><td>Keine Störung</td></tr></table></body></html>";
		assertEquals(0, this.sut.processBatteryStatusResponse(body));
	}

	@Test
	public void testProcessBatteryStatusResponseError() {
		var body = "<html><body><table><tr><td>Störung: Batteriefehler</td></tr></table></body></html>";
		assertEquals(1, this.sut.processBatteryStatusResponse(body));
	}

	@Test
	public void testProcessBatteryStatusResponseNoFault() {
		var body = "<html><body><table><tr><td>Alles OK</td></tr></table></body></html>";
		assertEquals(0, this.sut.processBatteryStatusResponse(body));
	}

	@Test
	public void testIsConnectedInitiallyFalse() {
		assertFalse(this.sut.isConnected());
	}

	@Test
	public void testIsConnectedAfterConnect() throws OpenemsException {
		this.sut.processConnectResponse("prefix WUI_SID='ABCDEFGHIJKLMNO' suffix");
		assertTrue(this.sut.isConnected());
	}

	@Test
	public void testProcessValuesResponseEmptyFields() throws Exception {
		// Empty/whitespace watt values should be treated as 0
		var body = "x|x| |75|x|x|x|x|x|x| |  |   | | ";
		this.sut.processValuesResponse(body);

		assertEquals(Integer.valueOf(0), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(75), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(0), this.sut.getCurrentStromAusNetz());
	}

	@Test
	public void testMultipleValueUpdates() throws Exception {
		// First update
		var body1 = "x|x|1.0kW|50|x|x|x|x|x|x|0.5kW|0.3kW|0.2kW|0.1kW|0.4kW";
		this.sut.processValuesResponse(body1);
		assertEquals(Integer.valueOf(1000), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(50), this.sut.getCurrentSoc());

		// Second update should overwrite
		var body2 = "x|x|2.0kW|80|x|x|x|x|x|x|1.0kW|0.6kW|0.4kW|0.3kW|0.8kW";
		this.sut.processValuesResponse(body2);
		assertEquals(Integer.valueOf(2000), this.sut.getCurrentPvProduction());
		assertEquals(Integer.valueOf(80), this.sut.getCurrentSoc());
		assertEquals(Integer.valueOf(1000), this.sut.getCurrentChargePower());
		assertEquals(Integer.valueOf(600), this.sut.getCurrentEinspeisung());
		assertEquals(Integer.valueOf(400), this.sut.getCurrentVerbrauchVonPv());
		assertEquals(Integer.valueOf(300), this.sut.getCurrentDischargePower());
		assertEquals(Integer.valueOf(800), this.sut.getCurrentStromAusNetz());
	}
}
