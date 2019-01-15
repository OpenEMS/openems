package io.openems.edge.controller.api.modbus;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.modbusslave.ModbusRecord;

public class GetModbusProtocolResponse extends JsonrpcResponseSuccess {

	private final TreeMap<Integer, ModbusRecord> tableRecords;

	public GetModbusProtocolResponse(UUID id, TreeMap<Integer, ModbusRecord> records) {
		super(id);
		this.tableRecords = records;
	}

	public TreeMap<Integer, ModbusRecord> getTableRecords() {
		return tableRecords;
	}

	@Override
	public JsonObject getResult() {
		JsonArray table = new JsonArray();
		for (Entry<Integer, ModbusRecord> entry : this.tableRecords.entrySet()) {
			ModbusRecord record = entry.getValue();
			table.add(JsonUtils.buildJsonObject() //
					.addProperty("ref", entry.getKey()) //
					.addProperty("name", record.getName()) //
					.addProperty("value", record.getValueDescription()) //
					.addProperty("unit", record.getUnit() == Unit.NONE ? "" : record.getUnit().toString()) //
					.addProperty("type", record.getType().toString()) //
					.build());
		}
		JsonObject j = new JsonObject();
		j.add("table", table);
		return j;
	}

}
