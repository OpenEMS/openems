package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.message.OfflineEdgeMessage;
import io.openems.backend.alerting.message.SumStateMessage;
import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.TimedExecutor;
import io.openems.backend.alerting.scheduler.TimedExecutor.TimedTask;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class SumStateHandler implements Handler<SumStateMessage> {
	private final Logger log = LoggerFactory.getLogger(SumStateHandler.class);

	private final Metadata metadata;
	private final Mailer mailer;

	private MessageSchedulerService mss;
	private MessageScheduler<SumStateMessage> msgScheduler;

	private TimedTask initMetadata;
	private TimedExecutor timeService;

	public SumStateHandler(MessageSchedulerService mss, TimedExecutor timeService, Mailer mailer, Metadata metadata,
			int initialDelay) {
		this.mailer = mailer;
		this.metadata = metadata;
		this.timeService = timeService;

		this.mss = mss;
		this.msgScheduler = mss.register(this);
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
	public void send(ZonedDateTime sentAt, List<SumStateMessage> pack) {
		// Ensure Edge is still in error state before sending mail.
		pack.removeIf((msg) -> !this.isEdgeError(msg.getEdgeId()));

		var params = JsonUtils
				.generateJsonArray(pack.stream().filter(m -> m.shouldSend()).map(SumStateMessage::getParams).toList());
		if (!params.isEmpty()) {
			this.mailer.sendMail(sentAt, SumStateMessage.TEMPLATE, params);
		}

		var logStrBuilder = new StringBuilder(pack.size() * 64);
		pack.forEach(msg -> {
			if (msg.shouldSend()) {
				logStrBuilder.append(msg).append(", ");
			}
			this.tryReschedule(msg);
		});
		var logStr = logStrBuilder.toString();
		if (!logStr.isBlank()) {
			this.log.info("Sent ErrorEdgeMsg: " + logStr);
		}
	}

	private void tryReschedule(SumStateMessage msg) {
		if (msg.update()) {
			this.msgScheduler.schedule(msg);
		}
	}

	private boolean isEdgeError(String edgeId) {
		var sumState = this.metadata.getSumState(edgeId);
		if (sumState.isPresent()) {
			return sumState.get().isAtLeast(Level.FAULT);
		} else {
			return false;
		}
	}

	/**
	 * Add Edge to list, with calculated TimeStamp (at which to notify).
	 *
	 * @param edge     to add
	 * @param sumState of edge
	 * @return {@link OfflineEdgeMessage} generated from edge
	 */
	protected SumStateMessage getEdgeMessage(Edge edge, Level sumState) {
		if (edge == null || edge.getId() == null) {
			this.log.warn("Called method SumStateHandler.getEdgeMessage with " //
					+ edge == null ? "Edge{null}" : "Edge{id=null}");
			return null;
		} else if (edge.isOffline()) {
			this.log.warn("Called method SumStateHandler.getEdgeMessage with offline" //
					+ "Edge{id=" + edge.getId() + '}');
			return null;
		}
		try {
			var sumStateSettings = this.metadata.getSumStateAlertingSettings(edge.getId());
			if (sumStateSettings == null || sumStateSettings.isEmpty()) {
				return null;
			}
			var message = new SumStateMessage(edge.getId(), sumState, this.timeService.now());
			for (var setting : sumStateSettings) {
				var delay = setting.getDelay(sumState);
				if (delay > 0 && this.shouldReceiveMail(edge, setting, sumState)) {
					message.addRecipient(setting, sumState);
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

	private boolean containsEdge(String edgeId) {
		return this.msgScheduler.isScheduled(msg -> {
			return Objects.equals(msg.getEdgeId(), edgeId);
		});
	}

	protected void tryRemoveEdge(Edge edge) {
		this.msgScheduler.remove(edge.getId());
	}

	protected void tryAddEdge(Edge edge, Level sumState) {
		var msg = this.getEdgeMessage(edge, sumState);
		if (msg != null) {
			this.msgScheduler.schedule(msg);
		}
	}

	private boolean shouldReceiveMail(Edge edge, SumStateAlertingSetting setting, Level state) {
		var lastMailRecievedAt = setting.lastNotification();
		if (lastMailRecievedAt == null) {
			return true;
		}
		var nextMailRecieveAt = edge.getLastmessage().plus(setting.getDelay(state), ChronoUnit.MINUTES);
		return nextMailRecieveAt.isAfter(lastMailRecievedAt);
	}

	private void handleEdgeOnSetOnline(EventReader event) {
		final var edgeId = event.getString(Edge.Events.OnSetOnline.EDGE_ID);

		// Exit if no message for edge exists
		if (!this.containsEdge(edgeId)) {
			return;
		}

		final var edgeOpt = this.metadata.getEdge(edgeId);
		edgeOpt.ifPresent((edge) -> {
			if (edge.isOffline()) {
				// remove sumState message, as this condition triggers offline edge message.
				this.tryRemoveEdge(edge);
			}
		});
	}

	private void handleEdgeOnSetSumState(EventReader event) {
		final var edgeId = (String) event.getProperty(Edge.Events.OnSetSumState.EDGE_ID);
		final var level = (Level) event.getProperty(Edge.Events.OnSetSumState.SUM_STATE);

		final var edgeOpt = this.metadata.getEdge(edgeId);
		edgeOpt.ifPresent(edge -> {
			if (level.isAtLeast(Level.WARNING)) {
				this.tryAddEdge(edge, level);
			} else {
				this.tryRemoveEdge(edge);
			}
		});
	}

	@Override
	public Consumer<EventReader> getEventHandler(String eventTopic) {
		return switch (eventTopic) {
		case Edge.Events.ON_SET_ONLINE:
			yield this::handleEdgeOnSetOnline;

		case Edge.Events.ON_SET_SUM_STATE:
			yield this::handleEdgeOnSetSumState;

		default:
			yield null;
		};
	}

	@Override
	public Class<SumStateMessage> getGeneric() {
		return SumStateMessage.class;
	}
}
