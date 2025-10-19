package io.openems.edge.pvinverter.kostal.piko;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class PvInverterKostalPikoImplTest {

	private static final String COMPONENT_ID = "pvinverter0";

	private static final String SAMPLE_HTML = """
			<html><head>
			<title>PV Webserver</title>
			</head>
			<body>
			<table cellspacing="0" cellpadding="0" width="770">
			<tr>
			<td>aktuell</td>
			<td align="right" bgcolor="#FFFFFF">1234</td>
			<td>&nbsp; W</td>
			<td width="100">Gesamtenergie</td>
			<td bgcolor="#CCCCCC">137491</td>
			<td width="50">&nbsp; kWh</td>
			</tr>
			<tr>
			<td width="100">Tagesenergie</td>
			<td width="70" align="left">42</td>
			<td>&nbsp; kWh</td>
			</tr>
			<tr>
			<td width="100">Status</td>
			<td colspan="4">Einspeisen MPP</td>
			</tr>
			<tr><td colspan="7"><b>PV-Generator</b></td></tr>
			<tr>
			<td width="100"><u>String 1</u></td>
			<td width="100"><u>L1</u></td>
			</tr>
			<tr>
			<td>Spannung</td>
			<td width="70" align="right">400</td>
			<td width="140">&nbsp; V</td>
			<td width="100">Spannung</td>
			<td bgcolor="#FFFFFF">230</td>
			<td>&nbsp; V</td>
			</tr>
			<tr>
			<td width="100">Strom</td>
			<td align="right" bgcolor="#EEEEEE">2.5</td>
			<td width="140">&nbsp; A</td>
			<td>Leistung</td>
			<td width="70" align="center">500</td>
			<td width="30">&nbsp; W</td>
			</tr>
			<tr>
			<td width="100"><u>String 2</u></td>
			<td width="100"><u>L2</u></td>
			</tr>
			<tr>
			<td width="100">Spannung</td>
			<td width="70">410</td>
			<td>&nbsp; V</td>
			<td>Spannung</td>
			<td width="70" align="right" bgcolor="#FFFFFF">232</td>
			<td width="30">&nbsp; V</td>
			</tr>
			<tr>
			<td>Strom</td>
			<td width="70" align="right" bgcolor="#FFFFFF">2.6</td>
			<td alight="left">&nbsp; A</td>
			<td width="-100">Leistung</td>
			<td bgcolor="#DDDDDD">400</td>
			<td>W</td>
			</tr>
			<tr>
			<td width="100"><u>String 3</u></td>
			<td width="100"><u>L3</u></td>
			</tr>
			<tr>
			<td width="100">Spannung</td>
			<td align="right">415</td>
			<td width="140">&nbsp; V</td>
			<td width="100">Spannung</td>
			<td width="70" bgcolor="#FFFFFF">231</td>
			<td>&nbsp; V</td>
			</tr>
			<tr>
			<td>Strom</td>
			<td width="70" align="right">2.7</td>
			<td>&nbsp; A</td>
			<td width="100">Leistung</td>
			<td>334</td>
			<td width="30">&nbsp; W</td>
			</tr>
			</table>
			</body></html>
			""";

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		// Pre-set the response for the initial request
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(SAMPLE_HTML));

		new ComponentTest(new PvInverterKostalPikoImpl()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setAlias("") //
						.setEnabled(true) //
						.setUrl("http://192.168.1.100") //
						.setUsername("pvserver") //
						.setPassword("pvwr") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(
								() -> httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(SAMPLE_HTML))) //
						.onExecuteWriteCallbacks(() -> httpTestBundle.triggerNextCycle()) //
				) //
				.next(new TestCase() //
						.output(PvInverterKostalPiko.ChannelId.DAY_YIELD, 42L) //
						.output(PvInverterKostalPiko.ChannelId.STATUS, "Einspeisen MPP") //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_VOLTAGE, 400) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_CURRENT, 2500) // 2.5A -> 2500mA
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_POWER, 1000) // 400V * 2.5A = 1000W
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_VOLTAGE, 410) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_CURRENT, 2600) // 2.6A -> 2600mA
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_POWER, 1066) // 410V * 2.6A = 1066W
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_VOLTAGE, 415) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_CURRENT, 2700) // 2.7A -> 2700mA
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_POWER, 1120) // 415V * 2.7A = 1120.5W -> 1120W
				);
	}

	@Test
	public void testWithNoData() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		final String noDataHtml = """
				<html><body>
				<table>
				<tr><td>aktuell</td><td>x x x</td><td> W</td></tr>
				<tr><td>Gesamtenergie</td><td bgcolor="#FFFFFF">x x x</td><td> kWh</td></tr>
				<tr><td>Tagesenergie</td><td align="center">x x x</td><td> kWh</td></tr>
				<tr><td>Spannung</td><td>x x x</td><td> V</td></tr>
				<tr><td>Spannung</td><td width="50">x x x</td><td> V</td></tr>
				<tr><td>Strom</td><td>x x x</td><td> A</td></tr>
				<tr><td>Leistung</td><td bgcolor="#EEEEEE">x x x</td><td> W</td></tr>
				<tr><td>Spannung</td><td>x x x</td><td> V</td></tr>
				<tr><td>Spannung</td><td align="right">x x x</td><td> V</td></tr>
				<tr><td>Strom</td><td>x x x</td><td> A</td></tr>
				<tr><td>Leistung</td><td>x x x</td><td> W</td></tr>
				<tr><td>Spannung</td><td width="70" bgcolor="#CCCCCC">x x x</td><td> V</td></tr>
				<tr><td>Spannung</td><td>x x x</td><td> V</td></tr>
				<tr><td>Strom</td><td>x x x</td><td> A</td></tr>
				<tr><td>Leistung</td><td align="left">x x x</td><td> W</td></tr>
				</table>
				</body></html>
				""";

		// Pre-set the response for the initial request
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(noDataHtml));

		new ComponentTest(new PvInverterKostalPikoImpl()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setAlias("") //
						.setEnabled(true) //
						.setUrl("http://192.168.1.100") //
						.setUsername("pvserver") //
						.setPassword("pvwr") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(
								() -> httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(noDataHtml))) //
						.onExecuteWriteCallbacks(() -> httpTestBundle.triggerNextCycle()) //
				) //
				.next(new TestCase() //
						.output(PvInverterKostalPiko.ChannelId.DAY_YIELD, null) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_VOLTAGE, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_CURRENT, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING1_POWER, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_VOLTAGE, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_CURRENT, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING2_POWER, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_VOLTAGE, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_CURRENT, 0) //
						.output(PvInverterKostalPiko.ChannelId.DC_STRING3_POWER, 0) //
				);
	}
}