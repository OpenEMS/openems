package io.openems.backend.alerting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.event.EventReader;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Alerting", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE, //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class Alerting extends AbstractOpenemsBackendComponent implements EventHandler {

	public static final int INITIAL_DELAY = 15; // Minutes

	private final Logger log = LoggerFactory.getLogger(Alerting.class);
	protected final ScheduleMessageService tasks;

	@Reference
	protected Metadata metadata;

	@Reference
	protected EventAdmin eventAdmin;

	@Reference
	protected Mailer mailer;

	public Alerting() {
		super("Alerting");

		this.tasks = new ScheduleMessageService(this, this.actionTimeout);
	}

	/**
	 * executed action after waiting time ends.
	 */
	private Consumer<Message> actionTimeout = (message) -> {
		var timeStamp = message.getTimeStamp();
		var listUser = message.getUser();
		var edgeId = message.getEdgeId();

		this.sendEmails(timeStamp, listUser, edgeId);
	};

	@Activate
	private void activate(Config config) {
		this.logInfo(this.log, "Activate");

		this.tasks.start();

		/* load all cached edges */
		if (this.metadata.isInitialized()) {
			this.checkMetadata();
		}
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");

		this.tasks.stop();
	}

	private void checkMetadata() {
		this.metadata.getAllEdges().forEach(edge -> {
			if (!edge.isOnline()) {
				this.tryAddEdge(edge);
			}
		});
	}

	/**
	 * add Edge to list, with calculated TimeStamp (at which to notify).
	 *
	 * @param edge to add
	 */
	private void tryAddEdge(Edge edge) {
		if (edge.getUser().isEmpty()) {
			return;
		}

		ZonedDateTime now = ZonedDateTime.now();
		Map<ZonedDateTime, List<EdgeUser>> edgeUsers = new TreeMap<>();
		edge.getUser().forEach(user -> {
			this.getNotifyStamp(edge, user).ifPresent(notifyStamp -> {
				if (notifyStamp.isBefore(now)) {
					notifyStamp = now;
				}

				edgeUsers.putIfAbsent(notifyStamp, new ArrayList<>());
				edgeUsers.get(notifyStamp).add(user);
			});
		});
		if (!edgeUsers.isEmpty()) {
			this.tasks.createTask(edgeUsers, edge.getId());
		}
	}

	/**
	 * send e-mail via mailer service.
	 *
	 * @param stamp  at with mail send was initialized
	 * @param user   list of recipients
	 * @param edgeId edge that went offline
	 */
	private void sendEmails(ZonedDateTime stamp, List<EdgeUser> user, String edgeId) {
		// log to Console
		this.logInfo(this.log, "send Email - to " + user.size() + " user");
		this.mailer.sendAlertingMail(stamp, user, edgeId);
	}

	/**
	 * get TimeStamp of next notification or null if no notification is needed.
	 *
	 * @param edge thats involved
	 * @param user that will receive the mail
	 * @return Optional of ZonedDateTime
	 */
	private Optional<ZonedDateTime> getNotifyStamp(Edge edge, EdgeUser user) {
		int timeToWait = user.getTimeToWait();

		// timeToWait <= 0 equals OFF
		if (timeToWait <= 0) {
			return Optional.ofNullable(null);
		} else {
			ZonedDateTime lastOnline = edge.getLastMessageTimestamp();
			if (lastOnline == null) {
				// If the System was never Online
				this.logDebug(this.log, "[" + edge.getId() + "] has no TimeStamp");
				return Optional.ofNullable(null);
			} else {
				// Last TimeStamp at which the Edge was Online
				ZonedDateTime lastStamp = user.getLastNotification(ZoneId.systemDefault());
				// The TimeStamp at which to send the notification
				ZonedDateTime notifyStamp = lastOnline.withZoneSameInstant(ZoneId.systemDefault()) //
						.plus(timeToWait, ChronoUnit.MINUTES);
				// If Notification TimeStamp is before lastOnline => Mail was already sent
				if (lastStamp != null && !notifyStamp.isAfter(lastStamp)) {
					notifyStamp = null;
				}
				return Optional.ofNullable(notifyStamp);
			}
		}
	}

	/**
	 * Handler for when the Edge.OnSetOnline Event was thrown.
	 *
	 * @param reader Reader for Event parameters
	 */
	private void handleEdgeOnSetOnline(EventReader reader) {
		boolean isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
		Edge edge = reader.getProperty(Edge.Events.OnSetOnline.EDGE);

		if (isOnline) {
			this.tasks.removeAll(edge.getId());
		} else {
			this.tryAddEdge(edge);
		}
	}

	/**
	 * Hander for when the Metadata.AfterInitialize Event was thrown.
	 *
	 * @param reader EventReader for parameters
	 */
	private void handleMetadataAfterInitialize(EventReader reader) {
		Executors.newSingleThreadScheduledExecutor().schedule(() -> {
			this.checkMetadata();
		}, Alerting.INITIAL_DELAY, TimeUnit.MINUTES);
	}

	@Override
	public void handleEvent(Event event) {
		EventReader reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_ONLINE:
			this.handleEdgeOnSetOnline(reader);
			break;

		case Metadata.Events.AFTER_IS_INITIALIZED:
			this.handleMetadataAfterInitialize(reader);
			break;
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logDebug(Logger log, String message) {
		super.logDebug(log, message);
	}
}
