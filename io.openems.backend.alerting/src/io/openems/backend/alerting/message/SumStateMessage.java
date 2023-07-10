package io.openems.backend.alerting.message;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.alerting.Message;
import io.openems.common.channel.Level;
import io.openems.common.utils.JsonUtils;

public class SumStateMessage extends Message {
	
	public static final String TEMPLATE = "alerting_sumState_email";

	// TODO implement delay settings; fixed delay is for testing only
	private static final Integer FIXED_DELAY_IN_MINUTES = 30;

	private Level sumState;
	private final ZonedDateTime stateSince;
	private ZonedDateTime lastMessage;

	public SumStateMessage(String edgeId, Level sumState, ZonedDateTime stateSince) {
		super(edgeId);
		this.stateSince = stateSince;
		this.lastMessage = stateSince;
		this.sumState = sumState;
	}

	@Override
	public ZonedDateTime getNotifyStamp() {
		return this.lastMessage.plusMinutes(FIXED_DELAY_IN_MINUTES);
	}

	public Level getSumState() {
		return this.sumState;
	}

	public String getEdgeId() {
		return super.getId();
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("recipients", new JsonArray()) //
				.addProperty("edgeId", this.getEdgeId()) //
				.addProperty("state", this.getSumState()) //
				.build();
	}

	@Override
	public String toString() {
		var localTime = this.getNotifyStamp().withZoneSameInstant(ZoneId.systemDefault()).toString();

		return "ErrorEdgeMessage{for=" + this.getEdgeId() + ", to=[service.projekt@fenecon.de], at=" + localTime //
				+ ", state=" + this.sumState.getName() + ", since=" + this.stateSince + "}";
	}

	/**
	 * Tell if mail should be sent.
	 * 
	 * @return true if mail should be sent
	 */
	public boolean shouldSend() {
		return this.stateSince.isEqual(this.lastMessage);
	}

	/**
	 * Update and prepare message for a next schedule.
	 * 
	 * @return true if should be rescheduled;
	 */
	public boolean update() {
		this.lastMessage = this.getNotifyStamp();
		return true;
	}
}
