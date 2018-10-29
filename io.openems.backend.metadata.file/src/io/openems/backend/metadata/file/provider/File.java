package io.openems.backend.metadata.file.provider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

/**
 * This implementation of MetadataService reads Edges configuration from a
 * csv-file. The layout of the fil is as follows:
 * 
 * <pre>
 * 	name;comment;producttype;role;edgeId;apikey
 * </pre>
 * 
 * This implementation does not require any login. It always serves the same
 * user, which is has 'role'-permissions on all given Edges.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Metadata.File", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class File implements MetadataService {

	private final Logger log = LoggerFactory.getLogger(File.class);

	private String path = "";

	private User user = null;
	private Map<Integer, MyEdge> edges = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocketService edgeWebsocketService;

	@Activate
	void activate(Config config) {
		log.info("Activate MetadataFile [path=" + config.path() + "]");
		this.path = config.path();
		this.edges.clear();
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate MetadataFile");
	}

	private void refreshData() {
		if (this.edges.isEmpty()) {
			try {
				// read file
				FileReader fr = new FileReader(this.path);
				BufferedReader br = new BufferedReader(fr);
				String s;
				while ((s = br.readLine()) != null) {
					try {
						String[] parameters = s.split(";");
						String name = parameters[0];
						String comment = parameters[1];
						String producttype = parameters[2];
						Role role = Role.getRole(parameters[3]);
						int edgeId = Integer.parseInt(parameters[4]);
						String apikey = parameters[5];
						MyEdge edge = new MyEdge(edgeId, apikey, name, comment, State.ACTIVE,
								OpenemsConstants.VERSION, producttype, new JsonObject(), role);
						edge.onSetConfig(jConfig -> {
							log.debug(
									"Edge [" + edgeId + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
						});
						edge.onSetSoc(soc -> {
							log.debug("Edge [" + edgeId + "]. Set SoC: " + soc);
						});
						edge.onSetIpv4(ipv4 -> {
							log.debug("Edge [" + edgeId + "]. Set IPv4: " + ipv4);
						});
						edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
						this.edges.put(edgeId, edge);
					} catch (Throwable e) {
						log.error("Unable to parse line [" + s + "]. " + e.getClass().getSimpleName() + ": "
								+ e.getMessage());
					}
				}
				fr.close();
			} catch (IOException e) {
				log.error("Unable to read file [" + this.path + "]: " + e.getMessage());
			}
			// refresh user
			this.user = new User(0, "admin");
			for (int edgeId : this.edges.keySet()) {
				this.user.addEdgeRole(edgeId, Role.ADMIN);
			}
		}

	}

	@Override
	public User authenticate() throws OpenemsException {
		this.refreshData();
		return this.user;
	}

	@Override
	public User authenticate(String sessionId) throws OpenemsException {
		return this.authenticate(); // ignore sessionId
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		this.refreshData();
		List<Integer> ids = new ArrayList<>();
		for (MyEdge edge : this.edges.values()) {
			if (edge.getApikey().equals(apikey)) {
				ids.add(edge.getId());
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
		this.refreshData();
		return Optional.ofNullable(this.edges.get(edgeId));
	}

	@Override
	public Optional<User> getUser(int userId) {
		this.refreshData();
		if (this.user != null && userId == this.user.getId()) {
			return Optional.of(this.user);
		} else {
			return Optional.empty();
		}
	}

}
