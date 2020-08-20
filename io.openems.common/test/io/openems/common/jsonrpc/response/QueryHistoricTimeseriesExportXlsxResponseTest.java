package io.openems.common.jsonrpc.response;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public class QueryHistoricTimeseriesExportXlsxResponseTest {

	private static SortedMap<ChannelAddress, JsonElement> getMockedEnergyData() {
		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();

		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_BUY_ACTIVE_ENERGY, new JsonPrimitive(500));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_SELL_ACTIVE_ENERGY, new JsonPrimitive(0));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.PRODUCTION_ACTIVE_ENERGY, new JsonPrimitive(300));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.CONSUMPTION_ACTIVE_ENERGY, new JsonPrimitive(700));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_DC_CHARGE_ENERGY, new JsonPrimitive(100));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_DC_DISCHARGE_ENERGY, new JsonPrimitive(80));

		return values;
	}

	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> getMockedPowerData() {

		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.PRODUCTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.CONSUMPTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_DISCHARGE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_SOC, new JsonPrimitive(50));

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();
		result.put(ZonedDateTime.of(2020, 07, 01, 0, 15, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values);

		return result;
	}

	private byte[] generateXlsxFile() throws OpenemsNamedException, IOException {
		ZonedDateTime fromDate = ZonedDateTime.of(2020, 07, 01, 0, 0, 0, 0, ZoneId.systemDefault());
		ZonedDateTime toDate = ZonedDateTime.of(2020, 07, 02, 0, 0, 0, 0, ZoneId.systemDefault());

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> powerData = getMockedPowerData();

		SortedMap<ChannelAddress, JsonElement> energyData = getMockedEnergyData();

		final byte[] result;

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook workbook = new Workbook(os, "Historic data", null);
			Worksheet ws = workbook.newWorksheet("Export");

			QueryHistoricTimeseriesExportXlsxResponse.addBasicInfo(ws, "0", fromDate, toDate);
			QueryHistoricTimeseriesExportXlsxResponse.addEnergyData(ws, energyData);
			QueryHistoricTimeseriesExportXlsxResponse.addPowerData(ws, powerData);

			workbook.finish();
			os.flush();

			result = os.toByteArray();

			os.close();
		}
		return result;
	}

	@Test
	public void test() throws IOException, OpenemsNamedException {
		byte[] content = this.generateXlsxFile();

		String expectedPayload = "UEsDBC0ACAAIAAAAAAAAAAAAAAAAAAAAAAAYAAAAeGwvd29ya3".substring(0, 50);
		String actualPayload = Base64.getEncoder().encodeToString(content).substring(0, 50);

		assertEquals(expectedPayload, actualPayload);
	}

	/**
	 * Use this "test" to write an actual Xlsx file with dummy content.
	 * 
	 * @throws IOException           on error
	 * @throws OpenemsNamedException on error
	 */
	// @Test
	public void writeToTempFile() throws IOException, OpenemsNamedException {
		byte[] content = this.generateXlsxFile();

		String rootPath = System.getProperty("user.home");
		try (OutputStream outputStream = new FileOutputStream(rootPath + "\\testExcel.xlsx")) {
			outputStream.write(content);
		}
	}
}
