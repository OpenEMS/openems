package io.openems.backend.alerting;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.gson.JsonElement;

import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.common.metadata.UserAlertingSettings;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.test.DummyMetadata;

public class Dummy {

	public static class MailerImpl implements Mailer {
		public final Map<ZonedDateTime, String> sentMails = new HashMap<>();

		@Override
		public void sendMail(ZonedDateTime sendAt, String template, JsonElement params) {
			this.sentMails.put(sendAt, template);
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

	public static class MetadataImpl extends SimpleMetadataImpl {
		private List<Edge> edges;
		private Map<String, List<UserAlertingSettings>> settings;

		/**
		 * Initialize Metadata with test data.
		 *
		 * @param edges    to add
		 * @param settings to add
		 */
		public void initialize(List<Edge> edges, Map<String, List<UserAlertingSettings>> settings) {
			this.edges = edges;
			this.settings = settings;
		}

		@Override
		public boolean isInitialized() {
			return true;
		}

		public Map<String, List<UserAlertingSettings>> getSettings() {
			return this.settings;
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
		public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
			return this.settings.get(edgeId);
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
}
