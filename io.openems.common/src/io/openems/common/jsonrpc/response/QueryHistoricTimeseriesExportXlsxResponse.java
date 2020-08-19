package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	public static final String NOT_AVAILABLE = "nicht verfügbar";

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss Z");

	public static ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");
	public static ChannelAddress GRID_BUY_ENERGY = new ChannelAddress("_sum", "GridBuyEnergy");
	public static ChannelAddress GRID_SELL_ENERGY = new ChannelAddress("_sum", "GridSellEnergy");
	public static ChannelAddress PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum", "ProductionActivePower");
	public static ChannelAddress PRODUCTION_ACTIVE_ENERGY = new ChannelAddress("_sum", "ProductionActiveEnergy");
	public static ChannelAddress CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static ChannelAddress CONSUMPTION_ACTIVE_ENERGY = new ChannelAddress("_sum", "ConsumptionActiveEnergy");
	public static ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress("_sum", "EssActivePower");
	public static ChannelAddress ESS_ACTIVE_ENERGY = new ChannelAddress("_sum", "EssActiveEnergy");
	public static ChannelAddress ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	/**
	 * All Power Channels, i.e. Channels that are exported per channel and
	 * timestamp.
	 */
	public static Set<ChannelAddress> POWER_CHANNELS = Stream.of(//
			GRID_ACTIVE_POWER, //
			PRODUCTION_ACTIVE_POWER, //
			CONSUMPTION_ACTIVE_POWER, //
			ESS_ACTIVE_POWER, //
			ESS_SOC //
	).collect(Collectors.toCollection(HashSet::new));

	/**
	 * All Energy Channels, i.e. exported with one value per channel.
	 */
	public static Set<ChannelAddress> ENERGY_CHANNELS = Stream.of(//
			GRID_BUY_ENERGY, //
			GRID_SELL_ENERGY, //
			PRODUCTION_ACTIVE_ENERGY, //
			CONSUMPTION_ACTIVE_ENERGY, //
			ESS_ACTIVE_ENERGY //
	).collect(Collectors.toCollection(HashSet::new));

	/**
	 * Constructs a {@link QueryHistoricTimeseriesExportXlsxResponse}.
	 * 
	 * <p>
	 * While constructing, the actual Excel file is generated as payload of the
	 * JSON-RPC Response.
	 * 
	 * @param id             the JSON-RPC ID
	 * @param edgeId         the Edge-ID
	 * @param fromDate       the start date of the export
	 * @param toDate         the end date of the export
	 * @param historicData   the power data per channel and timestamp
	 * @param historicEnergy the energy data, one value per channel
	 * @throws IOException           on error
	 * @throws OpenemsNamedException on error
	 */
	public QueryHistoricTimeseriesExportXlsxResponse(UUID id, String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData,
			SortedMap<ChannelAddress, JsonElement> historicEnergy) throws IOException, OpenemsNamedException {
		super(id, generatePayload(edgeId, fromDate, toDate, historicData, historicEnergy));
	}

	/**
	 * Generates the Payload for a
	 * {@link QueryHistoricTimeseriesExportXlsxResponse}.
	 * 
	 * @param edgeId     the Edge-Id
	 * @param fromDate   the start date of the export
	 * @param toDate     the end date of the export
	 * @param powerData  the power data per channel and timestamp
	 * @param energyData the energy data, one value per channel
	 * @return the Excel file as byte-array.
	 * @throws IOException           on error
	 * @throws OpenemsNamedException on error
	 */
	private static byte[] generatePayload(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> powerData,
			SortedMap<ChannelAddress, JsonElement> energyData) throws IOException, OpenemsNamedException {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook wb = new Workbook(os, OpenemsConstants.MANUFACTURER_MODEL, OpenemsConstants.VERSION_STRING);
			Worksheet ws = wb.newWorksheet("Export");

			addBasicInfo(ws, edgeId, fromDate, toDate);
			addEnergyData(ws, energyData);
			addPowerData(ws, powerData);

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
	 * @param ws       the {@link Worksheet}
	 * @param edgeId   the edgeId number
	 * @param fromDate the fromdate the excel exported from
	 * @param toDate   the todate the excel exported to
	 */
	protected static void addBasicInfo(Worksheet ws, String edgId, ZonedDateTime fromDate, ZonedDateTime toDate) {
		addValueBold(ws, 0, 0, "FEMS-Nr.");
		addValue(ws, 0, 1, edgId);
		addValueBold(ws, 1, 0, "Export erstellt am");
		addValue(ws, 1, 1, ZonedDateTime.now().format(DATE_TIME_FORMATTER));
		addValueBold(ws, 2, 0, "Export Zeitraum");

		String fromDateString = fromDate.format(DATE_FORMATTER);
		String toDateString = toDate.format(DATE_FORMATTER);
		addValue(ws, 2, 1, fromDateString + " - " + toDateString);
	}

	/**
	 * Adds the energy data header and values.
	 * 
	 * @param ws   the {@link Worksheet}
	 * @param data the energy data map
	 * @throws OpenemsNamedException on error
	 */
	protected static void addEnergyData(Worksheet ws, SortedMap<ChannelAddress, JsonElement> data)
			throws OpenemsNamedException {
		// Heading for Grid buy energy
		addValueBold(ws, 4, 1, "Netzbezug [kWh]");

		// Grid buy energy
		addvalueIfnotNull(ws, 5, 1, data.get(GRID_BUY_ENERGY));

		// Heading for Grid sell energy
		addValueBold(ws, 4, 2, "Netzeinspeisung [kWh]");

		// Grid sell energy
		addvalueIfnotNull(ws, 5, 2, data.get(GRID_SELL_ENERGY));

		// Heading for Production energy
		addValueBold(ws, 4, 3, "Erzeugung [kWh]");

		// Production energy
		addvalueIfnotNull(ws, 5, 3, data.get(PRODUCTION_ACTIVE_ENERGY));

		// Heading for storage Charge
		addValueBold(ws, 4, 4, "Speicher Beladung [kWh]");

		// Heading for storage disCharge
		addValueBold(ws, 4, 5, "Speicher Entladung [kWh]");

		// Charge and discharge energy
		if (isNotNull(data.get(ESS_ACTIVE_ENERGY))) {
			int essActiveEnergy = JsonUtils.getAsInt(data.get(ESS_ACTIVE_ENERGY));
			if (essActiveEnergy > 0) {
				addValue(ws, 5, 4, essActiveEnergy);
				addValue(ws, 5, 5, 0);
			} else {
				addValue(ws, 5, 4, 0);
				addValue(ws, 5, 5, essActiveEnergy);
			}
		} else {
			addValueItalic(ws, 5, 4, NOT_AVAILABLE);
			addValueItalic(ws, 5, 5, NOT_AVAILABLE);

		}

		// heading for consumption
		addValueBold(ws, 4, 6, "Verbrauch [kWh]");

		// Consumption energy
		addvalueIfnotNull(ws, 5, 6, data.get(CONSUMPTION_ACTIVE_ENERGY));
	}

	/**
	 * Adds the power data header and values.
	 * 
	 * @param ws   the {@link Worksheet}
	 * @param data the power data map
	 * @throws OpenemsNamedException on error
	 */
	protected static void addPowerData(Worksheet ws,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data) throws OpenemsNamedException {
		// Adding the headers
		addValueBold(ws, 7, 0, "Datum/Uhrzeit");
		addValueBold(ws, 7, 1, "Netzbezug [W]");
		addValueBold(ws, 7, 2, "Netzeinspeisung [W]");
		addValueBold(ws, 7, 3, "Erzeugung [W]");
		addValueBold(ws, 7, 4, "Speicher Beladung [W]");
		addValueBold(ws, 7, 5, "Speicher Entladung [W]");
		addValueBold(ws, 7, 6, "Verbrauch [W]");
		addValueBold(ws, 7, 7, "Ladezustand [%]");

		int rowCount = 8;

		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> row : data.entrySet()) {
			SortedMap<ChannelAddress, JsonElement> values = row.getValue();

			// Adding Date/time data column
			addValue(ws, rowCount, 0, row.getKey().format(DATE_TIME_FORMATTER));

			if (isNotNull(values.get(GRID_ACTIVE_POWER))) {
				int gridActivePower = JsonUtils.getAsInt(values.get(GRID_ACTIVE_POWER));

				if (gridActivePower >= 0) {
					// Grid buy power
					addValue(ws, rowCount, 1, (gridActivePower));
					// Grid sell power
					addValue(ws, rowCount, 2, 0);
				} else {
					// Grid buy power
					addValue(ws, rowCount, 1, 0);
					// Grid sell power
					addValue(ws, rowCount, 2, (gridActivePower / -1));
				}
			}

			// Production power
			if (isNotNull(values.get(PRODUCTION_ACTIVE_POWER))) {
				addValue(ws, rowCount, 3, JsonUtils.getAsInt(values.get(PRODUCTION_ACTIVE_POWER)));
			}

			if (isNotNull(values.get(ESS_ACTIVE_POWER))) {
				int essActivePower = JsonUtils.getAsInt(values.get(ESS_ACTIVE_POWER));
				if (essActivePower >= 0) {
					addValue(ws, rowCount, 4, (essActivePower));
					addValue(ws, rowCount, 5, 0);
				} else {
					addValue(ws, rowCount, 4, 0);
					addValue(ws, rowCount, 5, (essActivePower / -1));
				}
			}
			// Consumption power
			if (isNotNull(values.get(CONSUMPTION_ACTIVE_POWER))) {
				addValue(ws, rowCount, 6, (JsonUtils.getAsInt(values.get(CONSUMPTION_ACTIVE_POWER)) / 4000));
			}

			// State of charge
			if (isNotNull(values.get(ESS_SOC))) {
				addValue(ws, rowCount, 7, (JsonUtils.getAsInt(values.get(ESS_SOC))));
			}
			rowCount++;
		}
	}

	/**
	 * Helper method to add a value in bold font style to the excel sheet.
	 * 
	 * @param ws     the {@link Worksheet}
	 * @param row    row number
	 * @param column column number
	 * @param value  actual value to be bold
	 */
	protected static void addValueBold(Worksheet ws, int row, int column, Object value) {
		addValue(ws, row, column, value);
		ws.style(row, column).bold().set();
	}

	/**
	 * Helper method to add a value in bold + italic font style to the excel sheet.
	 * 
	 * @param ws     the {@link Worksheet}
	 * @param row    row number
	 * @param column column number
	 * @param value  actual value to be bold
	 */
	protected static void addValueItalic(Worksheet ws, int row, int column, Object value) {
		addValue(ws, row, column, value);
		ws.style(row, column).italic().set();
	}

	/**
	 * Helper method to add the value to the excel sheet.
	 * 
	 * @param ws     the {@link Worksheet}
	 * @param row    row number
	 * @param column column number
	 * @param value  actual value in the sheet
	 */
	protected static void addValue(Worksheet ws, int row, int column, Object value) {
		ws.value(row, column, value);
	}

	/**
	 * Helper method to add the value to the excel sheet. If the value is 'null',
	 * {@value #NOT_AVAILABLE} is added instead.
	 * 
	 * @param ws          the {@link Worksheet}
	 * @param row         row number
	 * @param col         column number
	 * @param jsonElement the value
	 * @throws OpenemsNamedException on error
	 */
	protected static void addvalueIfnotNull(Worksheet ws, int row, int col, JsonElement jsonElement)
			throws OpenemsNamedException {
		if (isNotNull(jsonElement)) {
			addValue(ws, row, col, JsonUtils.getAsInt(jsonElement));
		} else {
			addValueItalic(ws, row, col, NOT_AVAILABLE);
		}
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