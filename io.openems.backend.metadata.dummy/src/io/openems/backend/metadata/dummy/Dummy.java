package io.openems.backend.metadata.dummy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.User;
import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.Dummy", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Dummy implements MetadataService {

	private final Logger log = LoggerFactory.getLogger(Dummy.class);

	private int nextUserId = 0;
	private int nextEdgeId = 0;

	private Map<Integer, User> users = new HashMap<>();
	private Map<Integer, Edge> edges = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocketService edgeWebsocketService;

	@Activate
	void activate() {
		log.info("Activate Metadata.Dummy");
		this.nextUserId = 0;
		this.nextEdgeId = 0;
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
		int id = this.nextUserId++;
		User user = new User(id, "USER:" + sessionId);
		for (int edgeId : this.edges.keySet()) {
			user.addEdgeRole(edgeId, Role.ADMIN);
		}
		this.users.put(id, user);
		return user;
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		int[] edgeIds = this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.mapToInt(edge -> edge.getId()).toArray();
		if (edgeIds.length > 0) {
			return edgeIds;
		}
		// not found -> create
		int id = this.nextEdgeId++;
		Edge edge = new Edge(id, apikey, "EDGE:" + id, "comment [" + id + "]", State.ACTIVE,
				OpenemsConstants.OPENEMS_VERSION, "producttype [" + id + "]", new JsonObject(), null, null);
		edge.onSetConfig(jConfig -> {
			log.debug("Edge [" + id + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
		});
		edge.onSetSoc(soc -> {
			log.debug("Edge [" + id + "]. Set SoC: " + soc);
		});
		edge.onSetIpv4(ipv4 -> {
			log.debug("Edge [" + id + "]. Set IPv4: " + ipv4);
		});
		edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
		this.edges.put(id, edge);
		return new int[] { id };
	}

	@Override
	public Optional<Edge> getEdgeOpt(int edgeId) {
		Edge edge = this.edges.get(edgeId);
		return Optional.of(edge);
	}

	@Override
	public Optional<User> getUser(int userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

}
