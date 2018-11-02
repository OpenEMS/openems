package io.openems.edge.controller.api.modbus;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.openems.common.websocket.JsonrpcResponseSuccess;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.modbusslave.ModbusRecord;

public class JsonApiGetModbusProtocol extends JsonrpcResponseSuccess {

	public static final String METHOD = "getModbusProtocol";

	public static JsonApiGetModbusProtocol of(String id, TreeMap<Integer, ModbusRecord> records) {
		JSONArray table = new JSONArray();

		for (Entry<Integer, ModbusRecord> entry : records.entrySet()) {
			ModbusRecord record = entry.getValue();

			table.put(new JSONObject() //
					.put("ref", entry.getKey()) //
					.put("name", record.getName()) //
					.put("value", record.getValueDescription()) //
					.put("unit", record.getUnit() == Unit.NONE ? "" : record.getUnit().toString()) //
					.put("type", record.getType().toString()) //
			);
		}

		return new JsonApiGetModbusProtocol(id, new JSONObject().put("table", table));
	}

	protected JsonApiGetModbusProtocol(String id, JSONObject result) {
		super(id, result);
	}

}
