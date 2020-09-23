package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'queryHistoricTimeseriesExportXlxs'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesExportXlxs",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD
 *   }
 * }
 * </pre>
 */
public class QueryHistoricTimeseriesExportXlxsRequest extends JsonrpcRequest {

	public static final String METHOD = "queryHistoricTimeseriesExportXlxs";

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public static QueryHistoricTimeseriesExportXlxsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		int timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(p, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(p, "toDate", timezone).plusDays(1);
		QueryHistoricTimeseriesExportXlxsRequest result = new QueryHistoricTimeseriesExportXlxsRequest(r.getId(),
				fromDate, toDate);
		return result;

	}

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;

	public QueryHistoricTimeseriesExportXlxsRequest(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate)
			throws OpenemsNamedException {
		super(id, METHOD);

		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		if (this.timezoneDiff != ZoneOffset.from(toDate).getTotalSeconds()) {
			throw new OpenemsException("FromDate and ToDate need to be in the same timezone!");
		}

		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("timezone", this.timezoneDiff) //
				.addProperty("fromDate", FORMAT.format(this.fromDate)) //
				.addProperty("toDate", FORMAT.format(this.toDate)) //
				.build();
	}

	public ZonedDateTime getFromDate() {
		return this.fromDate;
	}

	public ZonedDateTime getToDate() {
		return this.toDate;
	}

}
