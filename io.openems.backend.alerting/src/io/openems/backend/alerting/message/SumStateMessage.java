package io.openems.backend.alerting.message;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.alerting.Message;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.common.channel.Level;
import io.openems.common.utils.JsonUtils;

public class SumStateMessage extends Message {

	public static final String TEMPLATE = "alerting_sum_state";

	private final List<SumStateAlertingSetting> recipients;

	private Level sumState;
	private ZonedDateTime stateSince;

	public SumStateMessage(String edgeId, Level sumState, ZonedDateTime stateSince,
			List<SumStateAlertingSetting> recipients) {
		super(edgeId);
		this.stateSince = stateSince;
		this.sumState = sumState;
		this.recipients = recipients;
	}

	public SumStateMessage(String edgeId, Level sumState, ZonedDateTime stateSince) {
		this(edgeId, sumState, stateSince, new ArrayList<>());
	}

	public Level getSumState() {
		return this.sumState;
	}

	public ZonedDateTime getStateSince() {
		return this.stateSince;
	}

	private OptionalInt minimumSetting() {
		var sumState = this.getSumState();
		return this.recipients.stream().mapToInt(r -> r.getDelay(sumState)).filter(i -> i > 0).min();
	}

	@Override
	public ZonedDateTime getNotifyStamp() {
		var min = this.minimumSetting().orElse(0);
		return this.getStateSince().plusMinutes(min);
	}

	public void setSumState(Level sumState, ZonedDateTime now) {
		this.stateSince = now;
		this.sumState = sumState;
	}

	public String getEdgeId() {
		return super.getId();
	}

	/**
	 * Get the list of settings, which are closest to be sent.
	 * 
	 * @return {@link List} of recipients
	 */
	public List<SumStateAlertingSetting> getCurrentRecipients() {
		var min = this.minimumSetting();
		if (min.isEmpty()) {
			return List.of();
		}
		var sumState = this.getSumState();
		return this.recipients.stream().filter(r -> r.getDelay(sumState) == min.getAsInt()).toList();
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("recipients", JsonUtils.generateJsonArray(//
						this.getCurrentRecipients(), s -> new JsonPrimitive(s.userLogin())))//
				.addProperty("edgeId", this.getEdgeId()) //
				.addProperty("state", this.getSumState().getName()) //
				.build();
	}

	@Override
	public String toString() {
		var localNotify = this.getNotifyStamp().withZoneSameInstant(ZoneId.systemDefault()).toString();
		var localSince = this.getStateSince().withZoneSameInstant(ZoneId.systemDefault()).toString();

		var rec = this.getCurrentRecipients().stream() //
				.map(s -> String.valueOf(s.userLogin())) //
				.collect(Collectors.joining(","));

		return SumStateMessage.class.getSimpleName() + "{for=" + this.getEdgeId() + ", to=[" + rec + "], at="
				+ localNotify //
				+ ", state=" + this.getSumState().getName() + ", since=" + localSince + "}";
	}

	/**
	 * Update and prepare message for a next schedule.
	 *
	 * @return true if should be rescheduled;
	 */
	public boolean update() {
		this.recipients.removeAll(this.getCurrentRecipients());
		return !this.isEmpty();
	}

	public boolean isEmpty() {
		return this.minimumSetting().isEmpty();
	}
}
