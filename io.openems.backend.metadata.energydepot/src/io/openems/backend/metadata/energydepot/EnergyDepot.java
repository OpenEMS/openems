package io.openems.backend.metadata.energydepot;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.User;
import io.openems.backend.metadata.energydepot.MyEdge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;

import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate( ocd=Config.class, factory=true)
@Component(name="io.openems.backend.metadata.energydepot")
public class EnergyDepot implements MetadataService{

	private final Logger log = LoggerFactory.getLogger(EnergyDepot.class);

	

	private Map<Integer, User> users = new HashMap<>();
	private Map<Integer, MyEdge> edges = new HashMap<>();
	
	private DBUtils dbu = null;
	

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocketService edgeWebsocketService;



	private User user;

	

	@Activate
	void activate(Config config) {
		this.dbu = new DBUtils(config.password());
		log.info("Activate EnergyDepot DB");
		this.edges.clear();
		this.edges = this.dbu.getEdges();
		log.info(this.edges.toString());
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate EnergyDepot DB");
	}

	@Override
	public User authenticate() throws OpenemsException {
		// TODO Auto-generated method stub
		return this.user;
	}

	@Override
	public User authenticate(String sessionId) throws OpenemsException {
		// TODO verify userdata from wordpress
		// add Edge Role
		String[] cookie = sessionId.split("%");
		String username = cookie[0];
		String expire = cookie[1];
		String hash = cookie[2];
		
		return this.authenticate();
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		
		this.user = new User(0, "admin");
		for (int edgeId : this.edges.keySet()) {
			this.user.addEdgeRole(edgeId, Role.ADMIN);
		}
		
		List<Integer> ids = new ArrayList<>();
		for (MyEdge edge : this.edges.values()) {
			if (edge.getApikey().equals(apikey)) {
				ids.add(edge.getId());
				edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
			}
		}
		int[] result = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			result[i] = ids.get(i);
		}
		return result;
		
		
	}

	@Override
	public Optional<Edge> getEdgeOpt(int edgeId) {
		// TODO Auto-generated method stub
		return Optional.ofNullable(this.edges.get(edgeId));
	}

	@Override
	public Optional<User> getUser(int userId) {
		// TODO Auto-generated method stub
		if (this.user != null && userId == this.user.getId()) {
			return Optional.of(this.user);
		} else {
			return Optional.empty();
		}
	}

}
