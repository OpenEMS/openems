package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.timedata.api.TimedataSingleton;
import io.openems.backend.utilities.StringUtils;
import io.openems.common.exceptions.OpenemsException;

public class TimedataDummySingleton implements TimedataSingleton {
	private final Logger log = LoggerFactory.getLogger(TimedataDummySingleton.class);

	@Override
	public void write(Optional<Integer> deviceId, JsonObject jData) {
		log.debug("Timedata Dummy. Would write data: " + StringUtils.toShortString(jData, 100));
	}

	@Override
	public JsonArray queryHistoricData(int deviceId, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution) throws OpenemsException {
		log.info("Timedata Dummy. Would query data: From [" + fromDate + "], To [" + toDate + "] Channels [" + channels
				+ "] Resolution [" + resolution + "]");
		return new JsonArray();
	}
}
