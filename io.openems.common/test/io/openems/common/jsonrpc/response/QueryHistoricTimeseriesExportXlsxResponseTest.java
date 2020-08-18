package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public class QueryHistoricTimeseriesExportXlsxResponseTest {

	private SortedMap<ChannelAddress, JsonElement> getMockedEnergyData() {
		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();

		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_BUY_ENERGY, new JsonPrimitive(500));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_SELL_ENERGY, new JsonPrimitive(0));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.PRODUCTION_ACTIVE_ENERGY, new JsonPrimitive(300));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.CONSUMPTION_ACTIVE_ENERGY, new JsonPrimitive(700));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_ACTIVE_ENERGY, new JsonPrimitive(100));

		return values;
	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> getMockedPowerData() {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();

		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
		values.put(QueryHistoricTimeseriesExportXlsxResponse.GRID_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.PRODUCTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.CONSUMPTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(QueryHistoricTimeseriesExportXlsxResponse.ESS_SOC, new JsonPrimitive(50));

		result.put(ZonedDateTime.of(2020, 07, 01, 0, 15, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values);

		return result;
	}

	// @Test
	public void test() throws IOException, OpenemsNamedException {
		ZonedDateTime fromDate = ZonedDateTime.of(2020, 07, 01, 0, 0, 0, 0, ZoneId.systemDefault());
		ZonedDateTime toDate = ZonedDateTime.of(2020, 07, 02, 0, 0, 0, 0, ZoneId.systemDefault());

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> powerData = getMockedPowerData();

		SortedMap<ChannelAddress, JsonElement> energyData = getMockedEnergyData();

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook workbook = new Workbook(os, "Historic data", "1.0");
			Worksheet ws = workbook.newWorksheet("Sheet1");

			QueryHistoricTimeseriesExportXlsxResponse.addBasicInfo(ws, "0", fromDate, toDate);
			QueryHistoricTimeseriesExportXlsxResponse.addEnergyData(ws, energyData);
			QueryHistoricTimeseriesExportXlsxResponse.addPowerData(ws, powerData);

			workbook.finish();
			os.flush();

			String rootPath = System.getProperty("user.home");
			try (OutputStream outputStream = new FileOutputStream(rootPath + "\\testExcel.xlsx")) {
				os.writeTo(outputStream);
			}

			os.close();
		}
	}
}
