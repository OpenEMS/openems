package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.openems.backend.alerting.message.AlertingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OfflineEdgeHandler extends AbstractEdgeHandler {

	public OfflineEdgeHandler(MessageSchedulerService mss, //
							  Mailer mailer, //
							  Metadata metadata, //
							  int initialDelay, //
							  boolean useThisNotifier) {
		super(mss, mailer, metadata, initialDelay, useThisNotifier);
	}

	@Override
	protected boolean removePack(String id) {
		return this.isEdgeOnline(id);
	}

	private boolean isEdgeOnline(String edgeId) {
		var edge = this.metadata.getEdge(edgeId);
		return edge.map(Edge::isOnline).orElse(true);
	}

	@Override
	protected void logErrorMaxMsg() {
		this.log.error("[OfflineEdgeHandler] Canceled checkMetadata(); tried to schedule over " //
				+ MAX_SIMULTANEOUS_MSGS + " SumState Messages at once!!");
	}

	@Override
	protected void logErrorMaxEdges() {
		this.log.error("[OfflineEdgeHandler] Canceled checkMetadata(); tried to schedule msgs for "
				+ MAX_SIMULTANEOUS_MSGS + " SumState Messages at once!!");
	}

	@Override
	protected Collection<Edge> getRelevantEdges() {
		return this.metadata.getAllOfflineEdges();
	}

	@Override
	protected ZonedDateTime getTimeStampOfCriticalEvent(Edge edge) {
		return edge.getLastmessage();
	}

	@Override
	protected void logInfoMessage() {
		this.log.info("[OfflineEdgeHandler] check Metadata for Error or Warning Sum");
	}

	@Override
	protected AlertingMessage newMessageInstance(String id, ZonedDateTime lastMessage) {
		return new OfflineEdgeMessage(id, lastMessage);
	}

	@Override
	public Runnable getEventHandler(EventReader event) {

		return switch (event.getTopic()) {
			case Edge.Events.ON_SET_ONLINE -> () -> {
				var edgeId = event.getString(Edge.Events.OnSetOnline.EDGE_ID);
				var isOnline = event.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
				var edgeOpt = this.metadata.getEdge(edgeId);
				edgeOpt.ifPresentOrElse((edge) -> {
					// Ensure that the online-state has not changed
					if (edge.isOnline() == isOnline) {
						if (isOnline) {
							this.tryRemoveEdge(edge);
						} else {
							this.tryAddEdge(edge);
						}
					}
				}, () -> this.edgeNotFound(edgeId));
			};
			case Metadata.Events.AFTER_IS_INITIALIZED -> this::handleMetadataAfterInitialize;
			default -> null;
		};
	}

	@Override
	protected boolean isAlertEnabled(Edge edge, AlertingSetting setting) {
		return setting.getOfflineAlertDelayTime() > 0;
	}

	@Override
	protected int getAlertDelayTime(AlertingSetting setting) {
		return setting.getOfflineAlertDelayTime();
	}

	@Override
	protected ZonedDateTime getAlertLastNotification(AlertingSetting setting) {
		return setting.getOfflineAlertLastNotification();
	}
}
