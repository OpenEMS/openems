package io.openems.backend.metadata.dummy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.OpenemsOEM;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Dummy", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class DummyMetadata extends AbstractMetadata implements Metadata, EventHandler {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(DummyMetadata.class);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final EventAdmin eventAdmin;
	private final AtomicInteger nextUserId = new AtomicInteger(-1);
	private final AtomicInteger nextEdgeId = new AtomicInteger(-1);

	private final Map<String, User> users = new HashMap<>();
	private final Map<String, MyEdge> edges = new HashMap<>();

	private Language defaultLanguage = Language.DE;

	@Activate
	public DummyMetadata(@Reference EventAdmin eventadmin) {
		super("Metadata.Dummy");
		this.eventAdmin = eventadmin;
		this.logInfo(this.log, "Activate");

		// Allow the services some time to settle
		this.executor.schedule(() -> {
			this.setInitialized();
		}, 10, TimeUnit.SECONDS);
	}

	@Deactivate
	private void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		var name = "User #" + this.nextUserId.incrementAndGet();
		var token = UUID.randomUUID().toString();
		var roles = new TreeMap<String, Role>();
		for (String edgeId : this.edges.keySet()) {
			roles.put(edgeId, Role.ADMIN);
		}
		var user = new User(username, name, token, this.defaultLanguage, Role.ADMIN, roles);
		this.users.put(user.getId(), user);
		return user;
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		for (User user : this.users.values()) {
			if (user.getToken().equals(token)) {
				return user;
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	@Override
	public void logout(User user) {
		this.users.remove(user.getId(), user);
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		var edgeOpt = this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.findFirst();
		if (edgeOpt.isPresent()) {
			return Optional.ofNullable(edgeOpt.get().getId());
		}
		// not found. Is apikey a valid Edge-ID?
		var idOpt = DummyMetadata.parseNumberFromName(apikey);
		int id;
		String edgeId;
		String setupPassword;
		if (idOpt.isPresent()) {
			edgeId = apikey;
			id = idOpt.get();
		} else {
			// create new ID
			id = this.nextEdgeId.incrementAndGet();
			edgeId = "edge" + id;
		}
		setupPassword = edgeId;
		var edge = new MyEdge(this, edgeId, apikey, setupPassword, "OpenEMS Edge #" + id, "", "", Level.OK,
				new EdgeConfig());
		this.edges.put(edgeId, edge);
		return Optional.ofNullable(edgeId);

	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		var edgeOpt = this.edges.values().stream().filter(edge -> edge.getSetupPassword().equals(setupPassword))
				.findFirst();

		if (edgeOpt.isPresent()) {
			var edge = edgeOpt.get();
			return Optional.of(edge);
		}

		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		Edge edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge);
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}

	private static Optional<Integer> parseNumberFromName(String name) {
		try {
			var matcher = DummyMetadata.NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				var nameNumberString = matcher.group(1);
				return Optional.ofNullable(Integer.parseInt(nameNumberString));
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		return Optional.empty();
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		throw new NotImplementedException("DummyMetadata.addEdgeToUser()");
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		throw new NotImplementedException("DummyMetadata.getUserInformation()");
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new NotImplementedException("DummyMetadata.setUserInformation()");
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new IllegalArgumentException("DummyMetadata.getSetupProtocol() is not implemented");
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		throw new NotImplementedException("DummyMetadata.getSetupProtocolData() is not implemented");
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new IllegalArgumentException("DummyMetadata.submitSetupProtocol() is not implemented");
	}

	@Override
	public void registerUser(JsonObject jsonObject, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		throw new IllegalArgumentException("DummyMetadata.registerUser() is not implemented");
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
		this.defaultLanguage = language;
	}

	@Override
	public Optional<List<EdgeUser>> getUserToEdge(String edgeId) {
		throw new IllegalArgumentException("DummyMetadata.getUserToEdge() is not implemented");
	}

	@Override
	public EventAdmin getEventAdmin() {
		return this.eventAdmin;
	}

	@Override
	public void handleEvent(Event event) {
		EventReader reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_CONFIG:
			MyEdge edge = reader.getProperty(Edge.Events.OnSetConfig.EDGE);
			EdgeConfigDiff diff = reader.getProperty(Edge.Events.OnSetConfig.DIFF);

			this.logInfo(this.log,
					"Edge [" + edge.getId() + "]. Update config: " + StringUtils.toShortString(diff.getAsHtml(), 100));
			break;
		}
	}

	@Override
	public Optional<EdgeUser> getEdgeUserTo(String edgeId, String userId) {
		return Optional.empty();
	}

}
