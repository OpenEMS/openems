package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class SumStateHandler implements Handler<SumStateMessage> {
	private final Map<String, ZonedDateTime> faultSince = new TreeMap<>();

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

		var params = JsonUtils.generateJsonArray(pack.stream().map(SumStateMessage::getParams).toList());
		if (!params.isEmpty()) {
			this.mailer.sendMail(sentAt, SumStateMessage.TEMPLATE, params);
		}

		var logStrBuilder = new StringBuilder(pack.size() * 64);
		pack.forEach(msg -> {
			logStrBuilder.append(msg).append(", ");
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
			return this.isSevere(sumState.get());
		} else {
			return false;
		}
	}

	private boolean isSevere(Level level) {
		return level != null && level.isAtLeast(Level.WARNING);
	}

	/**
	 * Add Edge to list, with calculated TimeStamp (at which to notify).
	 *
	 * @param edge     to add
	 * @param sumState of edge
	 * @return {@link OfflineEdgeMessage} generated from edge
	 * @throws OpenemsException on any error
	 */
	protected SumStateMessage getEdgeMessage(Edge edge, Level sumState) throws OpenemsException {
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
			var message = new SumStateMessage(edge.getId(), sumState, this.timeService.now(), sumStateSettings);
			return message;
		} catch (OpenemsException e) {
			throw new OpenemsException("Could not get alerting settings for " + edge.getId(), e);
		}
	}

	protected void tryRemoveEdge(String edgeId) {
		this.msgScheduler.remove(edgeId);
	}

	protected void addOrUpdate(Edge edge, Level sumState) {
		var oldMsg = this.msgScheduler.remove(edge.getId());
		if (oldMsg == null) {
			if (this.faultSince.containsKey(edge.getId())) {
				return;
			}

			try {
				var newMsg = this.getEdgeMessage(edge, sumState);
				if (newMsg != null && !newMsg.isEmpty()) {
					this.msgScheduler.schedule(newMsg);
				}
			} catch (OpenemsException ex) {
				this.log.warn(ex.getMessage());
			}
		} else {
			if (oldMsg.getSumState() != sumState) {
				oldMsg.setSumState(sumState, this.timeService.now());
			}
			if (!oldMsg.isEmpty()) {
				this.msgScheduler.schedule(oldMsg);
			}
		}
	}

	private void handleEdgeOnSetOnline(EventReader event) {
		final var edgeId = event.getString(Edge.Events.OnSetOnline.EDGE_ID);
		final var online = event.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);

		if (!online) {
			this.tryRemoveEdge(edgeId);
		}
	}

	private void handleEdgeOnSetSumState(EventReader event) {
		final var edgeId = event.getString(Edge.Events.OnSetSumState.EDGE_ID);
		final var level = (Level) event.getProperty(Edge.Events.OnSetSumState.SUM_STATE);

		if (this.isSevere(level)) {
			final var edgeOpt = this.metadata.getEdge(edgeId);
			if (edgeOpt.isEmpty()) {
				this.log.warn("Edge with id '" + edgeId + "' was not found!");
			}
			this.addOrUpdate(edgeOpt.get(), level);

			this.faultSince.putIfAbsent(edgeId, this.timeService.now());
		} else {
			this.tryRemoveEdge(edgeId);

			this.faultSince.remove(edgeId);
		}
	}

	@Override
	public Consumer<EventReader> getEventHandler(String eventTopic) {
		return switch (eventTopic) {
		case Edge.Events.ON_SET_ONLINE -> this::handleEdgeOnSetOnline;
		case Edge.Events.ON_SET_SUM_STATE -> this::handleEdgeOnSetSumState;
		default -> null;
		};
	}

	@Override
	public Class<SumStateMessage> getGeneric() {
		return SumStateMessage.class;
	}
}
