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

	public static String NOT_AVAILABLE = "nicht verfügbar";

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

	public static Set<ChannelAddress> POWER_CHANNELS = Stream.of(//
			GRID_ACTIVE_POWER, //
			PRODUCTION_ACTIVE_POWER, //
			CONSUMPTION_ACTIVE_POWER, //
			ESS_ACTIVE_POWER, //
			ESS_SOC //
	).collect(Collectors.toCollection(HashSet::new));

	public static Set<ChannelAddress> ENERGY_CHANNELS = Stream.of(//
			GRID_BUY_ENERGY, //
			GRID_SELL_ENERGY, //
			PRODUCTION_ACTIVE_ENERGY, //
			CONSUMPTION_ACTIVE_ENERGY, //
			ESS_ACTIVE_ENERGY //
	).collect(Collectors.toCollection(HashSet::new));

	public QueryHistoricTimeseriesExportXlsxResponse(UUID id, String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData,
			SortedMap<ChannelAddress, JsonElement> historicEnergy) throws IOException, OpenemsNamedException {
		super(id, generatePayload(edgeId, fromDate, toDate, historicData, historicEnergy));
	}

	public static byte[] generatePayload(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> powerData,
			SortedMap<ChannelAddress, JsonElement> energyData) throws IOException, OpenemsNamedException {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook workbook = new Workbook(os, "Historic data", "1.0");
			Worksheet worksheet = workbook.newWorksheet("Sheet1");

			addBasicInfo(worksheet, edgeId, fromDate, toDate);
			addEnergyData(worksheet, energyData);
			addPowerData(worksheet, powerData);

			workbook.finish();
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

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String fromDateString = fromDate.format(formatter);
		String toDateString = toDate.format(formatter);

		addValueBold(ws, 0, 0, "FEMS-Nr.");
		addValue(ws, 0, 1, edgId);
		addValueBold(ws, 1, 0, "Export erstellt am");
		addValue(ws, 1, 1, LocalDate.now().toString());
		addValueBold(ws, 2, 0, "Export Zeitraum");
		addValue(ws, 2, 1, fromDateString + " - " + toDateString);
	}

	/**
	 * Adds the energy data header and values.
	 * 
	 * @param ws   the {@link Worksheet}
	 * @param data the energy data map
	 * @throws OpenemsNamedException
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
		if (checkNull(data.get(ESS_ACTIVE_ENERGY))) {		
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
		addValueBold(ws, 7, 7, "State of Charge(Soc) [%]");

		int rowCount = 8;

		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> row : data.entrySet()) {
			SortedMap<ChannelAddress, JsonElement> values = row.getValue();

			// Adding Date/time data column
			addValue(ws, rowCount, 0, row.getKey().toString());

			if (checkNull(values.get(GRID_ACTIVE_POWER))) {
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
			if (checkNull(values.get(PRODUCTION_ACTIVE_POWER))) {
				addValue(ws, rowCount, 3, JsonUtils.getAsInt(values.get(PRODUCTION_ACTIVE_POWER)));
			}

			if (checkNull(values.get(ESS_ACTIVE_POWER))) {
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
			if (checkNull(values.get(CONSUMPTION_ACTIVE_POWER))) {
				addValue(ws, rowCount, 6, (JsonUtils.getAsInt(values.get(CONSUMPTION_ACTIVE_POWER)) / 4000));
			}

			// State of charge
			if (checkNull(values.get(ESS_SOC))) {
				addValue(ws, rowCount, 7, (JsonUtils.getAsInt(values.get(ESS_SOC))));
			}
			rowCount++;
		}
	}

	/**
	 * Helper method to make the font style to bold in excel
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
	 * Helper method to make the font style to bold in excel
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
	 * Helper method to add the value in the excel sheet
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
	 * Method to checks for null value? if not null add the value ; if null add "not
	 * available"
	 * 
	 * @param ws          the {@link Worksheet}
	 * @param row         row number
	 * @param col         column number
	 * @param jsonElement the value
	 * @throws OpenemsNamedException on error
	 */
	protected static void addvalueIfnotNull(Worksheet ws, int row, int col, JsonElement jsonElement)
			throws OpenemsNamedException {

		if (!jsonElement.isJsonNull()) {
			addValue(ws, row, col, JsonUtils.getAsInt(jsonElement));
		} else {
			addValueItalic(ws, row, col, NOT_AVAILABLE);
		}
	}

	/**
	 * Simple helper method to check null
	 * 
	 * @param jsonElement the value
	 * @return boolean true if not null, false if true
	 * @throws OpenemsNamedException on error
	 */
	protected static boolean checkNull(JsonElement jsonElement) throws OpenemsNamedException {

		if (!jsonElement.isJsonNull()) {
			return true;
		} else {
			return false;
		}
	}

}