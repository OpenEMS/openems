package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.backend.timedata.api.TimedataSingleton;
import io.openems.backend.utilities.StringUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

public class TimedataDummySingleton implements TimedataSingleton {
	private final Logger log = LoggerFactory.getLogger(TimedataDummySingleton.class);

	@Override
	public void write(MetadataDevices devices, JsonObject jData) {
		log.debug("Timedata Dummy. Would write data: " + StringUtils.toShortString(jData, 100));
	}

	@Override
	public JsonArray queryHistoricData(Optional<Integer> deviceIdOpt, ZonedDateTime fromDate, ZonedDateTime toDate,
			JsonObject channels, int resolution) throws OpenemsException {
		log.info("Timedata Dummy. Would query data: From [" + fromDate + "], To [" + toDate + "] Channels [" + channels
				+ "] Resolution [" + resolution + "]");
		return new JsonArray();
	}

	@Override
	public Optional<Object> getChannelValue(int deviceId, ChannelAddress channelAddress) {
		log.info("Timedata Dummy has no cache...");
		return Optional.empty();
	}
}
