package io.openems.backend.alerting.message;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.alerting.Message;
import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.common.utils.JsonUtils;

public class OfflineEdgeMessage extends Message {

	public static final String TEMPLATE = "alerting_offline";

	private final ZonedDateTime offlineAt;
	private final TreeMap<Integer, List<OfflineEdgeAlertingSetting>> recipients;

	private OfflineEdgeMessage(String edgeId, ZonedDateTime offlineAt,
			TreeMap<Integer, List<OfflineEdgeAlertingSetting>> recipients) {
		super(edgeId);
		this.offlineAt = offlineAt;
		this.recipients = recipients;
	}

	public OfflineEdgeMessage(String edgeId, ZonedDateTime offlineAt) {
		this(edgeId, offlineAt, new TreeMap<>());
	}

	@Override
	public ZonedDateTime getNotifyStamp() {
		final var minutes = this.recipients.isEmpty() ? 0 : this.recipients.firstKey();
		return this.offlineAt.plusMinutes(minutes);
	}

	/**
	 * Add a recipient with its delay to the message.
	 *
	 * @param setting of user to whom to send the mail to
	 */
	public void addRecipient(OfflineEdgeAlertingSetting setting) {
		this.recipients.putIfAbsent(setting.delay(), new ArrayList<>());
		final var settings = this.recipients.get(setting.delay());
		settings.add(setting);
	}

	public List<OfflineEdgeAlertingSetting> getCurrentRecipients() {
		return this.recipients.get(this.recipients.firstKey());
	}

	public int getMessageCount() {
		return this.recipients.values().stream().mapToInt(List::size).sum();
	}

	/**
	 * Transform message to use for next cycle.
	 *
	 * @return next Message with updated Recipients
	 */
	public boolean update() {
		this.recipients.remove(this.recipients.firstKey());
		return !this.isEmpty();
	}

	public String getEdgeId() {
		return super.getId();
	}

	public boolean isEmpty() {
		return this.recipients.isEmpty();
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("recipients", JsonUtils.generateJsonArray(//
						this.getCurrentRecipients(), s -> new JsonPrimitive(s.userLogin())))//
				.addProperty("edgeId", this.getEdgeId()) //
				.build();
	}

	@Override
	public String toString() {
		final var rec = this.getCurrentRecipients().stream() //
				.map(s -> String.valueOf(s.userLogin())) //
				.collect(Collectors.joining(","));
		return OfflineEdgeMessage.class.getSimpleName() + "{for=" + this.getEdgeId() + ", to=[" + rec + "], at="
				+ this.getNotifyStamp() + "}";
	}
}
