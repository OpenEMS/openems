package io.openems.common.jsonrpc.request;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class QueryHistoricDataRequest extends JsonrpcRequest {

	public final static String METHOD = "queryHistoricData";

	private final static DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-DD");

	public static QueryHistoricDataRequest from(JsonrpcRequest r) throws OpenemsException {
		JsonObject p = r.getParams();
		int timezoneDiff = JsonUtils.getAsInt(p, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(p, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(p, "toDate", timezone).plusDays(1);
		JsonObject channels = JsonUtils.getAsJsonObject(p, "channels");
		return new QueryHistoricDataRequest(r.getId(), fromDate, toDate, channels);
	}

	public static QueryHistoricDataRequest from(JsonObject j) throws OpenemsException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final int timezoneDiff;
	private final ZonedDateTime fromDate;
	private final ZonedDateTime toDate;
	private final JsonObject channels;

	public QueryHistoricDataRequest(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels)
			throws OpenemsException {
		super(id, METHOD);

		this.timezoneDiff = ZoneOffset.from(fromDate).getTotalSeconds();
		if (timezoneDiff != ZoneOffset.from(toDate).getTotalSeconds()) {
			throw new OpenemsException("FromDate and ToDate need to be in the same timezone!");
		}

		this.fromDate = fromDate;
		this.toDate = toDate;
		this.channels = channels;
	}

	public QueryHistoricDataRequest(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels)
			throws OpenemsException {
		this(UUID.randomUUID(), fromDate, toDate, channels);
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("timezone", this.timezoneDiff) //
				.addProperty("fromDate", FORMAT.format(this.fromDate)) //
				.addProperty("toDate", FORMAT.format(this.toDate)) //
				.add("channels", this.channels) //
				.build();
	}
}
