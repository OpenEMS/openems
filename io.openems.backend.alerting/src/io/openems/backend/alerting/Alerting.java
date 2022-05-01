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
	protected Mailer notifier;

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

		this.sendEmails(timeStamp, listUser);
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
	 * send e-mail via notifier service.
	 *
	 * @param stamp at with mail send was initialized
	 * @param user  list of recipents
	 */
	private void sendEmails(ZonedDateTime stamp, List<EdgeUser> user) {
		// log to Console
		this.logInfo(this.log, "send Email - to " + user.size() + " user");
		this.notifier.sendAlertingMail(stamp, user);
	}

	/**
	 * get TimeStamp of next notification or null if no notification is needed.
	 *
	 * @param edge thats involved
	 * @param user that will recieve the mail
	 * @return Optional of ZonedDateTime
	 */
	private Optional<ZonedDateTime> getNotifyStamp(Edge edge, EdgeUser user) {
		ZonedDateTime lastStamp = user.getLastNotification(ZoneId.systemDefault());
		ZonedDateTime lastOnline = edge.getLastMessageTimestamp();

		ZonedDateTime notifyStamp = null;
		if (lastOnline == null) {
			this.logDebug(this.log, "[" + edge.getId() + "] has no TimeStamp");
		} else {
			int timeToWait = user.getTimeToWait();

			// tmeToWait <= 0 equals OFF
			if (timeToWait > 0) {
				notifyStamp = lastOnline.withZoneSameInstant(ZoneId.systemDefault()) //
						.plus(timeToWait, ChronoUnit.MINUTES);

				if (lastStamp != null && !notifyStamp.isAfter(lastStamp)) {
					notifyStamp = null;
				}
			}
		}
		return Optional.ofNullable(notifyStamp);
	}

	private void handleEdgeOnSetOnline(EventReader reader) {
		boolean isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
		Edge edge = reader.getProperty(Edge.Events.OnSetOnline.EDGE);

		if (isOnline) {
			this.tasks.removeAll(edge.getId());
		} else {
			this.tryAddEdge(edge);
		}
	}

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
