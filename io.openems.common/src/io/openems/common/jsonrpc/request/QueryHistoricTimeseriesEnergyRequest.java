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
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'queryHistoricTimeseriesEnergy'.
 *
 * <p>
 * This Request is for use-cases where you want to get the energy for one period
 * per Channel, e.g. to show the entire energy over a period as a text. The
 * energy is calculated by subtracting first value of the period ('fromDate')
 * from the last value of the period ('toDate').
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesEnergy",
 *   "params": {
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[],
 *     "timezone": String,
 *     "resolution": {
 *       "value": Number,
 *       "unit": {@link ChronoUnit}
 *     }
 *   }
 * }
 * </pre>
 */

public class QueryHistoricTimeseriesEnergyRequest extends JsonrpcRequest {

	public static final String METHOD = "queryHistoricTimeseriesEnergy";

	/**
	 * Create {@link AuthenticateWithPasswordRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticateWithPasswordRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static QueryHistoricTimeseriesEnergyRequest from(JsonrpcRequest r) throws OpenemsNamedException {
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
		var result = new QueryHistoricTimeseriesEnergyRequest(r, fromDate, toDate);
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

	private QueryHistoricTimeseriesEnergyRequest(JsonrpcRequest request, ZonedDateTime fromDate, ZonedDateTime toDate)
			throws OpenemsNamedException {
		super(request, QueryHistoricTimeseriesEnergyRequest.METHOD);

		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public QueryHistoricTimeseriesEnergyRequest(ZonedDateTime fromDate, ZonedDateTime toDate)
			throws OpenemsNamedException {
		super(QueryHistoricTimeseriesEnergyRequest.METHOD);

		this.fromDate = fromDate;
		this.toDate = toDate;
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
				.addProperty("fromDate", QueryHistoricTimeseriesEnergyRequest.FORMAT.format(this.fromDate)) //
				.addProperty("toDate", QueryHistoricTimeseriesEnergyRequest.FORMAT.format(this.toDate)) //
				.add("channels", channels) //
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

}
