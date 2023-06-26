package io.openems.backend.alerting.handler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.message.AlertingMessage;
import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.MinuteTimer;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public abstract class AbstractEdgeHandler implements Handler<AlertingMessage> {

    public static final int MAX_SIMULTANEOUS_MSGS = 500;
    public static final int MAX_SIMULTANEOUS_EDGES = 1000;

    protected final Logger log = LoggerFactory.getLogger(OfflineEdgeHandler.class);
    protected final Metadata metadata;

    private final int initialDelay; // in Minutes
    protected boolean enabled;
    private final Mailer mailer;
    private final AtomicReference<String> messageTemplate = new AtomicReference<>("");

    private Runnable initMetadata;
    private MessageScheduler<AlertingMessage> msgScheduler;
    private MessageSchedulerService mss;

    /**
     * This is used to check if the pack has to be removed called on send method.
     * 
     * @param id the msg Id / EdgeId
     * @return true if remove Pack.
     */
    protected abstract boolean removePack(String id);

    protected abstract AlertingMessage newMessageInstance(String id, ZonedDateTime lastMessage);

    protected abstract boolean isAlertEnabled(Edge edge, AlertingSetting setting);

    protected abstract int getAlertDelayTime(AlertingSetting setting);

    protected abstract ZonedDateTime getAlertLastNotification(AlertingSetting setting);

    protected abstract void logErrorMaxMsg();

    protected abstract void logErrorMaxEdges();

    protected abstract void logInfoMessage();

    protected abstract Collection<Edge> getRelevantEdges();

    protected abstract ZonedDateTime getTimeStampOfCriticalEvent(Edge edge);

    public AbstractEdgeHandler(MessageSchedulerService mss, Mailer mailer, Metadata metadata, int initialDelay,
	    boolean enabled) {
	this.mss = mss;
	this.mailer = mailer;
	this.metadata = metadata;
	this.initialDelay = initialDelay;
	this.msgScheduler = mss.register(this);
	this.enabled = enabled;

	// TODO cle check this, metadata.isInitialized() can be false because metadata
	// may need several seconds/minutes to start
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
    public void send(ZonedDateTime sentAt, List<AlertingMessage> pack) {
	pack.removeIf((msg) -> this.removePack(msg.getId()));
	pack.stream().findAny().ifPresent(message -> {
	    this.messageTemplate.set(message.template);
	});
	var params = JsonUtils.generateJsonArray(pack, AlertingMessage::getParams);
	this.mailer.sendMail(sentAt, this.messageTemplate.get(), params);

	var logStr = new StringBuilder(pack.size() * 64);
	pack.forEach(msg -> {
	    logStr.append(msg).append(", ");
	    this.tryReschedule(msg);
	});
	this.log.info("Sent EdgeMsg: " + logStr.substring(0, logStr.length() - 2));
    }

    private void tryReschedule(AlertingMessage msg) {
	if (msg.update()) {
	    this.msgScheduler.schedule(msg);
	}
    }

    private void checkMetadata() {
	this.logInfoMessage();

	var msgs = new LinkedList<AlertingMessage>();
	var count = new AtomicInteger();
	var edgeList = this.getRelevantEdges().stream() //
		.filter(this::isValidEdge) //
		.collect(Collectors.toList());

	if (edgeList.size() > MAX_SIMULTANEOUS_EDGES) {
	    this.logErrorMaxEdges();
	    return;
	}
	try {
	    edgeList.forEach(edge -> {
		var msg = this.getEdgeMessage(edge);
		if (msg != null) {
		    var completeCnt = count.addAndGet(msg.getMessageCount());
		    if (completeCnt > MAX_SIMULTANEOUS_MSGS) {
			this.logErrorMaxMsg();
			throw new RuntimeException("Too Many Simultaneous messages");
		    } else {
			msgs.add(msg);
		    }
		}
	    });
	    msgs.forEach(this.msgScheduler::schedule);
	} catch (RuntimeException e) {
	    return;
	}
    }

    /**
     * Check if Edge is valid for Scheduling.
     *
     * @param edge to test
     * @return true if vald
     */
    protected boolean isValidEdge(Edge edge) {
	var invalid = edge.getLastmessage() == null // was never online
		|| edge.getLastmessage() //
			.isBefore(ZonedDateTime.now().minusWeeks(1)); // already offline for a week
	return !invalid;
    }

    protected AlertingMessage getEdgeMessage(Edge edge) {
	if (edge == null) {
	    this.log.warn("Called method getEdgeMessage with edge=null");
	    return null;
	}
	try {
	    var alertingSettings = this.metadata.getUserAlertingSettings(edge.getId());
	    if (alertingSettings == null || alertingSettings.isEmpty()) {
		return null;
	    }
	    var message = this.newMessageInstance(edge.getId(), edge.getLastmessage());
	    if (!message.isValid()) {
		this.log.warn("Invalid OfflineEdgeMessage " + message);
		return null;
	    }
	    alertingSettings.forEach(setting -> {
		if (this.getAlertDelayTime(setting) > 0 && this.shouldReceiveMail(edge, setting)) {
		    message.addRecipient(setting, this.getAlertDelayTime(setting));
		}
	    });
	    if (!message.isEmpty()) {
		return message;
	    }
	} catch (OpenemsException e) {
	    this.log.warn("Could not get alerting settings for " + edge.getId(), e);
	}
	return null;
    }

    private boolean shouldReceiveMail(Edge edge, AlertingSetting setting) {
	if (!this.isAlertEnabled(edge, setting)) {
	    return false;
	}
	var lastMailReceivedAt = this.getAlertLastNotification(setting);
	if (lastMailReceivedAt == null) {
	    return true;
	}

	// Children should tell parent the "critical" time when an event occurred
	var edgeCriticalEventTime = this.getTimeStampOfCriticalEvent(edge);
	if (edgeCriticalEventTime == null) {
	    return true;
	}

	if (lastMailReceivedAt.isAfter(edgeCriticalEventTime)) {
	    return false;
	}
	var nextMailRecieveAt = edgeCriticalEventTime.plus(this.getAlertDelayTime(setting), ChronoUnit.MINUTES);
	return nextMailRecieveAt.isAfter(lastMailReceivedAt);
    }

    /**
     * Check Metadata for all OfflineEdges. Waits given in initialDelay, before
     * executing.
     */
    protected void handleMetadataAfterInitialize() {
	if (this.initialDelay <= 0) {
	    this.checkMetadata();
	} else {
	    this.initMetadata = new Runnable() {
		final ZonedDateTime checkAt = ZonedDateTime.now().plusMinutes(AbstractEdgeHandler.this.initialDelay);

		@Override
		public void run() {
		    if (ZonedDateTime.now().isAfter(this.checkAt)) {
			AbstractEdgeHandler.this.checkMetadata();
			MinuteTimer.getInstance().unsubscribe(this);
			AbstractEdgeHandler.this.initMetadata = null;
		    }
		}
	    };
	    MinuteTimer.getInstance().subscribe(this.initMetadata);
	}
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

    protected void edgeNotFound(String edgeId) {
	this.log.warn("Edge with id: " + edgeId + " not found");
    }

    public Class<AlertingMessage> getGeneric() {
	return AlertingMessage.class;
    }

    public boolean isEnabled() {
	return this.enabled;
    }

}
