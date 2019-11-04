package io.openems.backend.metadata.wordpress;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.edgewebsocket.api.EdgeWebsocket;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.Metadata;
import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.backend.metadata.wordpress")
public class Wordpress extends AbstractOpenemsBackendComponent implements Metadata {

	public Wordpress() {
		super("Metadata.Wordpress");
		// TODO Auto-generated constructor stub
	}

	private final Logger log = LoggerFactory.getLogger(Wordpress.class);

	/**
	 * Maps User-ID to User
	 */
	private ConcurrentHashMap<String, BackendUser> users = new ConcurrentHashMap<>();
	private Map<String, MyEdge> edges = new HashMap<>();
	private final ExecutorService readEdgeExecutor = Executors.newSingleThreadExecutor();
	private Future<?> readEdgeFuture = null;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private DBUtils dbu = null;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocket edgeWebsocket;

	@Activate
	void activate(Config config) {
		this.dbu = new DBUtils(config.user(), config.password(), config.dbname(), config.dburl(), config.wpurl());
		log.info("Activate EnergyDepot DB");
		this.edges.clear();

		this.readEdgeFuture = this.readEdgeExecutor.submit((Runnable) () -> {

			updateEdges();

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
	public BackendUser authenticate() throws OpenemsException {
		MyUser user = this.dbu.getUserFromDB("Gastzugang", null);
		if (user == null) {
			throw new OpenemsException("User not found: Gastzugang");
		}

		for (String edgeId : user.getEdgeids()) {
			user.addEdgeRole(edgeId, Role.getRole(user.getRole()));
		}

		synchronized (this.users) {
			this.users.put(user.getId(), user);
		}

		return user;
	}

	@Override
	public BackendUser authenticate(String sessionId) throws OpenemsException {

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
					for (String edgeId : this.edges.keySet()) {
						user.addEdgeRole(edgeId, Role.ADMIN);
					}
				} else {
					for (String edgeId : user.getEdgeids())

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
	public Optional<Edge> getEdge(String edgeId) {
		synchronized (this.edges) {
			if (this.edges.containsKey(edgeId)) {
				return Optional.of(this.edges.get(edgeId));
			}
		}

		return Optional.empty();
	}

	@Override
	public Optional<BackendUser> getUser(String userId) {
		// try to read from cache
		synchronized (this.users) {
			return Optional.ofNullable(this.users.get(userId));
		}
	}

	private boolean updateEdges() {
		/*
		 * ResultSet result = this.dbu.getEdges();
		 */
		ResultSet result = this.dbu.getWPEdges();

		try {
			while (result.next()) {

				/*
				 * int id = result.getInt("Edges_id"); String name = result.getString("name");
				 * String comment = result.getString("comment"); String apikey =
				 * result.getString("apikey"); String producttype =
				 * result.getString("producttype");
				 */

				String id = result.getString("id");

				String name = result.getString("edge_name");
				String comment = result.getString("edge_comment");
				String apikey = result.getString("apikey");
				String producttype = result.getString("producttype");

				EdgeConfig config = new EdgeConfig();

				Role role = Role.getRole("ADMIN");
				MyEdge edge = new MyEdge(id, apikey, name, comment, State.ACTIVE, OpenemsConstants.VERSION.toString(), producttype,
						config, role, 0, "");

				this.addListeners(edge);

				synchronized (this.edges) {
					if (!this.edges.containsKey(id)) {
						this.edges.put(id, edge);
						log.info(
								"Adding Edge from Wordpress: " + name + ", " + comment + ", " + apikey + ", id: " + id);
					} else {
						MyEdge oldedge = this.edges.get(id);

						if (!oldedge.getApikey().equals(edge.getApikey())
								|| !oldedge.getComment().equals(edge.getComment())
								|| !oldedge.getProducttype().equals(edge.getProducttype())) {
							this.edges.remove(id);
							this.edges.put(id, edge);
							log.info("Updating Edge from Wordpress: " + name + ", " + comment + ", " + apikey + ", id: "
									+ id);
						}

					}

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		updateEdges();

		/*
		 * this.user = new User(0, "admin"); for (int edgeId : this.edges.keySet()) {
		 * this.user.addEdgeRole(edgeId, Role.ADMIN); }
		 */
		String id = null;

		/*
		 * Map<Integer, MyEdge> tmpedges = this.dbu.getEdges();
		 * if(!this.edges.equals(tmpedges)) { this.edges = tmpedges; }
		 */

		for (MyEdge edge : this.edges.values()) {
			if (edge.getApikey().equals(apikey)) {
				id = edge.getId();
				// edge.setOnline(true);
				break;
			}
		}
		// edge.setOnline(this.edgeWebsocket.isOnline(edge.getId()));

		return Optional.ofNullable(id);
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}

	private void addListeners(MyEdge edge) {
		edge.onSetConfig(config -> {
			log.debug("Edge [" + edge.getId() + "]. Update config: " + StringUtils.toShortString(config.toJson(), 100));
		});
		edge.onSetSoc(soc -> {
			log.debug("Edge [" + edge.getId() + "]. Set SoC: " + soc);
		});
		edge.onSetIpv4(ipv4 -> {
			log.debug("Edge [" + edge.getId() + "]. Set IPv4: " + ipv4);
		});
	}

	@Override
	public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
		
		try {

			String valid;
			
			

			JsonObject j = this.dbu.getWPResponse("/auth/generate_auth_cookie/?username=" + username + "&password=" + password);

			valid = j.get("status").getAsString();

			if (valid.equals("ok")) {
				
				String cookie = j.get("cookie").getAsString();
				
				log.debug("Cookie: " + cookie);
				
				return authenticate(cookie);
				

			} else {
				throw new OpenemsException("User Login not valid!");
			}
		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}
		
		return null;
	}
	
	@Override
	public
	Optional<String> addEdgeToDB(String apikey, String mac, String version){
		if (this.dbu.addEdge(apikey, mac, version)) {
			this.logInfo(log, "Added new hy-control to Wordpress: Apikey " + apikey + ", MAC " + mac );
			this.updateEdges();
			return this.getEdgeIdForApikey(apikey);
		}
		return null;
	}

}
