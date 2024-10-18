package io.openems.common.jsonrpc.response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse.Channel;
import io.openems.common.session.Language;
import io.openems.common.timedata.XlsxExportDetailData;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.CurrencyConfig;

public class CreateXlxsTest {

	private static SortedMap<ChannelAddress, JsonElement> getMockedEnergyData() {
		return ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
				.put(Channel.GRID_BUY_ACTIVE_ENERGY, new JsonPrimitive(500)) //
				.put(Channel.GRID_SELL_ACTIVE_ENERGY, new JsonPrimitive(0)) //
				.put(Channel.PRODUCTION_ACTIVE_ENERGY, new JsonPrimitive(300)) //
				.put(Channel.CONSUMPTION_ACTIVE_ENERGY, new JsonPrimitive(700)) //
				.put(Channel.ESS_DC_CHARGE_ENERGY, new JsonPrimitive(100)) //
				.put(Channel.ESS_DC_DISCHARGE_ENERGY, new JsonPrimitive(80)) //
				.build();
	}

	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> getMockedPowerData() {

		SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
		values.put(Channel.GRID_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.PRODUCTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.CONSUMPTION_ACTIVE_POWER, new JsonPrimitive(50));
		values.put(Channel.ESS_DISCHARGE_POWER, new JsonPrimitive(50));
		values.put(Channel.ESS_SOC, new JsonPrimitive(50));
		values.put(new ChannelAddress("meter0", "ActivePower"), new JsonPrimitive(100));
		values.put(new ChannelAddress("meter1", "ActivePower"), new JsonPrimitive(412));
		values.put(new ChannelAddress("evcs0", "ChargePower"), new JsonPrimitive(75));
		values.put(new ChannelAddress("meter2", "ActivePower"), new JsonPrimitive(10));
		values.put(new ChannelAddress("_sum", "GridBuyPower"), new JsonPrimitive(292.5));

		return ImmutableSortedMap.<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>naturalOrder()
				.put(ZonedDateTime.of(2020, 07, 01, 0, 15, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.put(ZonedDateTime.of(2020, 07, 01, 0, 30, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.put(ZonedDateTime.of(2020, 07, 01, 0, 45, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.put(ZonedDateTime.of(2020, 07, 01, 1, 0, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.put(ZonedDateTime.of(2020, 07, 01, 1, 15, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.put(ZonedDateTime.of(2020, 07, 01, 1, 30, 0, 0, ZoneId.systemDefault()).plusMinutes(15), values) //
				.build();
	}

	private static XlsxExportDetailData getMockedDetailData() {
		final var enumMap = new EnumMap<XlsxExportCategory, List<XlsxExportDataEntry>>(XlsxExportCategory.class);
		final var consumption = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var production = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var tou = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();

		enumMap.put(XlsxExportCategory.PRODUCTION, production);
		enumMap.put(XlsxExportCategory.CONSUMPTION, consumption);
		enumMap.put(XlsxExportCategory.TIME_OF_USE_TARIFF, tou);

		production.add(new XlsxExportDataEntry("PV-Dach", new ChannelAddress("meter0", "ActivePower"),
				HistoricTimedataSaveType.POWER));
		production.add(new XlsxExportDataEntry("PV-Alm", new ChannelAddress("meter1", "ActivePower"),
				HistoricTimedataSaveType.POWER));
		consumption.add(new XlsxExportDataEntry("Consumption Meter", new ChannelAddress("meter2", "ActivePower"),
				HistoricTimedataSaveType.POWER));
		consumption.add(new XlsxExportDataEntry("Wallbox Garage", new ChannelAddress("evcs0", "ChargePower"),
				HistoricTimedataSaveType.POWER));
		tou.add(new XlsxExportDataEntry("Dynamisch Gut", new ChannelAddress("_sum", "GridBuyPower"),
				HistoricTimedataSaveType.POWER));

		return new XlsxExportDetailData(enumMap, CurrencyConfig.EUR);
	}

	/**
	 * Main Method for creating a excel export with mocked data.
	 * 
	 * @param args not used
	 * @throws IOException           if file cant be written
	 * @throws OpenemsNamedException requests fails
	 */
	public static void main(String[] args) throws IOException, OpenemsNamedException {
		createFullXlsx();
		createHalfXlsx();
		createConsumptionOnlyXlsx();
		createProductionOnlyXlsx();
		createTouOnlyXlsx();
		createProductionAndTouXlsx();
		createConsumptionAndTouXlsx();
		createnSingleOfAllXlsx();
	}

	private static void createFullXlsx() throws IOException, OpenemsNamedException {
		var fromDate = ZonedDateTime.of(2020, 07, 01, 0, 0, 0, 0, ZoneId.systemDefault());
		var toDate = ZonedDateTime.of(2020, 07, 02, 0, 0, 0, 0, ZoneId.systemDefault());

		var powerData = CreateXlxsTest.getMockedPowerData();
		var energyData = CreateXlxsTest.getMockedEnergyData();
		var detailData = CreateXlxsTest.getMockedDetailData();

		final var request = new QueryHistoricTimeseriesExportXlsxResponse(UUID.randomUUID(), "edge0", fromDate, toDate,
				powerData, energyData, Language.EN, detailData);

		var payload = request.getPayload();

		byte[] excelData = Base64.getDecoder().decode(payload);

		String filePath = ".\\..\\build\\fullTestPrint.xlsx";

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(excelData);
			System.out.println("Testfile created under: " + filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createHalfXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\emptyTestPrint.xlsx", null, null, null);
	}

	private static void createTestPrint(String filePath, Consumer<List<XlsxExportDataEntry>> consProd,
			Consumer<List<XlsxExportDataEntry>> consCons, Consumer<List<XlsxExportDataEntry>> consTou)
			throws IOException, OpenemsNamedException {
		final var enumMap = new EnumMap<XlsxExportCategory, List<XlsxExportDataEntry>>(XlsxExportCategory.class);
		final var consumption = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var production = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var tou = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();

		if (consProd != null) {
			consProd.accept(production);
		}

		if (consCons != null) {
			consCons.accept(consumption);
		}

		if (consTou != null) {
			consTou.accept(tou);
		}

		enumMap.put(XlsxExportCategory.PRODUCTION, production);
		enumMap.put(XlsxExportCategory.CONSUMPTION, consumption);
		enumMap.put(XlsxExportCategory.TIME_OF_USE_TARIFF, tou);

		var detailData = new XlsxExportDetailData(enumMap, CurrencyConfig.EUR);

		var fromDate = ZonedDateTime.of(2020, 07, 01, 0, 0, 0, 0, ZoneId.systemDefault());
		var toDate = ZonedDateTime.of(2020, 07, 02, 0, 0, 0, 0, ZoneId.systemDefault());

		var powerData = CreateXlxsTest.getMockedPowerData();
		var energyData = CreateXlxsTest.getMockedEnergyData();

		final var request = new QueryHistoricTimeseriesExportXlsxResponse(UUID.randomUUID(), "edge0", fromDate, toDate,
				powerData, energyData, Language.EN, detailData);

		var payload = request.getPayload();

		byte[] excelData = Base64.getDecoder().decode(payload);

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(excelData);
			System.out.println("Testfile created under: " + filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createProductionOnlyXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\prodTestPrint.xlsx", production -> {
			production.add(new XlsxExportDataEntry("PV-Dach", new ChannelAddress("meter0", "ActivePower"),
					HistoricTimedataSaveType.POWER));
			production.add(new XlsxExportDataEntry("PV-Alm", new ChannelAddress("meter1", "ActivePower"),
					HistoricTimedataSaveType.POWER));
		}, null, null);
	}

	private static void createConsumptionOnlyXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\consTestPrint.xlsx", null, consumption -> {
			consumption.add(new XlsxExportDataEntry("Consumption Meter", new ChannelAddress("meter2", "ActivePower"),
					HistoricTimedataSaveType.POWER));
			consumption.add(new XlsxExportDataEntry("Wallbox Garage", new ChannelAddress("evcs0", "ChargePower"),
					HistoricTimedataSaveType.POWER));
		}, null);
	}

	private static void createTouOnlyXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\touPrint.xlsx", null, null, tou -> {
			tou.add(new XlsxExportDataEntry("Dynamisch Gut", new ChannelAddress("_sum", "GridBuyPower"),
					HistoricTimedataSaveType.POWER));
		});
	}

	private static void createProductionAndTouXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\prodAndTouPrint.xlsx", production -> {
			production.add(new XlsxExportDataEntry("PV-Dach", new ChannelAddress("meter0", "ActivePower"),
					HistoricTimedataSaveType.POWER));
			production.add(new XlsxExportDataEntry("PV-Alm", new ChannelAddress("meter1", "ActivePower"),
					HistoricTimedataSaveType.POWER));
		}, null, tou -> {
			tou.add(new XlsxExportDataEntry("Dynamisch Gut", new ChannelAddress("_sum", "GridBuyPower"),
					HistoricTimedataSaveType.POWER));
		});

	}

	private static void createConsumptionAndTouXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\consAndTouPrint.xlsx", null, consumption -> {
			consumption.add(new XlsxExportDataEntry("Consumption Meter", new ChannelAddress("meter2", "ActivePower"),
					HistoricTimedataSaveType.POWER));
			consumption.add(new XlsxExportDataEntry("Wallbox Garage", new ChannelAddress("evcs0", "ChargePower"),
					HistoricTimedataSaveType.POWER));
		}, tou -> {
			tou.add(new XlsxExportDataEntry("Dynamisch Gut", new ChannelAddress("_sum", "GridBuyPower"),
					HistoricTimedataSaveType.POWER));
		});
	}

	private static void createnSingleOfAllXlsx() throws IOException, OpenemsNamedException {
		createTestPrint(".\\..\\build\\singleOfAllPrint.xlsx", production -> {
			production.add(new XlsxExportDataEntry("PV-Alm", new ChannelAddress("meter1", "ActivePower"),
					HistoricTimedataSaveType.POWER));
		}, consumption -> {
			consumption.add(new XlsxExportDataEntry("Wallbox Garage", new ChannelAddress("evcs0", "ChargePower"),
					HistoricTimedataSaveType.POWER));
		}, tou -> {
			tou.add(new XlsxExportDataEntry("Dynamisch Gut", new ChannelAddress("_sum", "GridBuyPower"),
					HistoricTimedataSaveType.POWER));
		});

	}
}
