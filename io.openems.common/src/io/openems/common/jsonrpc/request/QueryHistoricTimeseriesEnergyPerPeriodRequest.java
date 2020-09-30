package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeSet;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
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

	public final static String METHOD = "queryHistoricTimeseriesEnergyPerPeriod";

	private final static DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public static QueryHistoricTimeseriesEnergyPerPeriodRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		int timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(p, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(p, "toDate", timezone).plusDays(1);
		int resolution = JsonUtils.getAsInt(p, "resolution");
		QueryHistoricTimeseriesEnergyPerPeriodRequest result = new QueryHistoricTimeseriesEnergyPerPeriodRequest(r.getId(),
				fromDate, toDate, resolution);
		JsonArray channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			ChannelAddress address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	public static QueryHistoricTimeseriesEnergyPerPeriodRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();
	private final int resolution;

	public QueryHistoricTimeseriesEnergyPerPeriodRequest(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate,
			int resolution) throws OpenemsNamedException {
		super(id, METHOD);

		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		if (timezoneDiff != ZoneOffset.from(toDate).getTotalSeconds()) {
			throw new OpenemsException("FromDate and ToDate need to be in the same timezone!");
		}

		this.fromDate = fromDate;
		this.toDate = toDate;
		this.resolution = resolution;
	}

	public QueryHistoricTimeseriesEnergyPerPeriodRequest(ZonedDateTime fromDate, ZonedDateTime toDate, int resolution)
			throws OpenemsNamedException {
		this(UUID.randomUUID(), fromDate, toDate, resolution);
	}

	private void addChannel(ChannelAddress address) {
		this.channels.add(address);
	}

	@Override
	public JsonObject getParams() {
		JsonArray channels = new JsonArray();
		for (ChannelAddress address : this.channels) {
			channels.add(address.toString());
		}
		return JsonUtils.buildJsonObject().addProperty("timezone", this.timezoneDiff) //
				.addProperty("fromDate", FORMAT.format(this.fromDate)) //
				.addProperty("toDate", FORMAT.format(this.toDate)) //
				.add("channels", channels) //
				.addProperty("resolution", resolution) //
				.build();
	}

	public ZonedDateTime getFromDate() {
		return fromDate;
	}

	public ZonedDateTime getToDate() {
		return toDate;
	}

	public TreeSet<ChannelAddress> getChannels() {
		return channels;
	}

	public int getResolution() {
		return resolution;
	}
}
