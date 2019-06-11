package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.UUID;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;

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

	private static class Label {
		protected final String title;
		protected final String comment;

		protected Label(String title, String comment) {
			this.title = title;
			this.comment = comment;
		}
	}

	private final static ImmutableMap<String, Label> LABELS = ImmutableMap.<String, Label>builder() //
			.put("datetime", //
					new Label("Date/Time", null)) //
			.put("_sum/EssSoc", //
					new Label("Ladezustand [%]", null)) //
			.put("_sum/EssActivePower", //
					new Label("Speicher Be-/Entladung [W]",
							"Positive Werte für Entladung, negative für Beladung. Bei DC-gekoppelten Speichersystemen wird die PV-Leistung hier ebenfalls erfasst.")) //
			.put("_sum/ConsumptionActivePower", //
					new Label("Verbrauch [W]",
							"Dieser Wert wird nicht direkt gemessen, sondern errechnet sich aus Speicher Be-/Entladung, Netzbezug/-einspeisung und Erzeugung.")) //
			.put("_sum/GridActivePower", //
					new Label("Netzbezug-/einspeisung [W]",
							"Positive Werte für Netzbezug, negative für Netzeinspeisung.")) //
			.put("_sum/ProductionActivePower", //
					new Label("Erzeugung [W]", null)) //
			.build();

	public QueryHistoricTimeseriesExportXlsxResponse(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData,
			SortedMap<ChannelAddress, JsonElement> historicEnergy) throws IOException {
		super(id, generatePayload(fromDate, toDate, historicData, historicEnergy));
	}

	private static final int COL_WIDTH = 15;

	private static byte[] generatePayload(ZonedDateTime fromDate, ZonedDateTime toDate,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData,
			SortedMap<ChannelAddress, JsonElement> historicEnergy) throws IOException {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook workbook = new Workbook(os, "Historic data", "1.0");
			Worksheet worksheet = workbook.newWorksheet("Sheet1");

			initializeColWidths(worksheet, COL_WIDTH, historicData.size() + 1);

			// TODO add Energy
			addData(worksheet, 0, historicData);

			workbook.finish();
			os.flush();
			payload = os.toByteArray();
			os.close();
		}
		return payload;
	}

	/**
	 * Initializes the width of WorkSheet columns.
	 * 
	 * @param worksheet the WorkSheet
	 * @param width     the width
	 * @param columns   the number of columns
	 */
	public static void initializeColWidths(Worksheet worksheet, int width, int columns) {
		for (int i = 0; i < columns; i++) {
			worksheet.width(i, COL_WIDTH);
		}
	}

	/**
	 * Adds all data.
	 * 
	 * @param worksheet    the Worksheet
	 * @param row          the start row
	 * @param historicData the data map
	 */
	public static void addData(Worksheet worksheet, int row,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table) {
		boolean isHeaderInitialized = false;
		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> tableRow : table.entrySet()) {
			if (!isHeaderInitialized) {
				initializeDataHeader(worksheet, row++, tableRow.getValue());
				isHeaderInitialized = true;
			}

			int col = 0;
			worksheet.value(row, col++, tableRow.getKey().toString());
			for (Entry<ChannelAddress, JsonElement> tableColumn : tableRow.getValue().entrySet()) {
				JsonElement jValue = tableColumn.getValue();
				if (jValue.isJsonNull()) {
					continue;
				}
				double value = jValue.getAsDouble();
				worksheet.value(row, col++, Math.round(value));
			}
			row++;
		}
//		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

	/**
	 * Initializes the header for Data.
	 * 
	 * @param worksheet the Worksheet
	 * @param row       the row number
	 * @param sortedMap the channels
	 */
	public static void initializeDataHeader(Worksheet worksheet, int row,
			SortedMap<ChannelAddress, JsonElement> channelsMap) {
		int col = 0;
		worksheet.value(row, col++, LABELS.get("datetime").title);
		for (Entry<ChannelAddress, JsonElement> entry : channelsMap.entrySet()) {
			String key = entry.getKey().toString();
			Label label = LABELS.get(key);
			if (label == null) {
				label = new Label(key.toString(), null);
			}
			worksheet.value(row, col, label.title);
			if (label.comment != null) {
				worksheet.comment(row, col, label.comment);
			}
			col++;
		}
//		worksheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

}
