package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
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
import io.openems.backend.alerting.scheduler.MinuteTimer;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.utils.JsonUtils;

public class SumStateHandler implements Handler<SumStateMessage> {
	private final Logger log = LoggerFactory.getLogger(SumStateHandler.class);

	private final Metadata metadata;
	private final Mailer mailer;

	private MessageSchedulerService mss;
	private MessageScheduler<SumStateMessage> msgScheduler;

	private Runnable initMetadata;

	public SumStateHandler(MessageSchedulerService mss, Mailer mailer, Metadata metadata, int initialDelay) {
		this.mailer = mailer;
		this.metadata = metadata;

		this.mss = mss;
		this.msgScheduler = mss.register(this);
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
			this.log.warn("Called method getEdgeMessage with " //
					+ edge == null ? "Edge{null}" : "Edge{id=null}");
		}
		var message = new SumStateMessage(edge.getId(), sumState, ZonedDateTime.now());
		return message;
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
			this.log.debug("Schedule : " + msg);
			this.msgScheduler.schedule(msg);
		}
	}

	@Override
	public Consumer<EventReader> getEventHandler(String eventTopic) {
		switch (eventTopic) {
		case Edge.Events.ON_SET_SUM_STATE: {
			return (event) -> {
				final var edgeId = (String) event.getProperty(Edge.Events.OnSetSumState.EDGE_ID);
				final var level = (Level) event.getProperty(Edge.Events.OnSetSumState.SUM_STATE);

				if (this.testFilter(edgeId, level)) {
					return;
				}

				final var edgeOpt = this.metadata.getEdge(edgeId);
				edgeOpt.ifPresent(edge -> {
					// TODO remove Fault check with configuration.
					if (level.isAtLeast(Level.FAULT)) {
						this.tryAddEdge(edge, level);
					} else {
						this.tryRemoveEdge(edge);
					}
				});
			};
		}
		case Edge.Events.ON_SET_ONLINE: {
			return (event) -> {
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
			};
		}
		default:
			return null;
		}
	}

	@Override
	public Class<SumStateMessage> getGeneric() {
		return SumStateMessage.class;
	}

	// TODO remove this test filter; filter is for testing only
	private boolean testFilter(String edgeId, Level level) {
		switch (edgeId.toLowerCase()) {
		case "edge1":
		case "edge42":
			break;
		default:
			return true;
		}

		var contains = this.containsEdge(edgeId);
		if ((level.isAtLeast(Level.FAULT) == contains)) {
			return true;
		}

		if (level.isAtLeast(Level.FAULT)) {
			this.log.info(edgeId + " entered FAULT state!");
		} else {
			this.log.info(edgeId + " left FAULT state!");
		}

		return false;
	}
}
