package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dhatim.fastexcel.Workbook;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse.Channel;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse.XlsxUtils;
import io.openems.common.types.ChannelAddress;

public class QueryHistoricTimeseriesExportXlsxResponseTest {

	private static SortedMap<ChannelAddress, JsonElement> getMockedEnergyData() {
		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();

		values.put(Channel.GRID_BUY_ACTIVE_ENERGY, new JsonPrimitive(500));
		values.put(Channel.GRID_SELL_ACTIVE_ENERGY, new JsonPrimitive(0));
		values.put(Channel.PRODUCTION_ACTIVE_ENERGY, new JsonPrimitive(300));
		values.put(Channel.CONSUMPTION_ACTIVE_ENERGY, new JsonPrimitive(700));
		values.put(Channel.ESS_DC_CHARGE_ENERGY, new JsonPrimitive(100));
		values.put(Channel.ESS_DC_DISCHARGE_ENERGY, new JsonPrimitive(80));

		return values;
	}

	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> getMockedPowerData() {

		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
		values.put(Channel.GRID_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.PRODUCTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.CONSUMPTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.ESS_DISCHARGE_POWER, new JsonPrimitive(50));
		values.put(Channel.ESS_SOC, new JsonPrimitive(50));

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();
		result.put(ZonedDateTime.of(2020, 07, 01, 0, 15, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values);

		return result;
	}

	private byte[] generateXlsxFile() throws OpenemsNamedException, IOException {
		var fromDate = ZonedDateTime.of(2020, 07, 01, 0, 0, 0, 0, ZoneId.systemDefault());
		var toDate = ZonedDateTime.of(2020, 07, 02, 0, 0, 0, 0, ZoneId.systemDefault());

		var powerData = QueryHistoricTimeseriesExportXlsxResponseTest.getMockedPowerData();

		var energyData = QueryHistoricTimeseriesExportXlsxResponseTest.getMockedEnergyData();

		final byte[] result;

		try (var os = new ByteArrayOutputStream()) {
			var workbook = new Workbook(os, "Historic data", null);
			var ws = workbook.newWorksheet("Export");

			Locale currentLocale = new Locale("en", "EN");

			var translationBundle = ResourceBundle.getBundle("io.openems.common.jsonrpc.response.translation",
					currentLocale);

			XlsxUtils.addBasicInfo(ws, "0", fromDate, toDate, translationBundle);
			XlsxUtils.addEnergyData(ws, energyData, translationBundle);
			XlsxUtils.addPowerData(ws, powerData, translationBundle);

			workbook.finish();
			os.flush();

			result = os.toByteArray();

			os.close();
		}
		return result;
	}

	@Test
	public void test() throws IOException, OpenemsNamedException {
		var content = this.generateXlsxFile();

		var expectedPayload = "UEsDBC0ACAAIAAAAAAAAAAAAAAAAAAAAAAAYAAAAeGwvd29ya3".substring(0, 50);
		var actualPayload = Base64.getEncoder().encodeToString(content).substring(0, 50);

		Assert.assertEquals(expectedPayload, actualPayload);
	}

	/**
	 * Use this "test" to write an actual Xlsx file with dummy content.
	 *
	 * @throws IOException           on error
	 * @throws OpenemsNamedException on error
	 */
	// @Test
	public void writeToTempFile() throws IOException, OpenemsNamedException {
		var content = this.generateXlsxFile();

		var rootPath = System.getProperty("user.home");
		try (OutputStream outputStream = new FileOutputStream(rootPath + "\\testExcel.xlsx")) {
			outputStream.write(content);
		}
	}
}
