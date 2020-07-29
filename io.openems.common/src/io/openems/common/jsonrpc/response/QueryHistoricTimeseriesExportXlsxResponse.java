package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.UUID;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

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

//	private static class Label {
//		protected final String title;
//		protected final String comment;
//
//		protected Label(String title, String comment) {
//			this.title = title;
//			this.comment = comment;
//		}
//	}

	public enum channelColNumber {
		DATETIME(0), //
		NETZBEZUG(1), //
		NETZBEZUG_KUMULIERT(2), //
		NETZEINSPEISUNG(3), //
		NETZEINSPEISUNG_KUMULIERT(4), //
		ERZEUGUNG(5), //
		ERZEUGUNG_KUMULIERT(6), //
		SPEICHER_BELADUNG(7), //
		SPEICHER_BELADUNG_KUMULIERT(8), //
		SPEICHER_ENTLADUNG(9), //
		SPEICHER_ENTLADUNG_KUMULIERT(10), //
		VERBRAUCH(11), //
		VERBRAUCH_KUMULIERT(12), //
		SPEICHER_LADEZUSTAND(13); //

		private final int colNumber;

		channelColNumber(int colNumber) {
			this.colNumber = colNumber;
		}

		public int getColNumber() {
			return this.colNumber;
		}

	}

	private static int ROW = 0;
	// Basic info
	private final static String[] BASICINFO = new String[] { "FEMS-Nr.", "Export erstellt am", "Export Zeitraum" };
	// This data will come from Edge info
	private final static String[] TEMPINFO = new String[] { "1234.", "DATE", "TIME" };
	// Main Header
	private final static String[] MAINHEADER = new String[] { "Netzbezug [kWh]", "Netzeinspeisung [kWh]",
			"Erzeugung [kWh]", "Speicher Beladung [kWh]", "Speicher Entladung [kWh]", "Verbrauch [kWh]" };
	// Comments for main header
//	private final static String[] MOREINFO = new String[] { //
//			"(berechnet aus Leistung [W])", //
//			"(Letzter kumulierter Wert minus erster kumulierter Wert)" };

	private final static String[] LABELS = new String[] { "Date/Time", //
			"Netzbezug [W]", // Grid buy
			"Netzbezug kumuliert [Wh]", //
			"Netzeinspeisung [W]", // Grid Sell
			"Netzeinspeisung kumuliert [Wh]", //
			"Erzeugung [W]", //
			"Erzeugung kumuliert [Wh]", //
			"Speicher Beladung [W]", //
			"Speicher Beladung kumuliert [Wh]", //
			"Speicher Entladung [W]", //
			"Speicher Entladung kumuliert [Wh]", //
			"Verbrauch [W]", //
			"Verbrauch kumuliert [Wh]", //
			"Speicher Ladezustand [%]" //
	};

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
			addData(worksheet, historicData);

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
	public static void addData(Worksheet worksheet,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table) {
		// base info
		boolean isBaseInfoInitialized = false;
		// Main header
		boolean isMainHeader = false;
		// Header Info
//		boolean isHeaderInfo = false;
		// Headers
		boolean isHeaderInitialized = false;
		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> tableRow : table.entrySet()) {

			if (!isBaseInfoInitialized) {
				ROW++;
				initializeBaseInfo(worksheet);
				isBaseInfoInitialized = true;
			}

			if (!isMainHeader) {
				ROW++;
				initializeMainHeader(worksheet);
				isMainHeader = true;
			}

			//TODO adding the comments
//			if (!isHeaderInfo) {
//				initializeHeaderInfo(worksheet);
//				isHeaderInfo = true;
//			}

			if (!isHeaderInitialized) {
				ROW++;
				initializeDataHeader(worksheet, tableRow.getValue());
				isHeaderInitialized = true;
			}

			int col = 0;
			ROW++;
			worksheet.value(ROW, col++, tableRow.getKey().toString());
			double bufferGridBuy = 0.0;
			double bufferGridSell = 0.0;
			double bufferProduction = 0.0;
			double bufferConsumption = 0.0;

			for (Entry<ChannelAddress, JsonElement> tableColumn : tableRow.getValue().entrySet()) {
				JsonElement jValue = tableColumn.getValue();
				String channelName = tableColumn.getKey().toString();
				if (jValue.isJsonNull()) {
					continue;
				}
				
				System.out.println(channelName);
				// Grid buy and sell
				if (channelName.equals("_sum/EssActivePower")) {
					if (jValue.getAsDouble() > 0) {
						worksheet.value(ROW, channelColNumber.NETZBEZUG.getColNumber(),
								"Grid sell " + (jValue.getAsDouble() / 4000));
					} else {
						System.out.println("Lesser than zero " + jValue.getAsDouble());
						worksheet.value(ROW, channelColNumber.NETZEINSPEISUNG.getColNumber(),
								"Grid buy " + (jValue.getAsDouble() / 4000));
					}
				}
				// Production
				if (channelName.equals("_sum/ProductionActivePower")) {
					worksheet.value(ROW, channelColNumber.ERZEUGUNG.getColNumber(),
							"Production " + (jValue.getAsDouble() / 4000));
				}
				// Charging and discharging
				if (channelName.equals("_sum/EssActivePower")) {
					if (jValue.getAsDouble() > 0) {
						worksheet.value(ROW, channelColNumber.SPEICHER_BELADUNG.getColNumber(),
								"Charging " + (jValue.getAsDouble() / 4000));
					} else {
						System.out.println("Lesser than zero " + jValue.getAsDouble());
						worksheet.value(ROW, channelColNumber.SPEICHER_ENTLADUNG.getColNumber(),
								"Discharging " + (jValue.getAsDouble() / 4000));
					}
				}
				// State of charge
				if (channelName.equals("_sum/EssSoc")) {
					worksheet.value(ROW, channelColNumber.SPEICHER_LADEZUSTAND.getColNumber(),
							"State of charge " + (jValue.getAsDouble() / 4000));
				}
				// Consumption
				if (channelName.equals("_sum/ConsumptionActivePower")) {
					worksheet.value(ROW, channelColNumber.VERBRAUCH.getColNumber(),
							"Consumption " + (jValue.getAsDouble() / 4000));
				}

				//TODO add ess active energy and 
				// Testing for energy values

				if (channelName.equals("_sum/GridBuyActiveEnergy")) {
					bufferGridBuy = bufferGridBuy + jValue.getAsDouble();
					worksheet.value(ROW, channelColNumber.NETZBEZUG_KUMULIERT.getColNumber(),
							"Consumption " + bufferGridBuy);
				}

				if (channelName.equals("_sum/GridSellActiveEnergy")) {
					bufferGridSell = bufferGridSell + jValue.getAsDouble();
					worksheet.value(ROW, channelColNumber.NETZEINSPEISUNG_KUMULIERT.getColNumber(),
							"Consumption " + bufferGridSell);
				}

				if (channelName.equals("_sum/ProductionActiveEnergy")) {
					bufferProduction = bufferProduction + jValue.getAsDouble();
					worksheet.value(ROW, channelColNumber.ERZEUGUNG_KUMULIERT.getColNumber(),
							"Consumption " + bufferProduction);
				}

				if (channelName.equals("_sum/ConsumptionActiveEnergy")) {
					bufferConsumption = bufferConsumption + jValue.getAsDouble();
					worksheet.value(ROW, channelColNumber.VERBRAUCH_KUMULIERT.getColNumber(),
							"Consumption " + bufferConsumption);
				}
			}
		}
//		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

	/**
	 * Methods which populates basic information of the FEMS
	 * 
	 * @param worksheet
	 */
	private static void initializeBaseInfo(Worksheet worksheet) {

		for (int i = 0; i < BASICINFO.length; i++) {
			worksheet.value(ROW, 0 /* column 0 */, BASICINFO[i]);
			worksheet.value(ROW, 1 /* column 1 */, TEMPINFO[i]);
			ROW++;
		}
	}

	/**
	 * Method which populates the Main header information
	 * 
	 * @param worksheet
	 */
	private static void initializeMainHeader(Worksheet worksheet) {
		int col = 1;
		for (int i = 0; i < MAINHEADER.length; i++) {
			worksheet.value(ROW, col, MAINHEADER[i]);
			col = col + 2;
		}
		ROW++;
	}

	//TODO Adding the comments 
//	/**
//	 * Method to populate the Header information
//	 * 
//	 * @param worksheet
//	 */
//	private static void initializeHeaderInfo(Worksheet worksheet) {
//		int col = 1;
//		for (int i = 0; i < MAINHEADER.length; i++) {
//			if (col % 2 != 0) {
//				worksheet.value(ROW + 1, col, MOREINFO[0]);
//			} else {
//				worksheet.value(ROW + 1, col, MOREINFO[1]);
//			}
//			col++;
//		}
//		ROW++;
//	}

	/**
	 * Initializes the header for Data.
	 * 
	 * @param worksheet the Worksheet
	 * @param row       the row number
	 * @param sortedMap the channels
	 */
	public static void initializeDataHeader(Worksheet worksheet, SortedMap<ChannelAddress, JsonElement> channelsMap) {
		int col = 0;
		for (int i = 0; i < LABELS.length; i++) {
			worksheet.value(ROW, col++, LABELS[i]);
		}
//		worksheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

}