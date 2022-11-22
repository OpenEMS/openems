package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.DateUtils;
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
		var timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		var timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		var fromDate = JsonUtils.getAsZonedDateWithZeroTime(p, "fromDate", timezone);
		var toDate = JsonUtils.getAsZonedDateWithZeroTime(p, "toDate", timezone).plusDays(1);
		return new QueryHistoricTimeseriesExportXlxsRequest(r, fromDate, toDate);

	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;

	private QueryHistoricTimeseriesExportXlxsRequest(JsonrpcRequest request, ZonedDateTime fromDate,
			ZonedDateTime toDate) throws OpenemsNamedException {
		super(request, QueryHistoricTimeseriesExportXlxsRequest.METHOD);

		DateUtils.assertSameTimezone(fromDate, toDate);
		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("timezone", this.timezoneDiff) //
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
