package io.openems.backend.metadata.dummy;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.MetadataUtils;
import io.openems.backend.common.metadata.SimpleEdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.ThreadPoolUtils;

/**
 * neutrava persistent Metadata provider (based on Metadata.Dummy).
 *
 * <p>
 * Edges are read from 'metadata.json', Users (incl. roles, per-user settings,
 * information and alerting) from 'users.json'. Only explicitly registered Edges
 * are served - no automatic Edge creation. Both files are watched for external
 * changes (mtime) and are rewritten atomically on updates.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Dummy", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		service = { Metadata.class, EventHandler.class, MetadataDummy.class }, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class MetadataDummy extends AbstractMetadata implements Metadata, EventHandler {

	private static final String[] METADATA_FILE_PATHS = new String[] {
			"/var/opt/openems/metadata.json",
			"/opt/openems/backend/metadata.json",
			"metadata.json"
	};
	private static final String[] USERS_FILE_PATHS = new String[] {
			"/var/opt/openems/users.json",
			"/opt/openems/backend/users.json",
			"users.json"
	};
	private static final String[] MAILER_FILE_PATHS = new String[] {
			"/var/opt/openems/mailer.json",
			"/opt/openems/backend/mailer.json",
			"mailer.json"
	};
	private static final String NEUTRAVA_MAILER_SERVICE = "ch.neutrava.openems.mailer.api.NeutravaMailer";

	private final Logger log = LoggerFactory.getLogger(MetadataDummy.class);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final EventAdmin eventAdmin;

	private final Map<String, User> users = new HashMap<>();
	private final Map<String, MyEdge> edges = new HashMap<>();
	private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();
	private final Map<String, ZonedDateTime> lastOfflineNotification = new ConcurrentHashMap<>();

	private Language defaultLanguage = Language.DE;
	private long lastMetadataFileModified = -1L;
	private long lastUsersFileModified = -1L;

	@Activate
	public MetadataDummy(@Reference EventAdmin eventadmin, Config config) {
		super("Metadata.Dummy");
		this.eventAdmin = eventadmin;
		this.logInfo(this.log, "Activate");

		this.refreshEdges();
		this.refreshUsers();

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

	// ------------------------------------------------------------------
	// File handling
	// ------------------------------------------------------------------

	private static File findFile(String[] paths) {
		for (String path : paths) {
			var file = new File(path);
			if (file.exists() && file.canRead()) {
				return file;
			}
		}
		return null;
	}

	private static File writableFile(String[] paths) {
		var file = findFile(paths);
		if (file != null) {
			return file;
		}
		// create in the first (preferred) location
		return new File(paths[0]);
	}

	private static JsonObject readJsonFile(File file) {
		try (var reader = new FileReader(file, StandardCharsets.UTF_8)) {
			var json = JsonParser.parseReader(reader);
			return json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	private static void writeJsonFile(File file, JsonObject json) {
		try {
			var parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			var tmp = new File(file.getAbsolutePath() + ".tmp");
			try (var writer = new FileWriter(tmp, StandardCharsets.UTF_8)) {
				writer.write(json.toString());
			}
			if (!tmp.renameTo(file)) {
				try (var writer = new FileWriter(file, StandardCharsets.UTF_8)) {
					writer.write(json.toString());
				}
				tmp.delete();
			}
		} catch (Exception e) {
			// log via System.err; logger not always available in static context
			System.err.println("[Metadata.Neutrava] Failed to write " + file + ": " + e.getMessage());
		}
	}

	private synchronized JsonObject readMetadataJson() {
		return readJsonFile(writableFile(METADATA_FILE_PATHS));
	}

	private synchronized JsonObject readUsersJson() {
		return readJsonFile(writableFile(USERS_FILE_PATHS));
	}

	private synchronized void writeMetadataJson(JsonObject json) {
		var file = writableFile(METADATA_FILE_PATHS);
		writeJsonFile(file, json);
		this.lastMetadataFileModified = -1L;
	}

	private synchronized void writeUsersJson(JsonObject json) {
		var file = writableFile(USERS_FILE_PATHS);
		writeJsonFile(file, json);
		this.lastUsersFileModified = -1L;
	}

	// ------------------------------------------------------------------
	// Edge registry (metadata.json)
	// ------------------------------------------------------------------

	private synchronized void refreshEdges() {
		var file = writableFile(METADATA_FILE_PATHS);
		var modified = file.exists() ? file.lastModified() : -1L;
		if (modified == this.lastMetadataFileModified) {
			return;
		}
		this.lastMetadataFileModified = modified;

		var config = readJsonFile(file);
		var jEdges = config.has("edges") && config.get("edges").isJsonObject() //
				? config.getAsJsonObject("edges") //
				: new JsonObject();

		for (var entry : jEdges.entrySet()) {
			var edgeId = entry.getKey();
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			var j = entry.getValue().getAsJsonObject();
			var apikey = optString(j, "apikey").orElse(edgeId);
			var setupPassword = optString(j, "setuppassword").orElse("");
			var comment = optString(j, "comment").orElse(edgeId);
			var settings = j.has("settings") && j.get("settings").isJsonObject() //
					? j.getAsJsonObject("settings") //
					: null;
			var locationId = optString(j, "locationId").orElse(null);
			var groupIds = optStringList(j, "groupIds");

			var existing = this.edges.get(edgeId);
			if (existing != null) {
				existing.update(apikey, setupPassword, comment, settings, locationId, groupIds);
			} else {
				this.edges.put(edgeId,
						new MyEdge(this, edgeId, apikey, setupPassword, comment, "", "", settings, locationId, groupIds));
			}
		}
		// remove edges that were deleted from the file
		this.edges.keySet().removeIf(id -> !jEdges.has(id));
	}

	private static List<String> optStringList(JsonObject j, String key) {
		var list = new ArrayList<String>();
		if (j.has(key) && j.get(key).isJsonArray()) {
			for (var el : j.getAsJsonArray(key)) {
				if (el.isJsonPrimitive()) {
					list.add(el.getAsString());
				}
			}
		}
		return list;
	}

	private static Optional<String> optString(JsonObject j, String key) {
		if (j.has(key) && j.get(key).isJsonPrimitive() && !j.get(key).isJsonNull()) {
			try {
				return Optional.of(j.get(key).getAsString());
			} catch (Exception e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	@Override
	public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
		this.refreshEdges();
		return this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.map(MyEdge::getId) //
				.findFirst();
	}

	@Override
	public synchronized Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		this.refreshEdges();
		return this.edges.values().stream() //
				.filter(edge -> edge.getSetupPassword().equals(setupPassword)) //
				.map(edge -> (Edge) edge) //
				.findFirst();
	}

	@Override
	public synchronized Optional<Edge> getEdge(String edgeId) {
		this.refreshEdges();
		return Optional.ofNullable(this.edges.get(edgeId));
	}

	@Override
	public synchronized Collection<Edge> getAllOfflineEdges() {
		this.refreshEdges();
		return this.edges.values().stream().filter(Edge::isOffline).map(e -> (Edge) e).toList();
	}

	// ------------------------------------------------------------------
	// Users (users.json)
	// ------------------------------------------------------------------

	private synchronized void refreshUsers() {
		var file = writableFile(USERS_FILE_PATHS);
		this.lastUsersFileModified = file.exists() ? file.lastModified() : -1L;
	}

	private JsonObject readUserFromUsersJson(String username) {
		if (username == null) {
			return null;
		}
		var json = this.readUsersJson();
		if (json.has(username) && json.get(username).isJsonObject()) {
			return json.getAsJsonObject(username);
		}
		return null;
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public CompletableFuture<User> getUserByExternalId(String login) {
		var cached = this.users.get(login);
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}
		final var user = this.createUser(login);
		this.users.put(login, user);
		return CompletableFuture.completedFuture(user);
	}

	private User createUser(String login) {
		Role globalRole = Role.GUEST;
		var multipleEdges = false;
		var settings = new JsonObject();
		var userObj = this.readUserFromUsersJson(login);
		if (userObj != null) {
			if (userObj.has("globalRole") && !userObj.get("globalRole").isJsonNull()) {
				try {
					globalRole = Role.valueOf(userObj.get("globalRole").getAsString().toUpperCase());
				} catch (Exception e) {
					globalRole = Role.GUEST;
				}
			} else if (userObj.has("globalRole")) {
				globalRole = Role.GUEST;
			}
			if (userObj.has("edges") && userObj.get("edges").isJsonObject()) {
				multipleEdges = userObj.getAsJsonObject("edges").size() > 1;
			}
			if (userObj.has("settings") && userObj.get("settings").isJsonObject()) {
				settings = userObj.getAsJsonObject("settings");
			}
		}
		return new User(login, login, login, this.defaultLanguage, globalRole, multipleEdges, settings);
	}

	@Override
	public Role getUserRole(User user, String edgeId) {
		if (user.getGlobalRole() == Role.ADMIN) {
			return Role.ADMIN;
		}
		var userObj = this.readUserFromUsersJson(user.getName());
		if (userObj != null && userObj.has("edges") && userObj.get("edges").isJsonObject()) {
			var edgesObj = userObj.getAsJsonObject("edges");
			if (edgesObj.has(edgeId)) {
				try {
					return Role.valueOf(edgesObj.get(edgeId).getAsString().toUpperCase());
				} catch (Exception e) {
					return Role.OWNER;
				}
			}
		}
		return Role.GUEST;
	}

	@Override
	public CompletableFuture<List<EdgeMetadata>> getPageDevice(User user, PaginationOptions paginationOptions) {
		this.refreshEdges();
		Collection<MyEdge> allowedEdges = this.edges.values();
		if (user.getGlobalRole() != Role.ADMIN) {
			var userObj = this.readUserFromUsersJson(user.getName());
			if (userObj != null && userObj.has("edges") && userObj.get("edges").isJsonObject()) {
				var edgesObj = userObj.getAsJsonObject("edges");
				allowedEdges = this.edges.values().stream() //
						.filter(e -> edgesObj.has(e.getId())) //
						.toList();
			} else {
				allowedEdges = Collections.emptyList();
			}
		}
		var list = allowedEdges.stream()
				.map(edge -> new EdgeMetadata(//
						edge.getId(), //
						edge.getComment(), //
						edge.getProducttype(), //
						edge.getVersion(), //
						this.getUserRole(user, edge.getId()), //
						edge.isOnline(), //
						edge.getLastmessage(), //
						null, // firstSetupProtocol
						Optional.ofNullable(edge.getSumState()).orElse(Level.OK), //
						edge.getSettings())) //
				.toList();
		return CompletableFuture.completedFuture(list);
	}

	@Override
	public CompletableFuture<EdgeMetadata> getEdgeMetadataForUser(User user, String edgeId) {
		this.refreshEdges();
		final var edge = this.edges.get(edgeId);
		if (edge == null) {
			return CompletableFuture.failedFuture(new OpenemsException("Unable to find edge with id [" + edgeId + "]"));
		}
		final Role role = this.getUserRole(user, edgeId);
		return CompletableFuture.completedFuture(new EdgeMetadata(//
				edge.getId(), //
				edge.getComment(), //
				edge.getProducttype(), //
				edge.getVersion(), //
				role, //
				edge.isOnline(), //
				edge.getLastmessage(), //
				null, // firstSetupProtocol
				edge.isOnline() ? Level.OK : Level.WARNING, //
				edge.getSettings() //
		));
	}

	// ------------------------------------------------------------------
	// Persisted user information & settings
	// ------------------------------------------------------------------

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		var userObj = this.readUserFromUsersJson(user.getName());
		if (userObj != null && userObj.has("information") && userObj.get("information").isJsonObject()) {
			var map = new HashMap<String, Object>();
			for (var e : userObj.getAsJsonObject("information").entrySet()) {
				map.put(e.getKey(), e.getValue());
			}
			return map;
		}
		return new HashMap<>();
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		var json = this.readUsersJson();
		var userObj = json.has(user.getName()) && json.get(user.getName()).isJsonObject() //
				? json.getAsJsonObject(user.getName()) //
				: new JsonObject();
		userObj.add("information", jsonObject == null ? new JsonObject() : jsonObject);
		json.add(user.getName(), userObj);
		this.writeUsersJson(json);
	}

	@Override
	public void updateUserSettings(User user, JsonObject settings) {
		var json = this.readUsersJson();
		var userObj = json.has(user.getName()) && json.get(user.getName()).isJsonObject() //
				? json.getAsJsonObject(user.getName()) //
				: new JsonObject();
		userObj.add("settings", settings == null ? new JsonObject() : settings);
		json.add(user.getName(), userObj);
		this.writeUsersJson(json);
		// update cached user instance
		var cached = this.users.get(user.getId());
		if (cached != null) {
			this.users.put(user.getId(), new User(cached.getId(), cached.getName(), cached.getToken(),
					cached.getLanguage(), cached.getGlobalRole(), cached.hasMultipleEdges(),
					settings == null ? new JsonObject() : settings));
		}
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
		var json = this.readUsersJson();
		var userObj = json.has(user.getName()) && json.get(user.getName()).isJsonObject() //
				? json.getAsJsonObject(user.getName()) //
				: new JsonObject();
		userObj.addProperty("language", language.name());
		json.add(user.getName(), userObj);
		this.writeUsersJson(json);
	}

	@Override
	public CompletableFuture<Void> updateEdgeSettings(String edgeId, JsonObject settings) {
		var edge = this.edges.get(edgeId);
		if (edge != null) {
			edge.setSettings(settings);
		}
		var json = this.readMetadataJson();
		var edgesObj = json.has("edges") && json.get("edges").isJsonObject() //
				? json.getAsJsonObject("edges") //
				: new JsonObject();
		var edgeObj = edgesObj.has(edgeId) && edgesObj.get(edgeId).isJsonObject() //
				? edgesObj.getAsJsonObject(edgeId) //
				: new JsonObject();
		edgeObj.add("settings", settings == null ? new JsonObject() : settings);
		edgesObj.add(edgeId, edgeObj);
		json.add("edges", edgesObj);
		this.writeMetadataJson(json);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
		var json = this.readUsersJson();
		var username = optString(jsonObject, "username")
				.orElseGet(() -> optString(jsonObject, "email").orElse(null));
		if (username != null && !json.has(username)) {
			var userObj = new JsonObject();
			userObj.addProperty("globalRole", "GUEST");
			userObj.add("edges", new JsonObject());
			json.add(username, userObj);
			this.writeUsersJson(json);
		}
	}

	// ------------------------------------------------------------------
	// Alerting
	// ------------------------------------------------------------------

	private static int optInt(JsonObject j, String key, int def) {
		try {
			return j.has(key) && !j.get(key).isJsonNull() ? j.get(key).getAsInt() : def;
		} catch (Exception e) {
			return def;
		}
	}

	private List<String> alertRecipientsFor(JsonObject userObj, String edgeId) {
		// recipients: users with 'alerting.email' assigned to this edge, plus
		// global alert recipients from metadata.json (internal neutrava alerts)
		var recipients = new ArrayList<String>();
		var metadata = this.readMetadataJson();
		if (metadata.has("alerting") && metadata.get("alerting").isJsonObject()) {
			var alerting = metadata.getAsJsonObject("alerting");
			if (alerting.has("recipients") && alerting.get("recipients").isJsonArray()) {
				for (var r : alerting.getAsJsonArray("recipients")) {
					recipients.add(r.getAsString());
				}
			}
		}
		return recipients;
	}

	private int globalOfflineDelay() {
		var metadata = this.readMetadataJson();
		if (metadata.has("alerting") && metadata.get("alerting").isJsonObject()) {
			return optInt(metadata.getAsJsonObject("alerting"), "offlineDelayMinutes", 30);
		}
		return 30;
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		var userObj = this.readUserFromUsersJson(userId);
		if (userObj != null && userObj.has("alerting") && userObj.get("alerting").isJsonObject()) {
			var a = userObj.getAsJsonObject("alerting");
			return new UserAlertingSettings(edgeId, userId, //
					optInt(a, "offlineEdgeDelay", 60), //
					optInt(a, "faultStateDelay", 30), //
					optInt(a, "warningStateDelay", 1440), null, null);
		}
		return new UserAlertingSettings(edgeId, userId, 0, 0, 0, null, null);
	}

	@Override
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
		var result = new ArrayList<UserAlertingSettings>();
		var json = this.readUsersJson();
		for (var entry : json.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			var userObj = entry.getValue().getAsJsonObject();
			var isAdmin = userObj.has("globalRole") && !userObj.get("globalRole").isJsonNull()
					&& "ADMIN".equalsIgnoreCase(userObj.get("globalRole").getAsString());
			var hasEdge = userObj.has("edges") && userObj.get("edges").isJsonObject()
					&& userObj.getAsJsonObject("edges").has(edgeId);
			if (!isAdmin && !hasEdge) {
				continue;
			}
			if (userObj.has("alerting") && userObj.get("alerting").isJsonObject()) {
				var a = userObj.getAsJsonObject("alerting");
				result.add(new UserAlertingSettings(edgeId, entry.getKey(), //
						optInt(a, "offlineEdgeDelay", 60), //
						optInt(a, "faultStateDelay", 30), //
						optInt(a, "warningStateDelay", 1440), null, null));
			}
		}
		return result;
	}

	@Override
	public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
		var result = new ArrayList<OfflineEdgeAlertingSetting>();
		var delay = this.globalOfflineDelay();
		var last = this.lastOfflineNotification.get(edgeId);
		for (var recipient : this.alertRecipientsFor(null, edgeId)) {
			result.add(new OfflineEdgeAlertingSetting(edgeId, recipient, delay, last));
		}
		return result;
	}

	@Override
	public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
		var result = new ArrayList<SumStateAlertingSetting>();
		var metadata = this.readMetadataJson();
		var faultDelay = 15;
		var warningDelay = 30;
		if (metadata.has("alerting") && metadata.get("alerting").isJsonObject()) {
			var a = metadata.getAsJsonObject("alerting");
			faultDelay = optInt(a, "faultDelayMinutes", 15);
			warningDelay = optInt(a, "warningDelayMinutes", 30);
		}
		for (var recipient : this.alertRecipientsFor(null, edgeId)) {
			result.add(new SumStateAlertingSetting(edgeId, recipient, faultDelay, warningDelay, null));
		}
		return result;
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> settings) {
		var json = this.readUsersJson();
		var userObj = json.has(user.getName()) && json.get(user.getName()).isJsonObject() //
				? json.getAsJsonObject(user.getName()) //
				: new JsonObject();
		if (settings != null && !settings.isEmpty()) {
			var s = settings.get(0);
			var a = new JsonObject();
			a.addProperty("offlineEdgeDelay", s.edgeOfflineDelay());
			a.addProperty("faultStateDelay", s.edgeFaultDelay());
			a.addProperty("warningStateDelay", s.edgeWarningDelay());
			userObj.add("alerting", a);
		} else {
			userObj.remove("alerting");
		}
		json.add(user.getName(), userObj);
		this.writeUsersJson(json);
	}

	@Override
	public Optional<Level> getSumState(String edgeId) {
		var edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge != null ? edge.getSumState() : null);
	}

	// ------------------------------------------------------------------
	// Admin JSON-RPC (invoked reflectively from Core.JsonRpcRequestHandler)
	// ------------------------------------------------------------------

	private static final String ADMIN_PREFIX = "neutravaAdmin";

	/**
	 * Handles neutrava admin JSON-RPC requests. Requires global ADMIN role.
	 * Methods: neutravaAdminGetUsers, neutravaAdminSetUser,
	 * neutravaAdminDeleteUser, neutravaAdminGetEdges, neutravaAdminSetEdge,
	 * neutravaAdminDeleteEdge.
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleAdminRpc(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		var method = request.getMethod();
		if (method == null || !method.startsWith(ADMIN_PREFIX)) {
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(method);
		}
		if (user.getGlobalRole() != Role.ADMIN) {
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception("neutravaAdmin", user.getGlobalRole());
		}
		var params = request.getParams() != null ? request.getParams() : new JsonObject();
		var result = switch (method) {
		case "neutravaAdminGetFleet" -> this.adminGetFleet();
		case "neutravaAdminGetUsers" -> this.adminGetUsers();
		case "neutravaAdminSetUser" -> this.adminSetUser(params);
		case "neutravaAdminDeleteUser" -> this.adminDeleteUser(params);
		case "neutravaAdminGetEdges" -> this.adminGetEdges();
		case "neutravaAdminSetEdge" -> this.adminSetEdge(params);
		case "neutravaAdminDeleteEdge" -> this.adminDeleteEdge(params);
		case "neutravaAdminSetLocation" -> this.adminSetLocation(params);
		case "neutravaAdminDeleteLocation" -> this.adminDeleteLocation(params);
		case "neutravaAdminSetGroup" -> this.adminSetGroup(params);
		case "neutravaAdminDeleteGroup" -> this.adminDeleteGroup(params);
		case "neutravaAdminGetMailConfig" -> this.adminGetMailConfig();
		case "neutravaAdminSetMailConfig" -> this.adminSetMailConfig(params);
		case "neutravaAdminSendTestMail" -> this.adminSendTestMail(params);
		case "neutravaAdminSendInvitation" -> this.adminSendInvitation(params);
		default -> throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(method);
		};
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), result));
	}

	private JsonObject adminGetFleet() {
		this.refreshEdges();
		var metadata = this.readMetadataJson();
		var usersJson = this.readUsersJson();

		Map<String, List<String>> edgeAssignedUsers = new HashMap<>();
		for (var entry : usersJson.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			var uObj = entry.getValue().getAsJsonObject();
			if (uObj.has("edges") && uObj.get("edges").isJsonObject()) {
				for (var edgeEntry : uObj.getAsJsonObject("edges").entrySet()) {
					edgeAssignedUsers.computeIfAbsent(edgeEntry.getKey(), k -> new ArrayList<>())
							.add(entry.getKey() + " (" + edgeEntry.getValue().getAsString() + ")");
				}
			}
		}

		var edgesArr = new JsonArray();
		int onlineCount = 0;
		for (var edge : this.edges.values()) {
			var out = new JsonObject();
			out.addProperty("id", edge.getId());
			out.addProperty("apikey", edge.getApikey());
			out.addProperty("setuppassword", edge.getSetupPassword());
			out.addProperty("comment", edge.getComment());
			boolean online = edge.isOnline();
			if (online) {
				onlineCount++;
			}
			out.addProperty("isOnline", online);
			out.addProperty("lastmessage", edge.getLastmessage() != null ? edge.getLastmessage().toString() : null);
			out.addProperty("sumState", edge.getSumState() != null ? edge.getSumState().name() : "OK");
			out.addProperty("locationId", edge.getLocationId());
			var groupsArr = new JsonArray();
			for (var g : edge.getGroupIds()) {
				groupsArr.add(g);
			}
			out.add("groupIds", groupsArr);
			var assignedArr = new JsonArray();
			for (var u : edgeAssignedUsers.getOrDefault(edge.getId(), Collections.emptyList())) {
				assignedArr.add(u);
			}
			out.add("assignedUsers", assignedArr);
			edgesArr.add(out);
		}

		var locsArr = new JsonArray();
		if (metadata.has("locations") && metadata.get("locations").isJsonObject()) {
			for (var entry : metadata.getAsJsonObject("locations").entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				var l = entry.getValue().getAsJsonObject();
				var out = new JsonObject();
				out.addProperty("id", entry.getKey());
				out.addProperty("name", optString(l, "name").orElse(entry.getKey()));
				out.addProperty("address", optString(l, "address").orElse(""));
				out.addProperty("contact", optString(l, "contact").orElse(""));
				out.addProperty("notes", optString(l, "notes").orElse(""));
				long edgeCount = this.edges.values().stream()
						.filter(e -> entry.getKey().equals(e.getLocationId())).count();
				out.addProperty("edgeCount", edgeCount);
				locsArr.add(out);
			}
		}

		var groupsArr = new JsonArray();
		if (metadata.has("groups") && metadata.get("groups").isJsonObject()) {
			for (var entry : metadata.getAsJsonObject("groups").entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				var g = entry.getValue().getAsJsonObject();
				var out = new JsonObject();
				out.addProperty("id", entry.getKey());
				out.addProperty("name", optString(g, "name").orElse(entry.getKey()));
				out.addProperty("type", optString(g, "type").orElse("ZEV"));
				out.addProperty("gridPointEdgeId", optString(g, "gridPointEdgeId").orElse(""));
				out.addProperty("notes", optString(g, "notes").orElse(""));
				long edgeCount = this.edges.values().stream()
						.filter(e -> e.getGroupIds().contains(entry.getKey())).count();
				out.addProperty("edgeCount", edgeCount);
				groupsArr.add(out);
			}
		}

		var usersArr = this.adminGetUsers().getAsJsonArray("users");

		var stats = new JsonObject();
		stats.addProperty("totalEdges", this.edges.size());
		stats.addProperty("onlineEdges", onlineCount);
		stats.addProperty("totalUsers", usersArr.size());
		stats.addProperty("totalLocations", locsArr.size());
		stats.addProperty("totalGroups", groupsArr.size());

		var result = new JsonObject();
		result.add("edges", edgesArr);
		result.add("locations", locsArr);
		result.add("groups", groupsArr);
		result.add("users", usersArr);
		result.add("stats", stats);
		return result;
	}

	private JsonObject adminGetUsers() {
		var json = this.readUsersJson();
		var arr = new com.google.gson.JsonArray();
		for (var entry : json.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			var u = entry.getValue().getAsJsonObject();
			var out = new JsonObject();
			out.addProperty("username", entry.getKey());
			out.addProperty("globalRole",
					u.has("globalRole") && !u.get("globalRole").isJsonNull()
							? u.get("globalRole").getAsString()
							: null);
			out.add("edges", u.has("edges") ? u.get("edges") : new JsonObject());
			out.addProperty("email", optString(u, "email").orElse(null));
			out.addProperty("hasPassword", u.has("passwordHash") || u.has("password"));
			arr.add(out);
		}
		var result = new JsonObject();
		result.add("users", arr);
		return result;
	}

	private JsonObject adminSetUser(JsonObject params) {
		var username = optString(params, "username")
				.orElseThrow(() -> new IllegalArgumentException("username required"));
		var json = this.readUsersJson();
		var isNew = !json.has(username);
		var userObj = json.has(username) && json.get(username).isJsonObject() //
				? json.getAsJsonObject(username) //
				: new JsonObject();
		var emailSet = false;
		if (params.has("email")) {
			if (params.get("email").isJsonNull() || params.get("email").getAsString().isEmpty()) {
				userObj.remove("email");
			} else {
				userObj.addProperty("email", params.get("email").getAsString());
				emailSet = true;
			}
		}
		// If email not set explicitly, but username is an email address, default email to username
		if (!userObj.has("email") && username.contains("@")) {
			userObj.addProperty("email", username);
		}
		optString(params, "password").ifPresent(pw -> {
			userObj.addProperty("passwordHash", hashPassword(pw));
			userObj.remove("password");
		});
		if (params.has("globalRole")) {
			if (params.get("globalRole").isJsonNull()) {
				userObj.add("globalRole", null);
			} else {
				userObj.addProperty("globalRole", params.get("globalRole").getAsString());
			}
		}
		if (params.has("edges") && params.get("edges").isJsonObject()) {
			userObj.add("edges", params.getAsJsonObject("edges"));
		}
		json.add(username, userObj);
		this.writeUsersJson(json);
		this.users.remove(username); // invalidate cached user

		// Send welcome mail if requested (new user default true, or explicit sendWelcomeMail: true)
		var email = optString(userObj, "email").orElse(null);
		var wantWelcomeMail = params.has("sendWelcomeMail")
				? params.get("sendWelcomeMail").getAsBoolean()
				: isNew;
		if (wantWelcomeMail && email != null) {
			this.sendWelcomeMailAsync(username, email);
		}
		return new JsonObject();
	}

	private JsonObject adminSendInvitation(JsonObject params) {
		var username = optString(params, "username")
				.orElseThrow(() -> new IllegalArgumentException("username required"));
		var json = this.readUsersJson();
		if (!json.has(username) || !json.get(username).isJsonObject()) {
			throw new IllegalArgumentException("user not found: " + username);
		}
		var userObj = json.getAsJsonObject(username);
		var email = optString(userObj, "email").orElse(null);
		if ((email == null || email.isBlank()) && username.contains("@")) {
			email = username;
			userObj.addProperty("email", email);
			json.add(username, userObj);
			this.writeUsersJson(json);
		}
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Benutzer hat keine E-Mail-Adresse hinterlegt.");
		}
		this.sendWelcomeMailAsync(username, email);
		var out = new JsonObject();
		out.addProperty("sent", true);
		return out;
	}

	private JsonObject adminDeleteUser(JsonObject params) {
		var username = optString(params, "username")
				.orElseThrow(() -> new IllegalArgumentException("username required"));
		var json = this.readUsersJson();
		json.remove(username);
		this.writeUsersJson(json);
		this.users.remove(username);
		return new JsonObject();
	}

	private JsonObject adminGetEdges() {
		this.refreshEdges();
		var arr = new com.google.gson.JsonArray();
		for (var edge : this.edges.values()) {
			var out = new JsonObject();
			out.addProperty("id", edge.getId());
			out.addProperty("apikey", edge.getApikey());
			out.addProperty("setuppassword", edge.getSetupPassword());
			out.addProperty("comment", edge.getComment());
			out.addProperty("isOnline", edge.isOnline());
			out.addProperty("locationId", edge.getLocationId());
			var groupsArr = new JsonArray();
			for (var g : edge.getGroupIds()) {
				groupsArr.add(g);
			}
			out.add("groupIds", groupsArr);
			arr.add(out);
		}
		var result = new JsonObject();
		result.add("edges", arr);
		return result;
	}

	private JsonObject adminSetEdge(JsonObject params) {
		var edgeId = optString(params, "edgeId")
				.orElseThrow(() -> new IllegalArgumentException("edgeId required"));
		var json = this.readMetadataJson();
		var edgesObj = json.has("edges") && json.get("edges").isJsonObject() //
				? json.getAsJsonObject("edges") //
				: new JsonObject();
		var edgeObj = edgesObj.has(edgeId) && edgesObj.get(edgeId).isJsonObject() //
				? edgesObj.getAsJsonObject(edgeId) //
				: new JsonObject();
		optString(params, "apikey").ifPresent(v -> edgeObj.addProperty("apikey", v));
		optString(params, "setuppassword").ifPresent(v -> edgeObj.addProperty("setuppassword", v));
		optString(params, "comment").ifPresent(v -> edgeObj.addProperty("comment", v));
		if (params.has("locationId")) {
			if (params.get("locationId").isJsonNull() || params.get("locationId").getAsString().isEmpty()) {
				edgeObj.remove("locationId");
			} else {
				edgeObj.addProperty("locationId", params.get("locationId").getAsString());
			}
		}
		if (params.has("groupIds") && params.get("groupIds").isJsonArray()) {
			edgeObj.add("groupIds", params.getAsJsonArray("groupIds"));
		}
		if (!edgeObj.has("apikey")) {
			edgeObj.addProperty("apikey", edgeId);
		}
		edgesObj.add(edgeId, edgeObj);
		json.add("edges", edgesObj);
		this.writeMetadataJson(json);
		this.refreshEdges();
		return this.adminGetFleet();
	}

	private JsonObject adminDeleteEdge(JsonObject params) {
		var edgeId = optString(params, "edgeId")
				.orElseThrow(() -> new IllegalArgumentException("edgeId required"));
		var json = this.readMetadataJson();
		var edgesObj = json.has("edges") && json.get("edges").isJsonObject() //
				? json.getAsJsonObject("edges") //
				: new JsonObject();
		edgesObj.remove(edgeId);
		json.add("edges", edgesObj);
		this.writeMetadataJson(json);
		this.edges.remove(edgeId);

		// Also remove this edge from all users
		var usersJson = this.readUsersJson();
		boolean usersChanged = false;
		for (var entry : usersJson.entrySet()) {
			if (entry.getValue().isJsonObject()) {
				var u = entry.getValue().getAsJsonObject();
				if (u.has("edges") && u.get("edges").isJsonObject()) {
					if (u.getAsJsonObject("edges").remove(edgeId) != null) {
						usersChanged = true;
					}
				}
			}
		}
		if (usersChanged) {
			this.writeUsersJson(usersJson);
			this.users.clear();
		}

		return this.adminGetFleet();
	}

	private JsonObject adminSetLocation(JsonObject params) {
		var metadata = this.readMetadataJson();
		var locsObj = metadata.has("locations") && metadata.get("locations").isJsonObject()
				? metadata.getAsJsonObject("locations")
				: new JsonObject();
		var locId = optString(params, "locationId")
				.orElseGet(() -> "loc-" + UUID.randomUUID().toString().substring(0, 8));
		var lObj = locsObj.has(locId) && locsObj.get(locId).isJsonObject()
				? locsObj.getAsJsonObject(locId)
				: new JsonObject();
		optString(params, "name").ifPresent(v -> lObj.addProperty("name", v));
		optString(params, "address").ifPresent(v -> lObj.addProperty("address", v));
		optString(params, "contact").ifPresent(v -> lObj.addProperty("contact", v));
		optString(params, "notes").ifPresent(v -> lObj.addProperty("notes", v));
		locsObj.add(locId, lObj);
		metadata.add("locations", locsObj);
		this.writeMetadataJson(metadata);
		return this.adminGetFleet();
	}

	private JsonObject adminDeleteLocation(JsonObject params) {
		var locId = optString(params, "locationId")
				.orElseThrow(() -> new IllegalArgumentException("locationId required"));
		var metadata = this.readMetadataJson();
		if (metadata.has("locations") && metadata.get("locations").isJsonObject()) {
			metadata.getAsJsonObject("locations").remove(locId);
		}
		if (metadata.has("edges") && metadata.get("edges").isJsonObject()) {
			for (var entry : metadata.getAsJsonObject("edges").entrySet()) {
				if (entry.getValue().isJsonObject()) {
					var eObj = entry.getValue().getAsJsonObject();
					if (locId.equals(optString(eObj, "locationId").orElse(null))) {
						eObj.remove("locationId");
					}
				}
			}
		}
		this.writeMetadataJson(metadata);
		this.refreshEdges();
		return this.adminGetFleet();
	}

	private JsonObject adminSetGroup(JsonObject params) {
		var metadata = this.readMetadataJson();
		var groupsObj = metadata.has("groups") && metadata.get("groups").isJsonObject()
				? metadata.getAsJsonObject("groups")
				: new JsonObject();
		var groupId = optString(params, "groupId")
				.orElseGet(() -> "group-" + UUID.randomUUID().toString().substring(0, 8));
		var gObj = groupsObj.has(groupId) && groupsObj.get(groupId).isJsonObject()
				? groupsObj.getAsJsonObject(groupId)
				: new JsonObject();
		optString(params, "name").ifPresent(v -> gObj.addProperty("name", v));
		optString(params, "type").ifPresent(v -> gObj.addProperty("type", v));
		optString(params, "gridPointEdgeId").ifPresent(v -> gObj.addProperty("gridPointEdgeId", v));
		optString(params, "notes").ifPresent(v -> gObj.addProperty("notes", v));
		groupsObj.add(groupId, gObj);
		metadata.add("groups", groupsObj);
		this.writeMetadataJson(metadata);
		return this.adminGetFleet();
	}

	private JsonObject adminDeleteGroup(JsonObject params) {
		var groupId = optString(params, "groupId")
				.orElseThrow(() -> new IllegalArgumentException("groupId required"));
		var metadata = this.readMetadataJson();
		if (metadata.has("groups") && metadata.get("groups").isJsonObject()) {
			metadata.getAsJsonObject("groups").remove(groupId);
		}
		if (metadata.has("edges") && metadata.get("edges").isJsonObject()) {
			for (var entry : metadata.getAsJsonObject("edges").entrySet()) {
				if (entry.getValue().isJsonObject()) {
					var eObj = entry.getValue().getAsJsonObject();
					if (eObj.has("groupIds") && eObj.get("groupIds").isJsonArray()) {
						var arr = eObj.getAsJsonArray("groupIds");
						var newArr = new JsonArray();
						for (var el : arr) {
							if (!groupId.equals(el.getAsString())) {
								newArr.add(el);
							}
						}
						eObj.add("groupIds", newArr);
					}
				}
			}
		}
		this.writeMetadataJson(metadata);
		this.refreshEdges();
		return this.adminGetFleet();
	}

	// ------------------------------------------------------------------
	// Public (unauthenticated) neutrava RPC: password recovery
	// ------------------------------------------------------------------

	private static final String PUBLIC_PREFIX = "neutravaPublic";
	private static final long RESET_TOKEN_TTL_SECONDS = 3600;
	private static final String PORTAL_URL = "https://ems.neutrava.ch";

	/**
	 * Handles public 'neutravaPublic*' JSON-RPC requests that do not require
	 * authentication (dispatched by the patched OnRequest of the UI-Websocket
	 * before the token check).
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handlePublicRpc(JsonrpcRequest request)
			throws OpenemsNamedException {
		var method = request.getMethod();
		if (method == null || !method.startsWith(PUBLIC_PREFIX)) {
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(method);
		}
		var params = request.getParams() != null ? request.getParams() : new JsonObject();
		var result = switch (method) {
		case "neutravaPublicRequestPasswordReset" -> this.publicRequestPasswordReset(params);
		case "neutravaPublicResetPassword" -> this.publicResetPassword(params);
		default -> throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(method);
		};
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), result));
	}

	private synchronized JsonObject publicRequestPasswordReset(JsonObject params) {
		var usernameOrEmail = optString(params, "username").orElse("").trim().toLowerCase();
		if (!usernameOrEmail.isEmpty()) {
			var json = this.readUsersJson();
			String matchedUser = null;
			String matchedEmail = null;
			for (var entry : json.entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				var u = entry.getValue().getAsJsonObject();
				var email = optString(u, "email").orElse(null);
				if (entry.getKey().equalsIgnoreCase(usernameOrEmail)
						|| (email != null && email.equalsIgnoreCase(usernameOrEmail))) {
					matchedUser = entry.getKey();
					matchedEmail = email;
					break;
				}
			}
			// Fallback: If user was found but has no explicit email field, check if username is an email address
			if (matchedUser != null && (matchedEmail == null || matchedEmail.isBlank())) {
				if (matchedUser.contains("@")) {
					matchedEmail = matchedUser;
				} else if (usernameOrEmail.contains("@")) {
					matchedEmail = usernameOrEmail;
				}
			}
			if (matchedUser != null && matchedEmail != null && !matchedEmail.isBlank()) {
				var rawToken = newResetToken();
				final var matchedUsername = matchedUser;
				var u = json.getAsJsonObject(matchedUser);
				var reset = new JsonObject();
				reset.addProperty("tokenHash", sha256Hex(rawToken));
				reset.addProperty("expiresAt", java.time.Instant.now().getEpochSecond() + RESET_TOKEN_TTL_SECONDS);
				u.add("passwordReset", reset);
				// Also persist the email if it was previously missing
				if (!u.has("email") || u.get("email").getAsString().isBlank()) {
					u.addProperty("email", matchedEmail);
				}
				json.add(matchedUser, u);
				this.writeUsersJson(json);
				final var email = matchedEmail;
				final var link = PORTAL_URL + "/reset?token=" + rawToken;
				CompletableFuture.runAsync(() -> {
					try {
						this.sendNeutravaMail(email, "neutrava EMS - Passwort zurücksetzen",
								"Hallo,\n\n"
										+ "du (oder jemand in deinem Namen) hat ein Zurücksetzen des Passworts "
										+ "für dein neutrava EMS Konto '" + matchedUsername + "' angefordert.\n\n"
										+ "Klicke auf folgenden Link, um ein neues Passwort zu setzen "
										+ "(gültig für 60 Minuten):\n\n" + link + "\n\n"
										+ "Falls du kein Zurücksetzen angefordert hast, kannst du diese "
										+ "E-Mail ignorieren.\n\nneutrava ag - ems.neutrava.ch");
						this.log.info("Password reset mail sent successfully to: {}", email);
					} catch (Exception e) {
						var cause = e;
						if (cause instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
							cause = (Exception) ite.getCause();
						}
						this.log.error("Failed to send password reset mail to {}: {}", email, cause.getMessage(), cause);
					}
				});
			} else {
				this.log.warn("Password reset requested for '{}' but no matching user or email found", usernameOrEmail);
			}
		}
		// Generic response: no user enumeration.
		var out = new JsonObject();
		out.addProperty("sent", true);
		return out;
	}

	private synchronized JsonObject publicResetPassword(JsonObject params) throws OpenemsNamedException {
		var token = optString(params, "token")
				.orElseThrow(() -> new IllegalArgumentException("token required"));
		var password = optString(params, "password")
				.orElseThrow(() -> new IllegalArgumentException("password required"));
		if (password.length() < 8) {
			throw OpenemsError.GENERIC.exception("password too short (min 8 chars)");
		}
		var tokenHash = sha256Hex(token);
		var now = java.time.Instant.now().getEpochSecond();
		var json = this.readUsersJson();
		for (var entry : json.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			var u = entry.getValue().getAsJsonObject();
			if (!u.has("passwordReset") || !u.get("passwordReset").isJsonObject()) {
				continue;
			}
			var reset = u.getAsJsonObject("passwordReset");
			var stored = optString(reset, "tokenHash").orElse("");
			var expires = reset.has("expiresAt") ? reset.get("expiresAt").getAsLong() : 0L;
			if (stored.equals(tokenHash) && expires >= now) {
				u.addProperty("passwordHash", hashPassword(password));
				u.remove("password");
				u.remove("passwordReset");
				json.add(entry.getKey(), u);
				this.writeUsersJson(json);
				this.users.remove(entry.getKey());
				var out = new JsonObject();
				out.addProperty("username", entry.getKey());
				return out;
			}
		}
		throw OpenemsError.GENERIC.exception("invalid or expired token");
	}

	// ------------------------------------------------------------------
	// neutrava e-mail helpers
	// ------------------------------------------------------------------

	private void sendWelcomeMailAsync(String username, String email) {
		var reset = this.createResetTokenForUser(username);
		var link = reset != null ? PORTAL_URL + "/reset?token=" + reset : PORTAL_URL;
		CompletableFuture.runAsync(() -> {
			try {
				this.sendNeutravaMail(email, "Willkommen bei neutrava EMS",
						"Hallo,\n\n"
								+ "für dich wurde ein Zugang zum neutrava Energiemanagement-Portal eingerichtet.\n\n"
								+ "Portal:  " + PORTAL_URL + "\n"
								+ "Benutzer: " + username + "\n\n"
								+ "Bitte setze dein persönliches Passwort über folgenden Link "
								+ "(gültig für 60 Minuten):\n\n" + link + "\n\n"
								+ "Bei Fragen erreichst du uns unter support@neutrava.ch.\n\n"
								+ "neutrava ag - ems.neutrava.ch");
				this.log.info("Welcome mail sent successfully to: {}", email);
			} catch (Exception e) {
				var cause = e;
				if (cause instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
					cause = (Exception) ite.getCause();
				}
				this.log.error("Failed to send welcome mail to {}: {}", email, cause.getMessage(), cause);
			}
		});
	}

	private synchronized String createResetTokenForUser(String username) {
		var json = this.readUsersJson();
		if (!json.has(username) || !json.get(username).isJsonObject()) {
			return null;
		}
		var rawToken = newResetToken();
		var u = json.getAsJsonObject(username);
		var reset = new JsonObject();
		reset.addProperty("tokenHash", sha256Hex(rawToken));
		reset.addProperty("expiresAt", java.time.Instant.now().getEpochSecond() + RESET_TOKEN_TTL_SECONDS);
		u.add("passwordReset", reset);
		json.add(username, u);
		this.writeUsersJson(json);
		return rawToken;
	}

	private static String newResetToken() {
		var bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String sha256Hex(String value) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			var sb = new StringBuilder();
			for (var b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sends a mail via the ch.neutrava.openems.mailer bundle, looked up via
	 * BundleContext to avoid a compile/manifest dependency.
	 */
	private void sendNeutravaMail(String to, String subject, String body) throws Exception {
		var bundle = org.osgi.framework.FrameworkUtil.getBundle(MetadataDummy.class);
		if (bundle == null) {
			throw new IllegalStateException("no bundle context");
		}
		var ctx = bundle.getBundleContext();
		var ref = ctx.getServiceReference(NEUTRAVA_MAILER_SERVICE);
		if (ref == null) {
			throw new IllegalStateException("NeutravaMailer service not registered (mailer bundle missing)");
		}
		var service = ctx.getService(ref);
		try {
			var m = service.getClass().getMethod("sendMail", String.class, String.class, String.class);
			m.invoke(service, to, subject, body);
		} catch (java.lang.reflect.InvocationTargetException e) {
			if (e.getCause() instanceof Exception ex) {
				throw ex;
			}
			throw e;
		} finally {
			ctx.ungetService(ref);
		}
	}

	private synchronized JsonObject readMailerJson() {
		return readJsonFile(writableFile(MAILER_FILE_PATHS));
	}

	private synchronized void writeMailerJson(JsonObject json) {
		writeJsonFile(writableFile(MAILER_FILE_PATHS), json);
	}

	private JsonObject adminGetMailConfig() {
		var json = this.readMailerJson();
		var out = json.deepCopy();
		if (out.has("password")) {
			out.addProperty("password", "******");
		}
		out.addProperty("configured", this.isMailerConfigured());
		return out;
	}

	private boolean isMailerConfigured() {
		var json = this.readMailerJson();
		return json.has("host") && json.has("from");
	}

	private JsonObject adminSetMailConfig(JsonObject params) {
		var json = this.readMailerJson();
		optString(params, "host").ifPresent(v -> json.addProperty("host", v));
		if (params.has("port") && params.get("port").isJsonPrimitive()) {
			json.addProperty("port", params.get("port").getAsInt());
		}
		optString(params, "tls").ifPresent(v -> json.addProperty("tls", v));
		optString(params, "username").ifPresent(v -> json.addProperty("username", v));
		optString(params, "password").ifPresent(v -> {
			if (!"******".equals(v)) {
				json.addProperty("password", v);
			}
		});
		optString(params, "from").ifPresent(v -> json.addProperty("from", v));
		if (params.has("to") && params.get("to").isJsonArray()) {
			json.add("to", params.getAsJsonArray("to"));
		}
		this.writeMailerJson(json);
		return this.adminGetMailConfig();
	}

	private JsonObject adminSendTestMail(JsonObject params) {
		var to = optString(params, "to")
				.orElseThrow(() -> new IllegalArgumentException("to required"));
		var out = new JsonObject();
		try {
			this.sendNeutravaMail(to, "neutrava EMS - Testmail",
					"Dies ist eine Testmail vom neutrava EMS (ems.neutrava.ch).\n\n"
							+ "Wenn du diese Mail erhältst, funktioniert die SMTP-Konfiguration.");
			out.addProperty("ok", true);
		} catch (Exception e) {
			out.addProperty("ok", false);
			out.addProperty("error", e.getMessage());
		}
		return out;
	}

	/**
	 * PBKDF2-SHA256 password hashing. Format:
	 * "pbkdf2:ITERATIONS:SALT_B64:HASH_B64". Must stay in sync with
	 * neutrava/scripts/manage_users.py and ch.neutrava.openems.auth.
	 */
	public static String hashPassword(String password) {
		try {
			var salt = new byte[16];
			new SecureRandom().nextBytes(salt);
			var iterations = 210_000;
			var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
			var hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
			return "pbkdf2:" + iterations + ":" + Base64.getEncoder().encodeToString(salt) + ":"
					+ Base64.getEncoder().encodeToString(hash);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean verifyPassword(String password, String stored) {
		try {
			if (stored != null && stored.startsWith("pbkdf2:")) {
				var parts = stored.split(":");
				var iterations = Integer.parseInt(parts[1]);
				var salt = Base64.getDecoder().decode(parts[2]);
				var expected = Base64.getDecoder().decode(parts[3]);
				var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expected.length * 8);
				var actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
				return MessageDigest.isEqual(expected, actual);
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	// ------------------------------------------------------------------
	// Remaining Metadata API
	// ------------------------------------------------------------------

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		var json = this.readUsersJson();
		var userObj = json.has(user.getName()) && json.get(user.getName()).isJsonObject() //
				? json.getAsJsonObject(user.getName()) //
				: new JsonObject();
		var edgesObj = userObj.has("edges") && userObj.get("edges").isJsonObject() //
				? userObj.getAsJsonObject("edges") //
				: new JsonObject();
		edgesObj.addProperty(edge.getId(), "OWNER");
		userObj.add("edges", edgesObj);
		json.add(user.getName(), userObj);
		this.writeUsersJson(json);
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Metadata.getSetupProtocol() is not implemented");
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Metadata.getSetupProtocolData() is not implemented");
	}

	@Override
	public SetupProtocolCoreInfo getLatestSetupProtocolCoreInfo(String edgeId) throws OpenemsNamedException {
		return null;
	}

	@Override
	public List<SetupProtocolCoreInfo> getProtocolsCoreInfo(String edgeId) throws OpenemsNamedException {
		return Collections.emptyList();
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new UnsupportedOperationException("Metadata.submitSetupProtocol() is not implemented");
	}

	@Override
	public void createSerialNumberExtensionProtocol(String edgeId, Map<String, Map<String, String>> serialNumbers,
			List<SetupProtocolItem> items) {
		this.log.info("SerialNumberProtocol[{}]: {}, {}", edgeId, serialNumbers, items);
	}

	@Override
	public EventAdmin getEventAdmin() {
		return this.eventAdmin;
	}

	@Override
	public void handleEvent(Event event) {
		var reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_CONFIG -> {
			this.edgeHandler.setEdgeConfigFromEvent(reader, (edge, oldConfig, newConfig) -> {
				EventBuilder.from(this.eventAdmin, Edge.Events.ON_UPDATE_CONFIG) //
						.addArg(Edge.Events.OnUpdateConfig.EDGE_ID, edge.getId()) //
						.addArg(Edge.Events.OnUpdateConfig.OLD_CONFIG, oldConfig) //
						.addArg(Edge.Events.OnUpdateConfig.NEW_CONFIG, newConfig) //
						.send();
			});
		}
		}
	}

	@Override
	public EdgeHandler edge() {
		return this.edgeHandler;
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		return Optional.empty();
	}

	@Override
	public Optional<String> getEmsTypeForEdge(String edgeId) {
		return Optional.empty();
	}

	@Override
	public void logGenericSystemLog(GenericSystemLog systemLog) {
		this.logInfo(this.log,
				"%s on %s executed %s [%s]".formatted(systemLog.user().getId(), systemLog.edgeId(), systemLog.teaser(),
						systemLog.getValues().entrySet().stream() //
								.map(t -> t.getKey() + "=" + t.getValue()) //
								.collect(joining(", "))));
	}

	@Override
	public UpdateMetadataCache.Notification generateUpdateMetadataCacheNotification() {
		var apikeysToEdgeIds = this.edges.values().stream() //
				.collect(java.util.stream.Collectors.toMap(//
						e -> e.getId(), //
						e -> e.getApikey()));
		return new UpdateMetadataCache.Notification(apikeysToEdgeIds);
	}

}
