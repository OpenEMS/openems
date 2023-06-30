package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
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
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[],
 *     "timezone": String,
 *     "resolution": {
 *       "value": Number,
 *       "unit": {@link ChronoUnit}
 *     }
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

		var jTimezone = JsonUtils.getAsPrimitive(p, "timezone");
		final ZoneId timezone;
		if (jTimezone.isNumber()) {
			// For UI version before 2022.4.0
			timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(JsonUtils.getAsInt(jTimezone) * -1));
		} else {
			timezone = TimeZone.getTimeZone(JsonUtils.getAsString(p, "timezone")).toZoneId();
		}

		var fromDate = JsonUtils.getAsZonedDateWithZeroTime(p, "fromDate", timezone);
		var toDate = JsonUtils.getAsZonedDateWithZeroTime(p, "toDate", timezone).plusDays(1);

		var jResolution = JsonUtils.getSubElement(p, "resolution");
		final Resolution resolution;
		if (jResolution.isJsonPrimitive()) {
			// For UI version before 2022.4.0
			resolution = new Resolution(JsonUtils.getAsInt(jResolution), ChronoUnit.SECONDS);
		} else {
			var value = JsonUtils.getAsInt(jResolution, "value");
			var unit = JsonUtils.getAsString(jResolution, "unit");
			resolution = new Resolution(value, unit);
		}

		var result = new QueryHistoricTimeseriesEnergyPerPeriodRequest(r, fromDate, toDate, resolution);
		var channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			var address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();
	private final Resolution resolution;

	private QueryHistoricTimeseriesEnergyPerPeriodRequest(JsonrpcRequest request, ZonedDateTime fromDate,
			ZonedDateTime toDate, Resolution resolution) throws OpenemsNamedException {
		super(request, QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD);

		this.fromDate = fromDate;
		this.toDate = toDate;
		this.resolution = resolution;
	}

	public QueryHistoricTimeseriesEnergyPerPeriodRequest(ZonedDateTime fromDate, ZonedDateTime toDate,
			Resolution resolution) throws OpenemsNamedException {
		super(QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD);

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
		return JsonUtils.buildJsonObject() //
				.addProperty("fromDate", QueryHistoricTimeseriesEnergyPerPeriodRequest.FORMAT.format(this.fromDate)) //
				.addProperty("toDate", QueryHistoricTimeseriesEnergyPerPeriodRequest.FORMAT.format(this.toDate)) //
				.add("channels", channels) //
				.add("resolution", JsonUtils.buildJsonObject() //
						.addProperty("unit", this.resolution.getUnit().name()) //
						.addProperty("value", this.resolution.getValue()) //
						.build()) //
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
	public Resolution getResolution() {
		return this.resolution;
	}
}
