package io.openems.backend.alerting.message;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.openems.backend.common.metadata.AlertingSetting;

public class SumStateMessage extends AlertingMessage {

    private SumStateMessage(String edgeId, ZonedDateTime warningOrFaultAt,
	    TreeMap<Integer, List<AlertingSetting>> map) {
	super(edgeId, warningOrFaultAt, map, AlertingTemplate.DEFAULT.templatePath);
    }

    public SumStateMessage(String edgeId, ZonedDateTime offlineAt) {
	this(edgeId, offlineAt, new TreeMap<>());
    }

    @Override
    public String toString() {
	var rec = this.getCurrentRecipients().stream().map(AlertingSetting::getUserId).collect(Collectors.joining(","));
	return "SumStateMessage{for=" + this.getEdgeId() + ", to=[" + rec + "], at=" + this.getNotifyStamp() + "}";
    }

}
