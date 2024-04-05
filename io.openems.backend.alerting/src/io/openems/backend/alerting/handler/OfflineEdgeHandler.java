package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.TimedExecutor;
import io.openems.backend.alerting.scheduler.TimedExecutor.TimedTask;
import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OfflineEdgeHandler implements Handler<OfflineEdgeMessage> {

	// Definition of unrealistically high values for messages sent simultaneously
	public static final int MAX_SIMULTANEOUS_MSGS = 500;
	public static final int MAX_SIMULTANEOUS_EDGES = 1000;
	public static final int EDGE_REBOOT_MINUTES = 5;
	
	private final Logger log = LoggerFactory.getLogger(OfflineEdgeHandler.class);

	private final int initialDelay; // in Minutes
	private final Metadata metadata;
	private final Mailer mailer;

	private MessageSchedulerService mss;
	private MessageScheduler<OfflineEdgeMessage> msgScheduler;

	private TimedTask initMetadata;
	private TimedExecutor timeService;
	
	public OfflineEdgeHandler(MessageSchedulerService mss, TimedExecutor timeService, Mailer mailer, Metadata metadata,
			int initialDelay) {
		this.mailer = mailer;
		this.metadata = metadata;
		this.initialDelay = initialDelay;
		this.timeService = timeService;

		this.mss = mss;
		this.msgScheduler = mss.register(this);
		if (this.metadata.isInitialized()) {
			this.handleMetadataAfterInitialize(null);
		}
	}

	@Override
	public void stop() {
		this.timeService.cancel(this.initMetadata);
		this.initMetadata = null;
		this.mss.unregister(this);
		this.msgScheduler = null;
		this.mss = null;
	}

	@Override
	public void send(ZonedDateTime sentAt, List<OfflineEdgeMessage> pack) {
		// Ensure Edge is still offline before sending mail.
		pack.removeIf((msg) -> !this.isEdgeOffline(msg.getEdgeId()));
		if (pack.isEmpty()) {
			return;
		}

		var params = JsonUtils.generateJsonArray(pack, OfflineEdgeMessage::getParams);

		this.mailer.sendMail(sentAt, OfflineEdgeMessage.TEMPLATE, params);

		var logStr = new StringBuilder(pack.size() * 64);
		pack.forEach(msg -> {
			logStr.append(msg).append(", ");
			this.tryReschedule(msg);
		});
		this.log.info("Sent OfflineEdgeMsg: " + logStr.substring(0, logStr.length() - 2));
	}

	private void tryReschedule(OfflineEdgeMessage msg) {
		if (msg.update()) {
			this.msgScheduler.schedule(msg);
		}
	}

	private boolean isEdgeOffline(String edgeId) {
		var edge = this.metadata.getEdge(edgeId);
		if (edge.isPresent()) {
			return edge.get().isOffline();
		}
		return false;
	}

	private void checkMetadata() {
		this.log.info("[OfflineEdgeHandler] check Metadata for Offline Edges");

		var msgs = new LinkedList<OfflineEdgeMessage>();
		var count = new AtomicInteger();
		var validOfflineEges = this.metadata.getAllOfflineEdges().stream() //
				.filter(this::isValidEdge) //
				.toList();

		if (validOfflineEges.size() > OfflineEdgeHandler.MAX_SIMULTANEOUS_EDGES) {
			this.log.error("[OfflineEdgeHandler] Canceled checkMetadata(); tried to schedule msgs for "
					+ OfflineEdgeHandler.MAX_SIMULTANEOUS_EDGES + " Offline-Edges at once!!");
			return;
		}

		for (var edge : validOfflineEges) {
			var msg = this.getEdgeMessage(edge);
			if (msg == null) {
				continue;
			}

			var completeCnt = count.addAndGet(msg.getMessageCount());
			if (completeCnt > OfflineEdgeHandler.MAX_SIMULTANEOUS_MSGS) {
				this.log.error("[OfflineEdgeHandler] Canceled checkMetadata(); tried to schedule over "
						+ OfflineEdgeHandler.MAX_SIMULTANEOUS_MSGS + " EdgeOffline Messages at once!!");
				return;
			}

			msgs.add(msg);
		}

		msgs.forEach(this.msgScheduler::schedule);
	}

	/**
	 * Check if Edge is valid for Scheduling.
	 *
	 * @param edge to test
	 * @return true if valid
	 */
	private boolean isValidEdge(Edge edge) {
		var invalid = edge.getLastmessage() == null // was never online
				|| edge.getLastmessage() //
						.isBefore(this.timeService.now().minusWeeks(1)); // already offline for a week
		return !invalid;
	}

	/**
	 * Add Edge to list, with calculated TimeStamp (at which to notify).
	 *
	 * @param edge to add
	 * @return {@link OfflineEdgeMessage} generated from edge
	 */
	protected OfflineEdgeMessage getEdgeMessage(Edge edge) {
		if (edge == null || edge.getId() == null) {
			this.log.warn("Called method getEdgeMessage with " //
					+ (edge == null ? "Edge{null}" : "Edge{id=null}"));
			return null;
		}
		try {
			var alertingSettings = this.metadata.getEdgeOfflineAlertingSettings(edge.getId());
			if (alertingSettings == null || alertingSettings.isEmpty()) {
				return null;
			}
			var message = new OfflineEdgeMessage(edge.getId(), edge.getLastmessage());
			for (var setting : alertingSettings) {
				if (setting.delay() > 0 && this.shouldReceiveMail(edge, setting)) {
					message.addRecipient(setting);
				}
			}
			if (!message.isEmpty()) {
				return message;
			}
		} catch (OpenemsException e) {
			this.log.warn("Could not get alerting settings for " + edge.getId(), e);
		}
		return null;
	}

	private boolean shouldReceiveMail(Edge edge, OfflineEdgeAlertingSetting setting) {
		final var lastMailRecievedAt = setting.lastNotification();
		final var edgeOfflineSince = edge.getLastmessage();

		var hasNotRecievedMailYet = true;
		var neverRecievedAnyMail = lastMailRecievedAt == null;

		if (!neverRecievedAnyMail) {
			var nextMailRecieveAt = edgeOfflineSince.plus(setting.delay(), ChronoUnit.MINUTES);
			hasNotRecievedMailYet = nextMailRecieveAt.isAfter(lastMailRecievedAt);
		}

		return neverRecievedAnyMail || hasNotRecievedMailYet;
	}

	protected void tryRemoveEdge(Edge edge) {
		this.msgScheduler.remove(edge.getId());
	}

	protected void tryAddEdge(Edge edge) {
		if (this.isValidEdge(edge)) {
			var msg = this.getEdgeMessage(edge);
			var msgScheduler = this.msgScheduler;
			if (msg != null && msgScheduler != null) {
				this.msgScheduler.schedule(msg);
			}
		}
	}

	/**
	 * Check Metadata for all OfflineEdges. Waits given in initialDelay, before
	 * executing.
	 *
	 * @param event Event data
	 */
	private void handleMetadataAfterInitialize(EventReader event) {
		if (this.initialDelay <= 0) {
			this.checkMetadata();
		} else {
			var executeAt = this.timeService.now().plusMinutes(OfflineEdgeHandler.this.initialDelay);
			this.initMetadata = this.timeService.schedule(executeAt, (now) -> {
				this.checkMetadata();
			});
		}
	}

	private void handleOnSetOnline(EventReader event) {
		var edgeId = event.getString(Edge.Events.OnSetOnline.EDGE_ID);
		var isOnline = event.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);

		var edgeOpt = this.metadata.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			var edge = edgeOpt.get();
			/* Ensure that the online-state has not changed */
			if (edge.isOnline() == isOnline) {
				if (isOnline) {
					this.tryRemoveEdge(edge);
				} else {
					this.timeService.schedule(this.timeService.now().plusMinutes(EDGE_REBOOT_MINUTES), t -> {
						if (edge.isOffline()) {
							this.tryAddEdge(edge);
						}
					});
				}
			}
		} else {
			this.log.warn("Edge with id: " + edgeId + " not found");
		}
	}

	@Override
	public Consumer<EventReader> getEventHandler(String eventTopic) {
		return switch (eventTopic) {
		case Edge.Events.ON_SET_ONLINE:
			yield this::handleOnSetOnline;

		case Metadata.Events.AFTER_IS_INITIALIZED:
			yield this::handleMetadataAfterInitialize; 

		default:
			yield null;
		};
	}

	@Override
	public Class<OfflineEdgeMessage> getGeneric() {
		return OfflineEdgeMessage.class;
	}
}
