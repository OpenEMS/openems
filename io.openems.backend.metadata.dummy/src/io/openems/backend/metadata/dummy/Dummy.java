package io.openems.backend.metadata.dummy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.Dummy", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Dummy implements Metadata {

	private final Logger log = LoggerFactory.getLogger(Dummy.class);

	private AtomicInteger nextUserId = new AtomicInteger(-1);
	private AtomicInteger nextEdgeId = new AtomicInteger(-1);

	private Map<String, User> users = new HashMap<>();
	private Map<String, Edge> edges = new HashMap<>();

	@Activate
	void activate() {
		log.info("Activate Metadata.Dummy");
		this.users.clear();
		this.edges.clear();
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate Metadata.Dummy");
	}

	@Override
	public User authenticate() throws OpenemsException {
		return this.authenticate("NO_SESSION_ID");
	}

	@Override
	public User authenticate(String sessionId) throws OpenemsException {
		int id = this.nextUserId.incrementAndGet();
		String userId = "user" + id;
		User user = new User(userId);
		for (String edgeId : this.edges.keySet()) {
			user.addEdgeRole(edgeId, Role.ADMIN);
		}
		this.users.put(userId, user);
		return user;
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		Optional<Edge> edgeOpt = this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.findFirst();
		if (edgeOpt.isPresent()) {
			return Optional.ofNullable(edgeOpt.get().getId());
		}
		// not found -> create
		int id = this.nextEdgeId.incrementAndGet();
		String edgeId = "edge" + id;
		Edge edge = new Edge(edgeId, apikey, "OpenEMS Edge #" + id, State.ACTIVE, "", "",
				new JsonObject(), null, null);
		edge.onSetConfig(jConfig -> {
			log.debug("Edge [" + edgeId + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
		});
		edge.onSetSoc(soc -> {
			log.debug("Edge [" + edgeId + "]. Set SoC: " + soc);
		});
		edge.onSetIpv4(ipv4 -> {
			log.debug("Edge [" + edgeId + "]. Set IPv4: " + ipv4);
		});
		this.edges.put(edgeId, edge);
		return Optional.ofNullable(edgeId);
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		Edge edge = this.edges.get(edgeId);
		return Optional.of(edge);
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}
}
