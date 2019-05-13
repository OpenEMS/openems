package io.openems.edge.controller.api.modbus.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.dhatim.fastexcel.Color;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.response.Base64PayloadResponse;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordFloat32;
import io.openems.edge.common.modbusslave.ModbusRecordFloat64;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Represents a JSON-RPC Response for 'getModbusProtocolExportXlsx'.
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
public class GetModbusProtocolExportXlsxResponse extends Base64PayloadResponse {

	public GetModbusProtocolExportXlsxResponse(UUID id, TreeMap<Integer, String> components,
			TreeMap<Integer, ModbusRecord> records) {
		super(id, generatePayload(components, records));
	}

	private static final int COL_ADDRESS = 0;
	private static final int COL_DESCRIPTION = 1;
	private static final int COL_TYPE = 2;
	private static final int COL_VALUE = 3;
	private static final int COL_UNIT = 4;
	private static final int COL_ACCESS = 5;

	private static byte[] generatePayload(TreeMap<Integer, String> components, TreeMap<Integer, ModbusRecord> records) {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook wb = new Workbook(os, "OpenEMS Modbus-TCP", "1.0");
			Worksheet ws = wb.newWorksheet("Modbus-Table");

			ws.width(COL_ADDRESS, 10);
			ws.width(COL_DESCRIPTION, 25);
			ws.width(COL_TYPE, 10);
			ws.width(COL_VALUE, 35);
			ws.width(COL_UNIT, 20);
			ws.width(COL_ACCESS, 10);
			// Add headers
			addSheetHeader(wb, ws);
			// Create Sheet
			int nextRow = 1;
			for (Entry<Integer, ModbusRecord> entry : records.entrySet()) {
				int address = entry.getKey();

				String component = components.get(address);
				if (address == 0 || component != null) {
					if (address == 0) {
						// Add the global header row
						addComponentHeader(ws, "Header", nextRow);
					} else {
						// Add Component-Header-Row
						addComponentHeader(ws, component, nextRow);
					}
					nextRow++;
				}

				// Add a Record-Row
				ModbusRecord record = entry.getValue();
				if (nextRow % 2 == 0) {
					addRecord(ws, address, record, nextRow);
				} else {
					addRecord(ws, address, record, nextRow);
				}
				nextRow++;
			}
			// Shading alternative Rows
			ws.range(1, 0, nextRow, 5).style().borderStyle("thin").shadeAlternateRows(Color.GRAY1).set();
			// Add undefined values sheet
			addUndefinedSheet(wb);
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

	public static void addSheetHeader(Workbook workbook, Worksheet sheet) {

		sheet.value(0, COL_ADDRESS, "Address");
		sheet.value(0, COL_DESCRIPTION, "Description");
		sheet.value(0, COL_TYPE, "Type");
		sheet.value(0, COL_VALUE, "Value/Range");
		sheet.value(0, COL_UNIT, "Unit");
		sheet.value(0, COL_ACCESS, "Access");
		sheet.style(0, 0).bold().fillColor(Color.GRAY5).borderStyle("thin");
	}

	public static void addComponentHeader(Worksheet sheet, String title, int rowCount) {
		sheet.value(rowCount, 0, title);
		sheet.style(rowCount, 0).bold().fillColor(Color.GRAY10).borderStyle("thin");
	}

	public static void addRecord(Worksheet sheet, int address, ModbusRecord record, int rowCount) {
		sheet.value(rowCount, COL_ADDRESS, address);
		sheet.value(rowCount, COL_DESCRIPTION, record.getName());
		sheet.value(rowCount, COL_TYPE, record.getType().toString());
		sheet.value(rowCount, COL_VALUE, record.getValueDescription());
		Unit unit = record.getUnit();
		if (unit != Unit.NONE) {
			sheet.value(rowCount, COL_UNIT, unit.toString());
		}
		sheet.value(rowCount, COL_ACCESS, record.getAccessMode().getAbbreviation());
	}

	/**
	 * Add Sheet to describe UNDEFINED values.
	 * 
	 * @param workbook the Workbook
	 */
	private static void addUndefinedSheet(Workbook wb) {
		Worksheet ws = wb.newWorksheet("Undefined values");

		ws.value(0, COL_ADDRESS, "In case a modbus value is 'undefined', the following value will be read:");
		ws.value(1, 0, "type");
		ws.value(1, 1, "value");

		int nextRow = 2;
		for (ModbusType modbusType : ModbusType.values()) {
			byte[] value = new byte[0];
			switch (modbusType) {
			case FLOAT32:
				value = ModbusRecordFloat32.UNDEFINED_VALUE;
				break;
			case FLOAT64:
				value = ModbusRecordFloat64.UNDEFINED_VALUE;
				break;
			case STRING16:
				value = ModbusRecordString16.UNDEFINED_VALUE;
				break;
			case UINT16:
				value = ModbusRecordUint16.UNDEFINED_VALUE;
				break;
			}

			ws.value(nextRow, 0, modbusType.toString());
			ws.value(nextRow, 1, byteArrayToString(value));
			//Alternate Row shading
			ws.range(1, 0, nextRow, 2).style().borderStyle("thin").shadeAlternateRows(Color.GRAY1).set();
		}
	}

	private static String byteArrayToString(byte[] value) {
		if (value.length == 0) {
			return "";
		}
		StringBuilder result = new StringBuilder("0x");
		for (byte b : value) {
			result.append(Integer.toHexString(b & 0xff));
		}
		return result.toString();
	}

}
