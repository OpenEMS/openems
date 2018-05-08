package io.openems.edge.timedata.api;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.doc.Doc;

@ProviderType
public interface Timedata {

	// TODO merge this Service with the corresponding Backend Service

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * TODO copied from backend.timedata.api
	 * 
	 * @param jHistoricData
	 * @return
	 * @throws OpenemsException
	 */
	default JsonArray queryHistoricData(JsonObject jHistoricData) throws OpenemsException {
		int timezoneDiff = JsonUtils.getAsInt(jHistoricData, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jHistoricData, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jHistoricData, "toDate", timezone).plusDays(1);
		JsonObject channels = JsonUtils.getAsJsonObject(jHistoricData, "channels");
		// TODO check if role is allowed to read these channels
		// JsonObject kWh = JsonUtils.getAsJsonObject(jQuery, "kWh");
		int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		// TODO better calculation of sensible resolution
		int resolution = 10 * 60; // 10 Minutes
		if (days > 25) {
			resolution = 24 * 60 * 60; // 1 Day
		} else if (days > 6) {
			resolution = 3 * 60 * 60; // 3 Hours
		} else if (days > 2) {
			resolution = 60 * 60; // 60 Minutes
		}
		return this.queryHistoricData(fromDate, toDate, channels, resolution);
	}

	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution/* , JsonObject kWh */) throws OpenemsException;

}
