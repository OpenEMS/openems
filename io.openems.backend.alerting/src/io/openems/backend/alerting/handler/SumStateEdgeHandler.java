package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.util.Collection;

import io.openems.backend.alerting.message.AlertingMessage;
import io.openems.backend.alerting.message.SumStateMessage;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;

public class SumStateEdgeHandler extends AbstractEdgeHandler {

    public SumStateEdgeHandler(MessageSchedulerService mss, //
	    Mailer mailer, //
	    Metadata metadata, //
	    int initialDelay, boolean enabled) {
	super(mss, mailer, metadata, initialDelay, enabled);
    }

    @Override
    public Runnable getEventHandler(EventReader event) {
	switch (event.getTopic()) {
	case Edge.Events.ON_SET_SUM_STATE:
	    var edge = (Edge) event.getObject(Edge.Events.OnSetSumState.EDGE);

	    if (edge.isOnline()) {
		var sumStateLevel = (Level) event.getObject(Edge.Events.OnSetSumState.SUM_STATE);
		return () -> {
		    if (sumStateLevel.isAtLeast(Level.INFO)) {
			// correct level check will be done in tryAddEdge()
			this.tryAddEdge(edge);
		    } else {
			this.tryRemoveEdge(edge);
		    }
		};
	    }
	    return null;
	case Metadata.Events.AFTER_IS_INITIALIZED:
	    return this::handleMetadataAfterInitialize;
	default:
	    return null;
	}

    }

    @Override
    protected boolean removePack(String id) {
	var edge = this.metadata.getEdge(id);
	return edge.filter(value -> value.getSumState() == Level.WARNING || value.getSumState() == Level.FAULT)
		.isEmpty();
    }

    @Override
    protected void logErrorMaxMsg() {
	this.log.error("[SumStateEdgeHandler] Canceled checkMetadata(); tried to schedule over " + MAX_SIMULTANEOUS_MSGS
		+ " SumState Messages at once!!");
    }

    @Override
    protected void logErrorMaxEdges() {
	this.log.error("[SumStateEdgeHandler] Canceled checkMetadata(); tried to schedule msgs for "
		+ MAX_SIMULTANEOUS_MSGS + " SumState Messages at once!!");
    }

    @Override
    protected void logInfoMessage() {
	this.log.info("[SumStateEdgeHandler] check Metadata for Error or Warning Sum");
    }

    @Override
    protected Collection<Edge> getRelevantEdges() {
	return this.metadata.getAllSumNotOkEdges();
    }

    @Override
    protected AlertingMessage newMessageInstance(String id, ZonedDateTime lastMessage) {
	return new SumStateMessage(id, lastMessage);
    }

    @Override
    protected ZonedDateTime getTimeStampOfCriticalEvent(Edge edge) {
	return edge.getLastSumStateChange();
    }

    @Override
    protected boolean isAlertEnabled(Edge edge, AlertingSetting setting) {
	if (setting.getSumStateAlertDelayTime() == 0 || edge.getSumState() == null
		|| setting.getSumStateAlertLevel() == null) {
	    return false;
	}
	return edge.getSumState().isAtLeast(setting.getSumStateAlertLevel());
    }

    @Override
    protected int getAlertDelayTime(AlertingSetting setting) {
	return setting.getSumStateAlertDelayTime();
    }

    @Override
    protected ZonedDateTime getAlertLastNotification(AlertingSetting setting) {
	return setting.getSumStateAlertLastNotification();
    }

}
