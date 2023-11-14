package io.openems.backend.alerting;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.gson.JsonElement;

import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.MinuteTimer;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.channel.Level;
import io.openems.common.test.TimeLeapClock;

public class Dummy {

	public static class MailerImpl implements Mailer {
		public record Mail(ZonedDateTime sentAt, String template) {
		}

		public final List<Mail> sentMails = new LinkedList<>();

		@Override
		public synchronized void sendMail(ZonedDateTime sendAt, String template, JsonElement params) {
			this.sentMails.add(new Mail(sendAt, template));
		}

		public int getMailsCount() {
			return this.sentMails.size();
		}
	}

	public static class MessageSchedulerServiceImpl implements MessageSchedulerService {
		public final List<MessageScheduler<? extends Message>> msgScheduler = new ArrayList<>();

		/**
		 * Find handler in MessageSchedulerService handler-list.
		 *
		 * @param <T>     type of handler
		 * @param handler to search for
		 * @return handler if found, else null
		 */
		@SuppressWarnings("unchecked")
		public <T extends Message> MessageScheduler<T> find(Handler<T> handler) {
			return (MessageScheduler<T>) this.msgScheduler.stream().filter(s -> s.isFor(handler)).findFirst()
					.orElseGet(null);
		}

		@Override
		public <T extends Message> MessageScheduler<T> register(Handler<T> handler) {
			var msgSch = new MessageScheduler<>(handler);
			this.msgScheduler.add(msgSch);
			return msgSch;
		}

		@Override
		public <T extends Message> void unregister(Handler<T> handler) {
			this.msgScheduler.removeIf(msgs -> msgs.isFor(handler));
		}
	}

	public static class SimpleMetadataImpl extends DummyMetadata {
		@Override
		public EventAdmin getEventAdmin() {
			return new EventAdminImpl();
		}
	}

	public static class AlertingMetadataImpl extends SimpleMetadataImpl {
		private Collection<Edge> edges;
		private Map<String, List<AlertingSetting>> alertingSettings;
		private Map<String, Level> sumStates = new HashMap<>(10);

		/**
		 * Initialize Metadata with test data for Alerting.
		 *
		 * @param edges    to add
		 * @param settings to add
		 */
		public void initialize(Collection<Edge> edges, Map<String, List<AlertingSetting>> settings) {
			this.edges = edges;
			this.alertingSettings = settings;
		}

		@Override
		public boolean isInitialized() {
			return true;
		}

		@Override
		public Optional<Edge> getEdge(String edgeId) {
			return this.edges.stream().filter(e -> e.getId() == edgeId).findFirst();
		}

		@Override
		public Collection<Edge> getAllOfflineEdges() {
			return this.edges.stream().filter(Edge::isOffline).toList();
		}

		@Override
		public List<AlertingSetting> getUserAlertingSettings(String edgeId) {
			return this.alertingSettings.get(edgeId);
		}

		public void setSumState(String edgeId, Level sumState) {
			this.sumStates.put(edgeId, sumState);
		}

		public Collection<Edge> getEdges() {
			return this.edges;
		}

		public Map<String, List<AlertingSetting>> getAlertingSettings() {
			return this.alertingSettings;
		}
	}

	public static class EventAdminImpl implements EventAdmin {
		private List<Event> lastEvents = new ArrayList<>();

		public EventAdminImpl() {

		}

		@Override
		public void postEvent(Event event) {
			this.lastEvents.add(event);
		}

		@Override
		public void sendEvent(Event event) {
			this.lastEvents.add(event);
		}
	}

	public static class TimeLeapMinuteTimer extends MinuteTimer {

		private final TimeLeapClock timeLeapClock;
		private int advanced = 0;

		public TimeLeapMinuteTimer(Instant instant) {
			this(new TimeLeapClock(instant));
		}

		private TimeLeapMinuteTimer(TimeLeapClock clock) {
			super(clock);
			this.timeLeapClock = clock;
		}

		/**
		 * Leap the given amount in minutes. executing cycle method every time.
		 *
		 * @param amount to leap in minutes
		 */
		public void leap(long amount) {
			for (int i = 0; i < amount; i++) {
				this.timeLeapClock.leap(1, ChronoUnit.MINUTES);
				this.advanced += 1;
				this.cycle();
			}
		}

		/**
		 * Try to advance the Clock to a specific amount of minutes after
		 * initialization. If the given point is ahead, the time will leap by the
		 * missing amount. If the given point is behind, nothing will happen.
		 * <p>
		 * A return value >=0 means, the clock has advanced the given amount in minutes
		 * with this call.
		 * </p>
		 * <p>
		 * A return value <0 means, the clock has already advanced the given amount
		 * above.
		 * </p>
		 *
		 * @param point to advance to
		 * @return difference
		 */
		public long leapTo(long point) {
			var advancement = point - this.advanced;
			if (advancement > 0) {
				this.leap(advancement);
			}
			return advancement;
		}

		/**
		 * Get the amount this Timer has advanced since it was initialized.
		 *
		 * @return total leapt minutes.
		 */
		public int advanced() {
			return this.advanced;
		}
	}

	public static Config testConfig(int initialDelay) {
		return new Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Config.class;
			}

			@Override
			public String webconsole_configurationFactory_nameHint() {
				return "Alerting";
			}

			@Override
			public int initialDelay() {
				return initialDelay;
			}
		};
	}
}
