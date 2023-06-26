package io.openems.backend.alerting.message;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.alerting.Message;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.common.utils.JsonUtils;

public abstract class AlertingMessage extends Message {

    public final String template;
    private final ZonedDateTime criticalEventTime;
    private final TreeMap<Integer, List<AlertingSetting>> recipients;

    protected AlertingMessage(String edgeId, ZonedDateTime criticalEventTime,
	    TreeMap<Integer, List<AlertingSetting>> recipients, String template) {
	super(edgeId);
	this.criticalEventTime = criticalEventTime;
	this.recipients = recipients;
	this.template = template;
    }

    @Override
    public ZonedDateTime getNotifyStamp() {
	var minutes = this.recipients.isEmpty() ? 0 : this.recipients.firstKey();
	return this.criticalEventTime.plusMinutes(minutes);
    }

    /**
     * Add a recipient with its delay to the message.
     *
     * @param setting   of user to whom to send the mail to
     * @param delayTime the time in minutes before acting.
     */
    public void addRecipient(AlertingSetting setting, int delayTime) {
	this.recipients.putIfAbsent(delayTime, new ArrayList<>());
	var settings = this.recipients.get(delayTime);
	settings.add(setting);
    }

    public List<AlertingSetting> getCurrentRecipients() {
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
    @Override
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
			this.getCurrentRecipients(), s -> new JsonPrimitive(s.getId())))//
		.addProperty("edgeId", this.getEdgeId()) //
		.build();
    }

    @Override
    public String toString() {
	var rec = this.getCurrentRecipients().stream().map(AlertingSetting::getUserId).collect(Collectors.joining(","));
	return "OfflineEdgeMessage{for=" + this.getEdgeId() + ", to=[" + rec + "], at=" + this.getNotifyStamp() + "}";
    }
}
