package io.openems.backend.metadata.energydepot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.User;
import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.backend.metadata.energydepot")
public class EnergyDepot implements MetadataService {

	private final Logger log = LoggerFactory.getLogger(EnergyDepot.class);

	private Map<Integer, MyUser> users = new HashMap<>();
	private Map<Integer, MyEdge> edges = new HashMap<>();
	private final ExecutorService readEdgeExecutor = Executors.newSingleThreadExecutor();
	private Future<?> readEdgeFuture = null;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private DBUtils dbu = null;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocketService edgeWebsocketService;

	@Activate
	void activate(Config config) {
		this.dbu = new DBUtils(config.user(),config.password(), config.dbname(), config.dburl(), config.wpurl());
		log.info("Activate EnergyDepot DB");
		this.edges.clear();
		
		
		this.readEdgeFuture = this.readEdgeExecutor.submit((Runnable) () -> {
			
				/*
				ResultSet result = this.dbu.getEdges();
				*/
				ResultSet result = this.dbu.getWPEdges();

				try {
					while (result.next()) {
						
						/*
						int id = result.getInt("Edges_id");
						String name = result.getString("name");
						String comment = result.getString("comment");
						String apikey = result.getString("apikey");
						String producttype = result.getString("producttype");
						*/
						
						int id = result.getInt("id");
						String name = result.getString("edge_name");
						String comment = result.getString("edge_comment");
						String apikey = result.getString("apikey");
						String producttype = result.getString("producttype");

						Role role = Role.getRole("ADMIN");
						MyEdge edge = new MyEdge(id, apikey, name, comment, State.ACTIVE, OpenemsConstants.OPENEMS_VERSION,
								producttype, new JsonObject(), role);

						edge.onSetConfig(jConfig -> {
							log.debug("Edge [" + id + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
						});
						edge.onSetSoc(soc -> {
							log.debug("Edge [" + id + "]. Set SoC: " + soc);
						});
						edge.onSetIpv4(ipv4 -> {
							log.debug("Edge [" + id + "]. Set IPv4: " + ipv4);
						});
						log.debug("Adding Edge from DB: " + name + ", " + comment + ", " + apikey);
						
						synchronized (this.edges) {
							this.edges.put(id, edge);
						}
						
						
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			
			this.isInitialized.set(true);
		});

	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate EnergyDepot DB");
		this.readEdgeFuture.cancel(true);
		this.readEdgeExecutor.shutdown();
		this.isInitialized.set(false);
	}

	@Override
	public User authenticate() throws OpenemsException {
		// TODO Auto-generated method stub
		MyUser user = this.dbu.getUserFromDB("Gastzugang", null);
		if (user == null) {
			throw new OpenemsException("User not found: Gastzugang");
		}

		for (int edgeId : user.getEdgeids()) {
			user.addEdgeRole(edgeId, Role.getRole(user.getRole()));
		}

		synchronized (this.users) {
			this.users.put(user.getId(), user);
		}

		return user;
	}

	@Override
	public User authenticate(String sessionId) throws OpenemsException {

		String[] cookiesplit = sessionId.split("%");
		String username = cookiesplit[0];

		try {

			boolean valid = false;

			JsonObject j = this.dbu.getWPResponse("/auth/validate_auth_cookie/?cookie=" + sessionId);

			valid = j.get("valid").getAsBoolean();

			if (valid) {
				MyUser user = this.dbu.getUserFromDB(username, sessionId);
				if (user == null) {

					throw new OpenemsException("User Not found!");

				}
				// admin, guest, installer, owner
				if (user.getRole().equals("admin")) {
					for (int edgeId : this.edges.keySet()) {
						user.addEdgeRole(edgeId, Role.ADMIN);
					}
				} else {
					for (int edgeId : user.getEdgeids())

						user.addEdgeRole(edgeId, Role.getRole(user.getRole()));
				}

				synchronized (this.users) {
					this.users.put(user.getId(), user);
				}

				return user;

			} else {
				throw new OpenemsException("User Login not valid!");
			}
		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}
		return null;

	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {

		/*
		 * this.user = new User(0, "admin"); for (int edgeId : this.edges.keySet()) {
		 * this.user.addEdgeRole(edgeId, Role.ADMIN); }
		 */
		List<Integer> ids = new ArrayList<>();

		/*
		 * Map<Integer, MyEdge> tmpedges = this.dbu.getEdges();
		 * if(!this.edges.equals(tmpedges)) { this.edges = tmpedges; }
		 */

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
		synchronized (this.edges) {
			if (this.edges.containsKey(edgeId)) {
				return Optional.of(this.edges.get(edgeId));
			}
		}

		return Optional.empty();
	}

	@Override
	public Optional<User> getUser(int userId) {
		// try to read from cache
		synchronized (this.users) {
			return Optional.ofNullable(this.users.get(userId));
		}
	}

}
