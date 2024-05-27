package io.openems.edge.controller.api.modbus.rtu.jsonrpc;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.modbusslave.ModbusRecord;

/**
 * Wraps a JSON-RPC Response to "getModbusProtocol" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "table": [{
 *       "ref": number // start address of the Modbus Record
 *       "name": string,
 *       "value": string, // value description
 *       "unit": string,
 *       "type" string
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetModbusProtocolResponse extends JsonrpcResponseSuccess {

	private final TreeMap<Integer, ModbusRecord> tableRecords;

	public GetModbusProtocolResponse(UUID id, TreeMap<Integer, ModbusRecord> records) {
		super(id);
		this.tableRecords = records;
	}

	@Override
	public JsonObject getResult() {
		var table = new JsonArray();
		for (Entry<Integer, ModbusRecord> entry : this.tableRecords.entrySet()) {
			var record = entry.getValue();
			table.add(JsonUtils.buildJsonObject() //
					.addProperty("ref", entry.getKey()) //
					.addProperty("name", record.getName()) //
					.addProperty("value", record.getValueDescription()) //
					.addProperty("unit", record.getUnit() == Unit.NONE ? "" : record.getUnit().toString()) //
					.addProperty("type", record.getType().toString()) //
					.addProperty("access", record.getAccessMode().getAbbreviation()) //
					.build());
		}
		var j = new JsonObject();
		j.add("table", table);
		return j;
	}

}
