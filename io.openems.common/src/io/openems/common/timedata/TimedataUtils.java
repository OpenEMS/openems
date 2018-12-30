package io.openems.common.timedata;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;

public class TimedataUtils {

	public static JsonObject handle(CommonTimedataService timeDataService, JsonObject jMessageId,
			JsonObject jHistoricData, Tag... tags) throws OpenemsException {
		String mode = JsonUtils.getAsString(jHistoricData, "mode");

		if (mode.equals("query")) {
			/*
			 * Query historic data
			 */
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

			JsonArray jData = timeDataService.queryHistoricData(fromDate, toDate, channels, resolution, tags);
			return DefaultMessages.historicDataQueryReply(jMessageId, jData);
		}

		throw new OpenemsException("Undefined Timedata mode.");
	}

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	public static Integer parseNumberFromName(String name) throws OpenemsException {
		try {
			Matcher matcher = NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				String nameNumberString = matcher.group(1);
				return Integer.parseInt(nameNumberString);
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		throw new OpenemsException("Unable to parse number from name [" + name + "]");
	}

}
