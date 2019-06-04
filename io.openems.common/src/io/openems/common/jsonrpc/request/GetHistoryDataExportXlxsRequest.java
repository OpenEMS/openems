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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public class GetHistoryDataExportXlxsRequest extends JsonrpcRequest {

	public final static String METHOD = "getHistoryDataExportXlsx";

	private final static DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public static GetHistoryDataExportXlxsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();		
		int timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(p, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(p, "toDate", timezone).plusDays(1);
		GetHistoryDataExportXlxsRequest result = new GetHistoryDataExportXlxsRequest(r.getId(), fromDate, toDate);

		JsonArray datachannels = JsonUtils.getAsJsonArray(p, "dataChannels");
		for (JsonElement channel : datachannels) {
			ChannelAddress address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addDataChannel(address);
		}
		JsonArray energyChannels = JsonUtils.getAsJsonArray(p, "energyChannels");
		for (JsonElement channel : energyChannels) {
			ChannelAddress address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addEnergyChannel(address);
		}
		return result;

	}

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;
	private final TreeSet<ChannelAddress> dataChannels = new TreeSet<>();
	private final TreeSet<ChannelAddress> energyChannels = new TreeSet<>();

	public GetHistoryDataExportXlxsRequest(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate)
			throws OpenemsNamedException {
		super(id, METHOD);

		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		if (timezoneDiff != ZoneOffset.from(toDate).getTotalSeconds()) {
			throw new OpenemsException("FromDate and ToDate need to be in the same timezone!");
		}

		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	private void addDataChannel(ChannelAddress address) {
		this.dataChannels.add(address);
	}
	
	private void addEnergyChannel(ChannelAddress address) {
		this.energyChannels.add(address);
	}

	@Override
	public JsonObject getParams() {
		JsonArray dataChannels = new JsonArray();
		for (ChannelAddress address : this.dataChannels) {
			dataChannels.add(address.toString());
		}
		JsonArray energyChannels = new JsonArray();
		for (ChannelAddress address : this.dataChannels) {
			energyChannels.add(address.toString());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("timezone", this.timezoneDiff) //
				.addProperty("fromDate", FORMAT.format(this.fromDate)) //
				.addProperty("toDate", FORMAT.format(this.toDate)) //
				.add("dataChannels", dataChannels) //
				.add("energyChannels", energyChannels)
				.build();
	}

	public ZonedDateTime getFromDate() {
		return fromDate;
	}

	public ZonedDateTime getToDate() {
		return toDate;
	}

	public TreeSet<ChannelAddress> getDataChannels() {
		return dataChannels;
	}
	
	public TreeSet<ChannelAddress> getEnergyChannels() {
		return energyChannels;
	}

}
