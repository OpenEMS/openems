package io.openems.common.jsonrpc.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.dhatim.fastexcel.Color;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
//import io.openems.edge.common.modbusslave.ModbusRecord;
//import io.openems.edge.common.modbusslave.ModbusRecordFloat32;
//import io.openems.edge.common.modbusslave.ModbusRecordFloat64;
//import io.openems.edge.common.modbusslave.ModbusRecordString16;
//import io.openems.edge.common.modbusslave.ModbusRecordUint16;
//import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Represents a JSON-RPC Response for 'getHistoryDataExportXlsx'.
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

	public QueryHistoricTimeseriesExportXlsxResponse(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate,
			Map<ChannelAddress, JsonElement> historicData,
			TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> energyData) {
		super(id, generatePayload(fromDate, toDate, historicData, energyData));
	}

	private static final int COL_DATUM_ZEIT = 0;
	private static final int COL_SOC = 1;
	private static final int COL_GRID_BUY_ACTIVE_ENERGY = 2;
	private static final int COL_GRID_SELL_ACTIVE_ENERGY = 3;
	private static final int COL_PRODUCTION_ACTIVE_ENERGY = 4;
	private static final int COL_CONSUMPTION_ACTIVE_ENERGY = 5;
	private static final int COL_ACTIVE_CHARGE_ENERGY = 6;
	private static final int COL_ACTIVE_DISCHARGE_ENERGY = 7;
	private static final int COL_WIDTH = 15;
	private static final int COL_DATA_CHANNELS_1 = 0;
	private static final int COL_DATA_CHANNELS_2 = 1;

	private static byte[] generatePayload(ZonedDateTime fromDate, ZonedDateTime toDate,
			Map<ChannelAddress, JsonElement> historicData,
			TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> energyData) {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook wb = new Workbook(os, "Historic data", "1.0");
			Worksheet ws = wb.newWorksheet("Sheet1");

			ws.width(COL_DATUM_ZEIT, COL_WIDTH);
			ws.width(COL_SOC, COL_WIDTH);
			ws.width(COL_GRID_BUY_ACTIVE_ENERGY, COL_WIDTH);
			ws.width(COL_GRID_SELL_ACTIVE_ENERGY, COL_WIDTH);
			ws.width(COL_PRODUCTION_ACTIVE_ENERGY, COL_WIDTH);
			ws.width(COL_CONSUMPTION_ACTIVE_ENERGY, COL_WIDTH);
			ws.width(COL_ACTIVE_CHARGE_ENERGY, COL_WIDTH);
			ws.width(COL_ACTIVE_DISCHARGE_ENERGY, COL_WIDTH);

			int nextRow = 1;
			// Add from date and to date in the excel sheet
			ws.value(nextRow, COL_DATA_CHANNELS_1, "Export-Zeitraum");
			ws.value(nextRow, COL_DATA_CHANNELS_2, fromDate.toString());
			nextRow++;
			ws.value(nextRow, COL_DATA_CHANNELS_1, "Datenexport erstellt am");
			ws.value(nextRow, COL_DATA_CHANNELS_2, toDate.toString());
			nextRow++;
			ws.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);

			// Create Data channels data
			for (Map.Entry<ChannelAddress, JsonElement> entry : historicData.entrySet()) {
				addDataRecord(ws, entry.getKey().toString(), entry.getValue().toString(), nextRow);
				nextRow++;
			}

			// Create Energy channels data

			// Add headers
			nextRow++;
			addSheetHeader(wb, ws, nextRow);
			// create sheet
			nextRow++;
			for (Map.Entry<ZonedDateTime, Map<ChannelAddress, JsonElement>> entry : energyData.rowMap().entrySet()) {
				// System.out.println(entry.getKey());
				// System.out.println(entry.getValue().keySet().toString());
				// System.out.println(entry.getValue().values());
				ZonedDateTime time = entry.getKey();
				Object[] values = entry.getValue().values().toArray();
				addEnergyRecord(ws, time, values, nextRow);
				nextRow++;
			}
			wb.finish();
			os.flush();
			payload = os.toByteArray();
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return payload;
	}

	public static void addDataRecord(Worksheet sheet, String key, String value, int rowCount) {
		sheet.value(rowCount, COL_DATA_CHANNELS_1, key);
		sheet.value(rowCount, COL_DATA_CHANNELS_2, value);
		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

	public static void addSheetHeader(Workbook workbook, Worksheet sheet, int rowCount) {
		sheet.value(rowCount, COL_DATUM_ZEIT, "Datum Zeit");
		sheet.value(rowCount, COL_SOC, "State of Charge [%]");
		sheet.value(rowCount, COL_GRID_BUY_ACTIVE_ENERGY, "Grid buy Active Energy [KW]");
		sheet.value(rowCount, COL_GRID_SELL_ACTIVE_ENERGY, "Grid sell Active Energy [kW]");
		sheet.value(rowCount, COL_PRODUCTION_ACTIVE_ENERGY, "Production Active Energy [kW]	");
		sheet.value(rowCount, COL_CONSUMPTION_ACTIVE_ENERGY, "Consumption Active Energy [kW]	");
		sheet.value(rowCount, COL_ACTIVE_CHARGE_ENERGY, "Active Charge Energy [kW]	");
		sheet.value(rowCount, COL_ACTIVE_DISCHARGE_ENERGY, "Active Discharge Energy [kW]");
		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

	public static void addEnergyRecord(Worksheet sheet, ZonedDateTime time, Object[] values, int rowCount) {
		sheet.value(rowCount, COL_DATUM_ZEIT, time.toString());

		// Order of the Object Array " values"
		// {_sum/ConsumptionActiveEnergy=null, _sum/EssActiveChargeEnergy=null,
		// _sum/EssActiveDischargeEnergy=null, _sum/EssSoc=null,
		// _sum/GridBuyActiveEnergy=null, _sum/GridSellActiveEnergy=null,
		// _sum/ProductionActiveEnergy=null}

		sheet.value(rowCount, COL_CONSUMPTION_ACTIVE_ENERGY, values[0].toString());
		sheet.value(rowCount, COL_ACTIVE_CHARGE_ENERGY, values[1].toString());
		sheet.value(rowCount, COL_ACTIVE_DISCHARGE_ENERGY, values[2].toString());
		sheet.value(rowCount, COL_SOC, values[3].toString());
		sheet.value(rowCount, COL_GRID_BUY_ACTIVE_ENERGY, values[4].toString());
		sheet.value(rowCount, COL_GRID_SELL_ACTIVE_ENERGY, values[5].toString());
		sheet.value(rowCount, COL_PRODUCTION_ACTIVE_ENERGY, values[6].toString());
		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin").wrapText(true);
	}

}
