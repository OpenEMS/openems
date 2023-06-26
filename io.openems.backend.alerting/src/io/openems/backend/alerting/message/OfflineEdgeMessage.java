package io.openems.backend.alerting.message;

import io.openems.backend.common.metadata.AlertingSetting;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;

public class OfflineEdgeMessage extends AlertingMessage {

    private OfflineEdgeMessage(String edgeId, ZonedDateTime warningOrFaultAt,
                            TreeMap<Integer, List<AlertingSetting>> map) {
        super(edgeId, warningOrFaultAt, map, AlertingTemplate.DEFAULT.templatePath);
    }

    public OfflineEdgeMessage(String edgeId, ZonedDateTime offlineAt) {
        this(edgeId, offlineAt, new TreeMap<>());
    }
}
