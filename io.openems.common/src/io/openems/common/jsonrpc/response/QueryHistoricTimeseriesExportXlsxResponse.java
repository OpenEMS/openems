package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dhatim.fastexcel.BorderSide;
import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.timedata.XlsxExportDetailData;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry;
import io.openems.common.timedata.XlsxWorksheetWrapper;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'queryHistoricTimeseriesExportXlxs'.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": Base64-String
 *   }
 * }
 * </pre>
 */
public class QueryHistoricTimeseriesExportXlsxResponse extends Base64PayloadResponse {

	protected static class Channel {
		public static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");
		public static final ChannelAddress GRID_BUY_ACTIVE_ENERGY = new ChannelAddress("_sum", "GridBuyActiveEnergy");
		public static final ChannelAddress GRID_SELL_ACTIVE_ENERGY = new ChannelAddress("_sum", "GridSellActiveEnergy");
		public static final ChannelAddress PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum",
				"ProductionActivePower");
		public static final ChannelAddress PRODUCTION_ACTIVE_ENERGY = new ChannelAddress("_sum",
				"ProductionActiveEnergy");
		public static final ChannelAddress CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum",
				"ConsumptionActivePower");
		public static final ChannelAddress CONSUMPTION_ACTIVE_ENERGY = new ChannelAddress("_sum",
				"ConsumptionActiveEnergy");
		public static final ChannelAddress ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
		public static final ChannelAddress ESS_DC_CHARGE_ENERGY = new ChannelAddress("_sum", "EssDcChargeEnergy");
		public static final ChannelAddress ESS_DC_DISCHARGE_ENERGY = new ChannelAddress("_sum", "EssDcDischargeEnergy");
		public static final ChannelAddress ESS_SOC = new ChannelAddress("_sum", "EssSoc");
	}

	private static final String BLUE = "44B3E1";
	private static final String LIGHT_GREY = "BFBFBF";
	private static final String DARK_GREY = "D9D9D9";
	private static final String FONT_NAME = "Calibri";

	/**
	 * All Power Channels, i.e. Channels that are exported per channel and
	 * timestamp.
	 */
	public static final Set<ChannelAddress> POWER_CHANNELS = Stream.of(//
			Channel.GRID_ACTIVE_POWER, //
			Channel.PRODUCTION_ACTIVE_POWER, //
			Channel.CONSUMPTION_ACTIVE_POWER, //
			Channel.ESS_DISCHARGE_POWER, //
			Channel.ESS_SOC //
	).collect(Collectors.toCollection(HashSet::new));

	/**
	 * All Energy Channels, i.e. exported with one value per channel.
	 */
	public static final Set<ChannelAddress> ENERGY_CHANNELS = Stream.of(//
			Channel.GRID_BUY_ACTIVE_ENERGY, //
			Channel.GRID_SELL_ACTIVE_ENERGY, //
			Channel.PRODUCTION_ACTIVE_ENERGY, //
			Channel.CONSUMPTION_ACTIVE_ENERGY, //
			Channel.ESS_DC_CHARGE_ENERGY, //
			Channel.ESS_DC_DISCHARGE_ENERGY //
	).collect(Collectors.toCollection(HashSet::new));

	/**
	 * Constructs a {@link QueryHistoricTimeseriesExportXlsxResponse}.
	 *
	 * <p>
	 * While constructing, the actual Excel file is generated as payload of the
	 * JSON-RPC Response.
	 *
	 * @param id               the JSON-RPC ID
	 * @param edgeId           the Edge-ID
	 * @param fromDate         the start date of the export
	 * @param toDate           the end date of the export
	 * @param historicData     the power data per channel and timestamp
	 * @param historicEnergy   the energy data, one value per channel
	 * @param language         the {@link Language}
	 * @param detailComponents the components for the detail view
	 * @throws IOException           on error
	 * @throws OpenemsNamedException on error
	 */
	public QueryHistoricTimeseriesExportXlsxResponse(UUID id, String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData,
			SortedMap<ChannelAddress, JsonElement> historicEnergy, Language language,
			XlsxExportDetailData detailComponents) throws IOException, OpenemsNamedException {

		super(id, XlsxUtils.generatePayload(edgeId, fromDate, toDate, historicData, historicEnergy, language,
				detailComponents));
	}

	protected static class XlsxUtils {

		private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
				.ofPattern("dd.MM.yyyy HH:mm:ss Z");

		/**
		 * Generates the Payload for a
		 * {@link QueryHistoricTimeseriesExportXlsxResponse}.
		 *
		 * @param edgeId           the Edge-Id
		 * @param fromDate         the start date of the export
		 * @param toDate           the end date of the export
		 * @param powerData        the power data per channel and timestamp
		 * @param energyData       the energy data, one value per channel
		 * @param language         the {@link Language}
		 * @param detailComponents the components for the detail view
		 * @return the Excel file as byte-array.
		 * @throws IOException           on error
		 * @throws OpenemsNamedException on error
		 */
		private static byte[] generatePayload(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> powerData,
				SortedMap<ChannelAddress, JsonElement> energyData, Language language,
				XlsxExportDetailData detailComponents) throws IOException, OpenemsNamedException {
			byte[] payload = {};
			try (//
					var os = new ByteArrayOutputStream();
					var wb = new Workbook(os, "", null) //
			) {
				var ws = wb.newWorksheet("Export");

				Locale currentLocale = language.getLocal();

				var translationBundle = ResourceBundle.getBundle("io.openems.common.jsonrpc.response.translation",
						currentLocale);

				XlsxUtils.addBasicInfo(ws, edgeId, fromDate, toDate, translationBundle);
				XlsxUtils.addEnergyData(ws, energyData, translationBundle);
				var rowCount = XlsxUtils.addPowerData(ws, powerData, translationBundle);

				final var worksheetWrapper = new XlsxWorksheetWrapper(ws);

				// Box for Total Overview
				worksheetWrapper.setForRange(9, 0, 9, 7, t -> t.style().borderStyle(BorderSide.TOP, BorderStyle.THIN));
				worksheetWrapper.setForRange(9, 0, rowCount, 0,
						t -> t.style().borderStyle(BorderSide.LEFT, BorderStyle.THIN));
				worksheetWrapper.setForRange(rowCount, 0, rowCount, 7,
						t -> t.style().borderStyle(BorderSide.BOTTOM, BorderStyle.THIN));
				worksheetWrapper.setForRange(9, 7, rowCount, 7,
						t -> t.style().borderStyle(BorderSide.RIGHT, BorderStyle.THIN));

				if (detailComponents.data().values().stream().anyMatch(de -> !de.isEmpty())) { //
					var rightestColumns = XlsxUtils.addDetailData(ws, powerData, detailComponents, translationBundle,
							energyData);

					final var colProd = rightestColumns.get(0) - 1;
					final var colCons = rightestColumns.get(1) - 1;
					final var colTou = rightestColumns.get(2) - 1;

					// Set Box for detail Timerange data
					worksheetWrapper.setForRange(9, 10, 9, colTou,
							t -> t.style().borderStyle(BorderSide.TOP, BorderStyle.THIN));
					worksheetWrapper.setForRange(9, 10, rowCount, 10,
							t -> t.style().borderStyle(BorderSide.LEFT, BorderStyle.THIN));
					worksheetWrapper.setForRange(rowCount, 10, rowCount, colTou,
							t -> t.style().borderStyle(BorderSide.BOTTOM, BorderStyle.THIN));
					worksheetWrapper.setForRange(9, colTou, rowCount, colTou,
							t -> t.style().borderStyle(BorderSide.RIGHT, BorderStyle.THIN));

					// Set Separators between prod, cons and tou
					worksheetWrapper.setForRange(9, colProd, rowCount, colProd,
							t -> t.style().borderStyle(BorderSide.RIGHT, BorderStyle.THIN));
					worksheetWrapper.setForRange(9, colCons, rowCount, colCons,
							t -> t.style().borderStyle(BorderSide.RIGHT, BorderStyle.THIN));

					// Set "blue" Separator between detailed Overview and total Overview
					worksheetWrapper.setForRange(0, 8, rowCount, 8, t -> t.style()//
							.borderStyle(BorderSide.LEFT, BorderStyle.MEDIUM)//
							.borderStyle(BorderSide.RIGHT, BorderStyle.MEDIUM)//
							.fillColor(BLUE));
					worksheetWrapper.getCellWrapper(0, 8).style().borderStyle(BorderSide.TOP, BorderStyle.MEDIUM);
					worksheetWrapper.getCellWrapper(rowCount, 8).style().borderStyle(BorderSide.BOTTOM,
							BorderStyle.MEDIUM);
				}

				worksheetWrapper.setAll();
				wb.finish();
				os.flush();
				payload = os.toByteArray();

				os.close();
			}
			return payload;
		}

		/**
		 * Adds basic information like the Edge-ID, date of creation,...
		 *
		 * @param ws                the {@link Worksheet}
		 * @param edgeId            the edgeId number
		 * @param fromDate          the fromdate the excel exported from
		 * @param toDate            the todate the excel exported to
		 * @param translationBundle the {@link ResourceBundle} for translations
		 */
		protected static void addBasicInfo(Worksheet ws, String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
				ResourceBundle translationBundle) {

			XlsxUtils.addStringValueBold(ws, 0, 0, "Nr.");
			XlsxUtils.addStringValue(ws, 0, 1, edgeId);
			XlsxUtils.addStringValueBold(ws, 1, 0, translationBundle.getString("exportCreatedOn"));
			XlsxUtils.addStringValue(ws, 1, 1, ZonedDateTime.now().format(XlsxUtils.DATE_TIME_FORMATTER));
			XlsxUtils.addStringValueBold(ws, 2, 0, translationBundle.getString("exportPeriod"));

			var fromDateString = fromDate.format(XlsxUtils.DATE_FORMATTER);
			var toDateString = toDate.format(XlsxUtils.DATE_FORMATTER);

			if (fromDate.truncatedTo(ChronoUnit.DAYS).isEqual(//
					toDate.minus(1, ChronoUnit.SECONDS) // toDate might be 0 o'clock in the morning
							.truncatedTo(ChronoUnit.DAYS))) {
				// Only one day selected
				XlsxUtils.addStringValue(ws, 2, 1, fromDateString);
			} else {
				// Multiple days selected
				XlsxUtils.addStringValue(ws, 2, 1, fromDateString + " - " + toDateString);
			}
		}

		/**
		 * Adds the energy data header and values.
		 *
		 * @param ws                the {@link Worksheet}
		 * @param data              the energy data map
		 * @param translationBundle the {@link ResourceBundle} for translations
		 * @throws OpenemsNamedException on error
		 */
		protected static void addEnergyData(Worksheet ws, SortedMap<ChannelAddress, JsonElement> data,
				ResourceBundle translationBundle) throws OpenemsNamedException {
			ws.range(4, 1, 4, 6).merge();
			ws.value(4, 1, translationBundle.getString("totalOverview"));
			ws.range(4, 1, 4, 6).style().fontName(FONT_NAME).fontSize(12).horizontalAlignment("center")
					.verticalAlignment("center").bold().fillColor(BLUE).set();
			ws.range(5, 1, 5, 6).style().bold().fillColor(LIGHT_GREY).set();
			ws.range(6, 1, 6, 6).style().fillColor(DARK_GREY).set();
			// Grid buy energy
			XlsxUtils.addStringValueBold(ws, 5, 1, translationBundle.getString("gridBuy") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 1, data.get(Channel.GRID_BUY_ACTIVE_ENERGY), translationBundle);

			// Grid sell energy
			XlsxUtils.addStringValueBold(ws, 5, 2, translationBundle.getString("gridFeedIn") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 2, data.get(Channel.GRID_SELL_ACTIVE_ENERGY), translationBundle);

			// Production energy
			XlsxUtils.addStringValueBold(ws, 5, 3, translationBundle.getString("production") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 3, data.get(Channel.PRODUCTION_ACTIVE_ENERGY), translationBundle);

			// Charge energy
			XlsxUtils.addStringValueBold(ws, 5, 4, translationBundle.getString("storageCharging") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 4, data.get(Channel.ESS_DC_CHARGE_ENERGY), translationBundle);

			// Charge energy
			XlsxUtils.addStringValueBold(ws, 5, 5, translationBundle.getString("storageDischarging") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 5, data.get(Channel.ESS_DC_DISCHARGE_ENERGY), translationBundle);

			// Consumption energy
			XlsxUtils.addStringValueBold(ws, 5, 6, translationBundle.getString("consumption") + " [kWh]");
			XlsxUtils.addKwhValueIfnotNull(ws, 6, 6, data.get(Channel.CONSUMPTION_ACTIVE_ENERGY), translationBundle);
		}

		/**
		 * Adds the power data header and values.
		 *
		 * @param ws                the {@link Worksheet}
		 * @param data              the power data map
		 * @param translationBundle the {@link ResourceBundle} for translations
		 * @return the y index of the lowest row
		 * @throws OpenemsNamedException on error
		 */
		protected static int addPowerData(Worksheet ws,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data, ResourceBundle translationBundle)
				throws OpenemsNamedException {
			// Adding the headers
			XlsxUtils.addStringValueBold(ws, 9, 0, translationBundle.getString("date/time"));
			XlsxUtils.addStringValueBold(ws, 9, 1, translationBundle.getString("gridBuy") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 2, translationBundle.getString("gridFeedIn") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 3, translationBundle.getString("production") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 4, translationBundle.getString("storageCharging") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 5, translationBundle.getString("storageDischarging") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 6, translationBundle.getString("consumption") + " [W]");
			XlsxUtils.addStringValueBold(ws, 9, 7, translationBundle.getString("stateOfCharge") + " [%]");
			XlsxUtils.addStringValueBold(ws, 8, 1, translationBundle.getString("generalData"));

			var rowCount = 10;

			for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> row : data.entrySet()) {
				var values = row.getValue();

				// Adding Date/time data column
				XlsxUtils.addStringValue(ws, rowCount, 0, row.getKey().format(XlsxUtils.DATE_TIME_FORMATTER));

				if (XlsxUtils.isNotNull(values.get(Channel.GRID_ACTIVE_POWER))) {
					var gridActivePower = JsonUtils.getAsFloat(values.get(Channel.GRID_ACTIVE_POWER));

					if (gridActivePower >= 0) {
						// Grid buy power
						XlsxUtils.addFloatValue(ws, rowCount, 1, gridActivePower);
						// Grid sell power
						XlsxUtils.addFloatValue(ws, rowCount, 2, 0);
					} else {
						// Grid buy power
						XlsxUtils.addFloatValue(ws, rowCount, 1, 0);
						// Grid sell power
						XlsxUtils.addFloatValue(ws, rowCount, 2, gridActivePower / -1);
					}
				} else {
					XlsxUtils.addStringValue(ws, rowCount, 1, "-");
					XlsxUtils.addStringValue(ws, rowCount, 2, "-");
				}

				// Production power
				if (XlsxUtils.isNotNull(values.get(Channel.PRODUCTION_ACTIVE_POWER))) {
					XlsxUtils.addFloatValue(ws, rowCount, 3,
							JsonUtils.getAsFloat(values.get(Channel.PRODUCTION_ACTIVE_POWER)));
				} else {
					XlsxUtils.addStringValue(ws, rowCount, 3, "-");
				}

				if (XlsxUtils.isNotNull(values.get(Channel.ESS_DISCHARGE_POWER))) {
					var essDischargePower = JsonUtils.getAsFloat(values.get(Channel.ESS_DISCHARGE_POWER));
					if (essDischargePower >= 0) {
						XlsxUtils.addFloatValue(ws, rowCount, 4, 0);
						XlsxUtils.addFloatValue(ws, rowCount, 5, essDischargePower);
					} else {
						XlsxUtils.addFloatValue(ws, rowCount, 4, essDischargePower / -1);
						XlsxUtils.addFloatValue(ws, rowCount, 5, 0);
					}
				} else {
					XlsxUtils.addStringValue(ws, rowCount, 4, "-");
					XlsxUtils.addStringValue(ws, rowCount, 5, "-");
				}
				// Consumption power
				if (XlsxUtils.isNotNull(values.get(Channel.CONSUMPTION_ACTIVE_POWER))) {
					XlsxUtils.addFloatValue(ws, rowCount, 6,
							JsonUtils.getAsFloat(values.get(Channel.CONSUMPTION_ACTIVE_POWER)));
				} else {
					XlsxUtils.addStringValue(ws, rowCount, 6, "-");
				}

				// State of charge
				if (XlsxUtils.isNotNull(values.get(Channel.ESS_SOC))) {
					XlsxUtils.addFloatValue(ws, rowCount, 7, JsonUtils.getAsFloat(values.get(Channel.ESS_SOC)));
				} else {
					XlsxUtils.addStringValue(ws, rowCount, 7, "-");
				}
				rowCount++;
			}
			rowCount--;
			return rowCount;
		}

		protected static List<Integer> addDetailData(Worksheet ws,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data,
				XlsxExportDetailData detailComponents, ResourceBundle translationBundle,
				SortedMap<ChannelAddress, JsonElement> energyData) throws OpenemsNamedException {
			ws.width(8, 4);
			ws.width(9, 4);
			ws.width(10, 25);
			ws.width(11, 25);
			ws.width(12, 25);
			ws.width(13, 25);
			ws.width(14, 25);
			ws.width(15, 25);

			ws.range(4, 10, 4, 15).merge();
			ws.range(4, 10, 4, 15).style().fontName(FONT_NAME).fontSize(12).bold().fillColor(BLUE).set();
			ws.value(4, 10, translationBundle.getString("detailData"));
			ws.range(5, 10, 5, 15).merge();
			ws.range(5, 10, 5, 15).style().fillColor(LIGHT_GREY).set();
			ws.value(5, 10, translationBundle.getString("detailHint"));
			var rightestColumn1 = addProductionData(ws, data, detailComponents, translationBundle);
			var rightestColumn2 = addConsumptionData(ws, data, detailComponents, rightestColumn1, translationBundle);
			var rightestColumn3 = addTimeOfUseTariffData(ws, data, detailComponents, rightestColumn2,
					translationBundle);
			ws.width(rightestColumn3, 35);
			return List.of(rightestColumn1, rightestColumn2, rightestColumn3);
		}

		protected static int addProductionData(Worksheet ws,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data,
				XlsxExportDetailData detailComponents, ResourceBundle translationBundle) throws OpenemsNamedException {
			return XlsxUtils.addGenericData(ws, data, detailComponents, 10, translationBundle,
					XlsxExportCategory.PRODUCTION, "production", (t, d) -> t.alias() + " [W]", 1);
		}

		protected static int addConsumptionData(Worksheet ws,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data,
				XlsxExportDetailData detailComponents, int righestColumn, ResourceBundle translationBundle)
				throws OpenemsNamedException {
			return XlsxUtils.addGenericData(ws, data, detailComponents, righestColumn, translationBundle,
					XlsxExportCategory.CONSUMPTION, "consumption", (t, d) -> t.alias() + " [W]", 1);
		}

		protected static int addTimeOfUseTariffData(Worksheet ws,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data,
				XlsxExportDetailData detailComponents, int righestColumn, ResourceBundle translationBundle)
				throws OpenemsNamedException {
			final var unit = "[" + detailComponents.currency().getUnderPart() + "/kWh]";
			return XlsxUtils.addGenericData(ws, data, detailComponents, righestColumn, translationBundle,
					XlsxExportCategory.TIME_OF_USE_TARIFF, "timeOfUse", (t, d) -> t.alias() + " " + unit,
					1000f / detailComponents.currency().getRatio());
		}

		protected static int addGenericData(//
				Worksheet ws, //
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data, //
				XlsxExportDetailData detailComponents, //
				int righestColumn, //
				ResourceBundle translationBundle, //
				XlsxExportDetailData.XlsxExportCategory category, //
				String translationKey, //
				BiFunction<XlsxExportDataEntry, XlsxExportDetailData, String> aliasBuilder, //
				float ratio) //
				throws OpenemsNamedException {

			final var righestColumnOld = righestColumn;

			for (var item : detailComponents.data().get(category)) {
				ws.value(8, righestColumnOld, translationBundle.getString(translationKey));
				ws.style(8, righestColumnOld).bold().set();

				ws.value(9, righestColumn, aliasBuilder.apply(item, detailComponents));
				ws.style(9, righestColumn).bold().set();

				var rowCount = 10;
				for (final var row : data.entrySet()) {
					final var values = row.getValue();
					final var channelAddress = item.channel();

					if (XlsxUtils.isNotNull(values.get(channelAddress))) {
						XlsxUtils.addFloatValueNotRounded(ws, rowCount, righestColumn,
								JsonUtils.getAsFloat(values.get(channelAddress)) / ratio);
					} else {
						XlsxUtils.addStringValue(ws, rowCount, righestColumn, "-");
					}
					rowCount++;
				}
				righestColumn++;
			}

			return righestColumn;
		}

		/**
		 * Helper method to add a value in bold font style to the excel sheet.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value to be bold
		 */
		protected static void addStringValueBold(Worksheet ws, int row, int col, String value) {
			XlsxUtils.addStringValue(ws, row, col, value);
			ws.style(row, col).bold().set();
		}

		/**
		 * Helper method to add a value in bold + italic font style to the excel sheet.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value to be bold
		 */
		protected static void addStringValueItalic(Worksheet ws, int row, int col, String value) {
			XlsxUtils.addStringValue(ws, row, col, value);
			ws.style(row, col).italic().set();
		}

		/**
		 * Helper method to add a energy value in unit [Wh] to the excel sheet. The
		 * value is rounded to 100 Wh and formatted as [kWh]. If the value is 'null',
		 * {@value #NOT_AVAILABLE} is added instead.
		 *
		 * @param ws                the {@link Worksheet}
		 * @param row               row number
		 * @param col               column number
		 * @param jsonElement       the value
		 * @param translationBundle the {@link ResourceBundle}
		 * @throws OpenemsNamedException on error
		 */
		protected static void addKwhValueIfnotNull(Worksheet ws, int row, int col, JsonElement jsonElement,
				ResourceBundle translationBundle) throws OpenemsNamedException {
			if (XlsxUtils.isNotNull(jsonElement)) {
				XlsxUtils.addStringValueRightAligned(ws, row, col,
						String.format("%.1f", JsonUtils.getAsFloat(jsonElement) / 1000));
			} else {
				XlsxUtils.addStringValueItalic(ws, row, col, translationBundle.getString("notAvailable"));
			}
		}

		/**
		 * Helper method to add the value to the excel sheet.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value in the sheet
		 */
		protected static void addStringValueRightAligned(Worksheet ws, int row, int col, String value) {
			XlsxUtils.addStringValue(ws, row, col, value);
			ws.style(row, col).horizontalAlignment("right").set();
		}

		/**
		 * Helper method to add the value to the excel sheet.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value in the sheet
		 */
		protected static void addStringValue(Worksheet ws, int row, int col, String value) {
			ws.value(row, col, value);
		}

		/**
		 * Helper method to add the value to the excel sheet. The float value is
		 * mathematically rounded.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value in the sheet
		 */
		protected static void addFloatValue(Worksheet ws, int row, int col, float value) {
			ws.value(row, col, Math.round(value));
		}

		/**
		 * Helper method to add the value to the excel sheet. The float value is
		 * mathematically rounded.
		 *
		 * @param ws    the {@link Worksheet}
		 * @param row   row number
		 * @param col   column number
		 * @param value actual value in the sheet
		 */
		protected static void addFloatValueNotRounded(Worksheet ws, int row, int col, float value) {
			ws.value(row, col, value);
		}

		/**
		 * Simple helper method to check for null values.
		 *
		 * @param jsonElement the value
		 * @return boolean true if not null, false if null
		 * @throws OpenemsNamedException on error
		 */
		private static boolean isNotNull(JsonElement jsonElement) throws OpenemsNamedException {
			if (jsonElement == null || jsonElement.isJsonNull()) {
				return false;
			}
			return true;
		}
	}

}