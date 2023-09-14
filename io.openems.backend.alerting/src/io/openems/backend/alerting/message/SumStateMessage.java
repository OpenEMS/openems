package io.openems.backend.alerting.message;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.alerting.Message;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.common.channel.Level;
import io.openems.common.utils.JsonUtils;

public class SumStateMessage extends Message {

	public static final String TEMPLATE = "alerting_sum_state";

	private Level sumState;
	private final ZonedDateTime stateSince;
	private ZonedDateTime lastMessage;
	private final TreeMap<Integer, List<SumStateAlertingSetting>> recipients;

	private SumStateMessage(String edgeId, Level sumState, ZonedDateTime stateSince,
			TreeMap<Integer, List<SumStateAlertingSetting>> recipients) {
		super(edgeId);
		this.stateSince = stateSince;
		this.lastMessage = stateSince;
		this.sumState = sumState;
		this.recipients = recipients;
	}

	public SumStateMessage(String edgeId, Level sumState, ZonedDateTime stateSince) {
		this(edgeId, sumState, stateSince, new TreeMap<>());
	}

	@Override
	public ZonedDateTime getNotifyStamp() {
		var minutes = this.recipients.isEmpty() ? 0 : this.recipients.firstKey();
		return this.stateSince.plusMinutes(minutes);
	}

	public Level getSumState() {
		return this.sumState;
	}

	public String getEdgeId() {
		return super.getId();
	}

	public List<SumStateAlertingSetting> getCurrentRecipients() {
		return this.recipients.get(this.recipients.firstKey());
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
		var localSince = this.stateSince.withZoneSameInstant(ZoneId.systemDefault()).toString();

		var rec = this.getCurrentRecipients().stream() //
				.map(s -> String.valueOf(s.userLogin())) //
				.collect(Collectors.joining(","));

		return SumStateMessage.class.getSimpleName() + "{for=" + this.getEdgeId() + ", to=[" + rec + "], at="
				+ localNotify //
				+ ", state=" + this.sumState.getName() + ", since=" + localSince + "}";
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
	 * Add a recipient with its delay to the message.
	 *
	 * @param setting of user to whom to send the mail to
	 * @param state   of edge
	 */
	public void addRecipient(SumStateAlertingSetting setting, Level state) {
		var delay = setting.getDelay(state);
		this.recipients.putIfAbsent(delay, new ArrayList<>());
		var settings = this.recipients.get(delay);
		settings.add(setting);
	}

	/**
	 * Update and prepare message for a next schedule.
	 *
	 * @return true if should be rescheduled;
	 */
	public boolean update() {
		this.recipients.remove(this.recipients.firstKey());
		return !this.isEmpty();
	}

	public boolean isEmpty() {
		return this.recipients.isEmpty();
	}
}
