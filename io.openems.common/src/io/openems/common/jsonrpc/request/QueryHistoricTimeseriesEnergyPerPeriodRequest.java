package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'queryHistoricTimeseriesEnergyPerPeriod'.
 *
 * <p>
 * This Request is for use-cases where you want to get the energy for each
 * period (with length 'resolution') per Channel, e.g. to visualize energy in a
 * histogram chart. For each period the energy is calculated by subtracting
 * first value of the period from the last value of the period.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesEnergyPerPeriod",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[],
 *     "resolution": Number
 *   }
 * }
 * </pre>
 */

public class QueryHistoricTimeseriesEnergyPerPeriodRequest extends JsonrpcRequest {

	public static final String METHOD = "queryHistoricTimeseriesEnergyPerPeriod";

	/**
	 * Create {@link QueryHistoricTimeseriesEnergyPerPeriodRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static QueryHistoricTimeseriesEnergyPerPeriodRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		var timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		var fromDate = JsonUtils.getAsZonedDateTime(p, "fromDate", timezone);
		var toDate = JsonUtils.getAsZonedDateTime(p, "toDate", timezone).plusDays(1);
		var resolution = JsonUtils.getAsInt(p, "resolution");
		var result = new QueryHistoricTimeseriesEnergyPerPeriodRequest(r, fromDate, toDate, resolution);
		var channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			var address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();
	private final int resolution;

	private QueryHistoricTimeseriesEnergyPerPeriodRequest(JsonrpcRequest request, ZonedDateTime fromDate,
			ZonedDateTime toDate, int resolution) throws OpenemsNamedException {
		super(request, QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD);

		DateUtils.assertSameTimezone(fromDate, toDate);
		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.resolution = resolution;
	}

	public QueryHistoricTimeseriesEnergyPerPeriodRequest(ZonedDateTime fromDate, ZonedDateTime toDate, int resolution)
			throws OpenemsNamedException {
		super(QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD);

		DateUtils.assertSameTimezone(fromDate, toDate);
		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.resolution = resolution;
	}

	private void addChannel(ChannelAddress address) {
		this.channels.add(address);
	}

	@Override
	public JsonObject getParams() {
		var channels = new JsonArray();
		for (ChannelAddress address : this.channels) {
			channels.add(address.toString());
		}
		return JsonUtils.buildJsonObject().addProperty("timezone", this.timezoneDiff) //
				.addProperty("fromDate", QueryHistoricTimeseriesEnergyPerPeriodRequest.FORMAT.format(this.fromDate)) //
				.addProperty("toDate", QueryHistoricTimeseriesEnergyPerPeriodRequest.FORMAT.format(this.toDate)) //
				.add("channels", channels) //
				.addProperty("resolution", this.resolution) //
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

	/**
	 * Gets the {@link ChannelAddress}es.
	 *
	 * @return Set of {@link ChannelAddress}
	 */
	public TreeSet<ChannelAddress> getChannels() {
		return this.channels;
	}

	/**
	 * Gets the requested Resolution in [s].
	 *
	 * @return Resolution
	 */
	public int getResolution() {
		return this.resolution;
	}
}
