package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	/**
	 * Create {@link QueryHistoricTimeseriesExportXlxsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link QueryHistoricTimeseriesExportXlxsRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static QueryHistoricTimeseriesExportXlxsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var jTimezone = JsonUtils.getAsPrimitive(p, "timezone");
		final ZoneId timezone;
		if (jTimezone.isNumber()) {
			// For UI version before 2022.4.0
			timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(JsonUtils.getAsInt(jTimezone) * -1));
		} else {
			timezone = TimeZone.getTimeZone(JsonUtils.getAsString(jTimezone)).toZoneId();
		}

		var fromDate = JsonUtils.getAsZonedDateWithZeroTime(p, "fromDate", timezone);
		var toDate = JsonUtils.getAsZonedDateWithZeroTime(p, "toDate", timezone).plusDays(1);
		return new QueryHistoricTimeseriesExportXlxsRequest(r, fromDate, toDate);

	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;

	private QueryHistoricTimeseriesExportXlxsRequest(JsonrpcRequest request, ZonedDateTime fromDate,
			ZonedDateTime toDate) throws OpenemsNamedException {
		super(request, QueryHistoricTimeseriesExportXlxsRequest.METHOD);

		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public QueryHistoricTimeseriesExportXlxsRequest(ZonedDateTime fromDate, ZonedDateTime toDate)
			throws OpenemsNamedException {
		super(QueryHistoricTimeseriesExportXlxsRequest.METHOD);

		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("fromDate", QueryHistoricTimeseriesExportXlxsRequest.FORMAT.format(this.fromDate)) //
				.addProperty("toDate", QueryHistoricTimeseriesExportXlxsRequest.FORMAT.format(this.toDate)) //
				.build();
	}

	/**
	 * Gets the From-Date.
	 *
	 * @return From-Date
	 */
	public ZonedDateTime getFromDate() {
		return this.fromDate;
	}

	/**
	 * Gets the To-Date.
	 *
	 * @return To-Date
	 */
	public ZonedDateTime getToDate() {
		return this.toDate;
	}

}
