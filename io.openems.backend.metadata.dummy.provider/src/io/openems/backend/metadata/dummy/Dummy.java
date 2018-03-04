package io.openems.backend.metadata.dummy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

@Component(name = "io.openems.backend.metadata.dummy.provider")
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
		log.debug("Activate MetadataDummy");
		this.nextUserId = 0;
		this.nextEdgeId = 0;
		this.users.clear();
		this.edges.clear();
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate MetadataDummy");
	}

	@Override
	public User getUserWithSession(String sessionId) throws OpenemsException {
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
		Optional<Edge> edgeOpt = this.getEdgeOpt(this.nextEdgeId);
		return new int[] { edgeOpt.get().getId() };
	}

	@Override
	public Optional<Edge> getEdgeOpt(int edgeId) {
		Edge edge = this.edges.get(edgeId);
		if (edge == null) {
			int id = this.nextEdgeId++;
			edge = new Edge(id, "EDGE:" + id, "comment [" + id + "]", "producttype [" + id + "]", new JsonObject());
			edge.onSetConfig(jConfig -> {
				log.debug("Edge [" + edgeId + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
			});
			edge.onSetSoc(soc -> {
				log.debug("Edge [" + edgeId + "]. Set SoC: " + soc);
			});
			edge.onSetIpv4(ipv4 -> {
				log.debug("Edge [" + edgeId + "]. Set IPv4: " + ipv4);
			});
			edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
			this.edges.put(id, edge);
		}
		return Optional.of(edge);
	}

	@Override
	public Optional<User> getUser(int userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

}
