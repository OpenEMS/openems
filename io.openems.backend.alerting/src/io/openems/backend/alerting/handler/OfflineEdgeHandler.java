package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.MinuteTimer;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OfflineEdgeHandler implements Handler<OfflineEdgeMessage> {

	public static final int MAX_SIMULTANEOUS_MSGS = 500;
	public static final int MAX_SIMULTANEOUS_EDGES = 1000;

	private final Logger log = LoggerFactory.getLogger(OfflineEdgeHandler.class);

	private final int initialDelay; // in Minutes
	private final Metadata metadata;
	private final Mailer mailer;

	private MessageSchedulerService mss;
	private MessageScheduler<OfflineEdgeMessage> msgScheduler;

	private Runnable initMetadata;

	public OfflineEdgeHandler(MessageSchedulerService mss, Mailer mailer, Metadata metadata, int initialDelay) {
		this.mailer = mailer;
		this.metadata = metadata;
		this.initialDelay = initialDelay;

		this.mss = mss;
		this.msgScheduler = mss.register(this);
		if (this.metadata.isInitialized()) {
			this.handleMetadataAfterInitialize();
		}
	}

	@Override
	public void stop() {
		if (this.initMetadata != null) {
			MinuteTimer.getInstance().unsubscribe(this.initMetadata);
		}
		this.initMetadata = null;
		this.mss.unregister(this);
		this.msgScheduler = null;
		this.mss = null;
	}

	@Override
	public void send(ZonedDateTime sentAt, List<OfflineEdgeMessage> pack) {
		// Ensure Edge is still offline before sending mail.
		pack.removeIf((msg) -> !this.isEdgeOffline(msg.getEdgeId()));

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
	 * @return true if vald
	 */
	private boolean isValidEdge(Edge edge) {
		var invalid = edge.getLastmessage() == null // was never online
				|| edge.getLastmessage() //
						.isBefore(ZonedDateTime.now().minusWeeks(1)); // already offline for a week
		return !invalid;
	}

	/**
	 * Add Edge to list, with calculated TimeStamp (at which to notify).
	 *
	 * @param edge to add
	 * @return {@link OfflineEdgeMessage} generated from edge
	 */
	protected OfflineEdgeMessage getEdgeMessage(Edge edge) {
		if (edge == null) {
			this.log.warn("Called method getEdgeMessage with edge=null");
			return null;
		}
		try {
			var alertingSettings = this.metadata.getUserAlertingSettings(edge.getId());
			if (alertingSettings == null || alertingSettings.isEmpty()) {
				return null;
			}
			var message = new OfflineEdgeMessage(edge.getId(), edge.getLastmessage());
			if (!message.isValid()) {
				this.log.warn("Invalid OfflineEdgeMessage " + message.toString());
				return null;
			}
			for (var setting : alertingSettings) {
				if (setting.getDelayTime() > 0 && this.shouldReceiveMail(edge, setting)) {
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

	private boolean shouldReceiveMail(Edge edge, AlertingSetting setting) {
		var lastMailRecievedAt = setting.getLastNotification();
		if (lastMailRecievedAt == null) {
			return true;
		}
		var edgeOfflineSince = edge.getLastmessage();
		if (lastMailRecievedAt.isAfter(edgeOfflineSince)) {
			return false;
		}
		var nextMailRecieveAt = edgeOfflineSince.plus(setting.getDelayTime(), ChronoUnit.MINUTES);
		return nextMailRecieveAt.isAfter(lastMailRecievedAt);
	}

	protected void tryRemoveEdge(Edge edge) {
		this.msgScheduler.remove(edge.getId());
	}

	protected void tryAddEdge(Edge edge) {
		if (this.isValidEdge(edge)) {
			var msg = this.getEdgeMessage(edge);
			if (msg != null) {
				this.msgScheduler.schedule(msg);
			}
		}
	}

	/**
	 * Check Metadata for all OfflineEdges. Waits given in initialDelay, before
	 * executing.
	 */
	private void handleMetadataAfterInitialize() {
		if (this.initialDelay <= 0) {
			this.checkMetadata();
		} else {
			this.initMetadata = new Runnable() {
				final ZonedDateTime checkAt = ZonedDateTime.now().plusMinutes(OfflineEdgeHandler.this.initialDelay);

				@Override
				public void run() {
					if (ZonedDateTime.now().isAfter(this.checkAt)) {
						OfflineEdgeHandler.this.checkMetadata();
						MinuteTimer.getInstance().unsubscribe(this);
						OfflineEdgeHandler.this.initMetadata = null;
					}
				}
			};
			MinuteTimer.getInstance().subscribe(this.initMetadata);
		}
	}

	@Override
	public Runnable getEventHandler(EventReader event) {
		
		return switch (event.getTopic()) {
		 case Edge.Events.ON_SET_ONLINE -> {
			 var edgeId = event.getString(Edge.Events.OnSetOnline.EDGE_ID);
			 var isOnline = event.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
			yield () -> {
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
				}, () -> {
					this.log.warn("Edge with id: " + edgeId + " not found");
				 });
		    	};
		   }
		  case Metadata.Events.AFTER_IS_INITIALIZED ->
		  		this::handleMetadataAfterInitialize; 

		  default -> null;
			
		};
	}

	@Override
	public Class<OfflineEdgeMessage> getGeneric() {
		return OfflineEdgeMessage.class;
	}
}
