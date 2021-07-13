package io.openems.backend.metadata.dummy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Edge.State;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Dummy", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DummyMetadata extends AbstractMetadata implements Metadata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(DummyMetadata.class);

	private final AtomicInteger nextUserId = new AtomicInteger(-1);
	private final AtomicInteger nextEdgeId = new AtomicInteger(-1);

	private final Map<String, User> users = new HashMap<>();
	private final Map<String, MyEdge> edges = new HashMap<>();

	public DummyMetadata() {
		super("Metadata.Dummy");
		this.setInitialized();
	}

	@Activate
	void activate() {
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		String name = "User #" + this.nextUserId.incrementAndGet();
		String token = UUID.randomUUID().toString();
		TreeMap<String, Role> roles = new TreeMap<>();
		for (String edgeId : this.edges.keySet()) {
			roles.put(edgeId, Role.ADMIN);
		}
		User user = new User(username, name, token, Role.ADMIN, roles);
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
		Optional<MyEdge> edgeOpt = this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.findFirst();
		if (edgeOpt.isPresent()) {
			return Optional.ofNullable(edgeOpt.get().getId());
		}
		// not found. Is apikey a valid Edge-ID?
		Optional<Integer> idOpt = DummyMetadata.parseNumberFromName(apikey);
		int id;
		String edgeId;
		String setupPassword;
		if (idOpt.isPresent()) {
			edgeId = apikey;
			id = idOpt.get();
			setupPassword = edgeId;
		} else {
			// create new ID
			id = this.nextEdgeId.incrementAndGet();
			edgeId = "edge" + id;
			setupPassword = edgeId;
		}
		MyEdge edge = new MyEdge(edgeId, apikey, setupPassword, "OpenEMS Edge #" + id, State.ACTIVE, "", "", Level.OK,
				new EdgeConfig());
		edge.onSetConfig(config -> {
			this.logInfo(this.log, "Edge [" + edgeId + "]. Update config: "
					+ StringUtils.toShortString(EdgeConfigDiff.diff(config, edge.getConfig()).getAsHtml(), 100));
		});
		this.edges.put(edgeId, edge);
		return Optional.ofNullable(edgeId);

	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		Optional<MyEdge> optEdge = this.edges.values().stream()
				.filter(edge -> edge.getSetupPassword().equals(setupPassword)).findFirst();

		if (optEdge.isPresent()) {
			MyEdge edge = optEdge.get();
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
			Matcher matcher = NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				String nameNumberString = matcher.group(1);
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
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new IllegalArgumentException("DummyMetadata.submitSetupProtocol() is not implemented");
	}

}
