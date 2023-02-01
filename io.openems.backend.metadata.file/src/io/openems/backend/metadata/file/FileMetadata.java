package io.openems.backend.metadata.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.SimpleEdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.common.OpenemsOEM;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

/**
 * This implementation of MetadataService reads Edges configuration from a file.
 * The layout of the file is as follows:
 *
 * <pre>
 * {
 *   edges: {
 *     [edgeId: string]: {
 *       comment: string,
 *       apikey: string
 *       setuppassword?: string
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * This implementation does not require any login. It always serves the same
 * user, which has 'ADMIN'-permissions on all given Edges.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.File", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class FileMetadata extends AbstractMetadata implements Metadata, EventHandler {

	private static final String USER_ID = "admin";
	private static final String USER_NAME = "Administrator";
	private static final Role USER_GLOBAL_ROLE = Role.ADMIN;

	private static Language LANGUAGE = Language.DE;

	private final Logger log = LoggerFactory.getLogger(FileMetadata.class);
	private final Map<String, MyEdge> edges = new HashMap<>();
	private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();

	private User user;
	private String path = "";

	@Reference
	private EventAdmin eventAdmin;

	public FileMetadata() {
		super("Metadata.File");
		this.user = FileMetadata.generateUser();
	}

	@Activate
	private void activate(Config config) {
		this.log.info("Activate [path=" + config.path() + "]");
		this.path = config.path();

		// Read the data async
		CompletableFuture.runAsync(() -> {
			this.refreshData();
		});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		return this.user;
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		if (this.user.getToken().equals(token)) {
			return this.user;
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	@Override
	public void logout(User user) {
		this.user = FileMetadata.generateUser();
	}

	@Override
	public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
		this.refreshData();
		for (Entry<String, MyEdge> entry : this.edges.entrySet()) {
			var edge = entry.getValue();
			if (edge.getApikey().equals(apikey)) {
				return Optional.of(edge.getId());
			}
		}
		return Optional.empty();
	}

	@Override
	public synchronized Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		this.refreshData();
		for (MyEdge edge : this.edges.values()) {
			if (edge.getSetupPassword().equals(setupPassword)) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}

	@Override
	public synchronized Optional<Edge> getEdge(String edgeId) {
		this.refreshData();
		Edge edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge);
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.of(this.user);
	}

	@Override
	public synchronized Collection<Edge> getAllOfflineEdges() {
		this.refreshData();
		return this.edges.values().stream().filter(Edge::isOffline).collect(Collectors.toUnmodifiableList());
	}

	private synchronized void refreshData() {
		if (this.edges.isEmpty()) {
			// read file
			var sb = new StringBuilder();
			String line = null;
			try (var br = new BufferedReader(new FileReader(this.path))) {
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			} catch (IOException e) {
				this.logWarn(this.log, "Unable to read file [" + this.path + "]: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			List<MyEdge> edges = new ArrayList<>();

			// parse to JSON
			try {
				var config = JsonUtils.parse(sb.toString());
				var jEdges = JsonUtils.getAsJsonObject(config, "edges");
				for (Entry<String, JsonElement> entry : jEdges.entrySet()) {
					var edge = JsonUtils.getAsJsonObject(entry.getValue());
					edges.add(new MyEdge(//
							this, //
							entry.getKey(), // Edge-ID
							JsonUtils.getAsString(edge, "apikey"), //
							JsonUtils.getAsOptionalString(edge, "setuppassword").orElse(""), //
							JsonUtils.getAsString(edge, "comment"), //
							"", // Version
							"" // Product-Type
					));
				}
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to JSON-parse file [" + this.path + "]: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			// Add Edges and configure User permissions
			for (MyEdge edge : edges) {
				this.edges.put(edge.getId(), edge);
				this.user.setRole(edge.getId(), Role.ADMIN);
			}
		}
		this.setInitialized();
	}

	private static User generateUser() {
		return new User(FileMetadata.USER_ID, FileMetadata.USER_NAME, UUID.randomUUID().toString(),
				FileMetadata.LANGUAGE, FileMetadata.USER_GLOBAL_ROLE, new TreeMap<>());
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.addEdgeToUser() is not implemented");
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.getUserInformation() is not implemented");
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.setUserInformation() is not implemented");
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.getSetupProtocol() is not implemented");
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.getSetupProtocolData() is not implemented");
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new UnsupportedOperationException("FileMetadata.submitSetupProtocol() is not implemented");
	}

	@Override
	public void registerUser(JsonObject jsonObject, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.registerUser() is not implemented");
	}

	@Override
	public void updateUserLanguage(User user, Language locale) throws OpenemsNamedException {
		FileMetadata.LANGUAGE = locale;
	}

	@Override
	public EventAdmin getEventAdmin() {
		return this.eventAdmin;
	}

	@Override
	public EdgeHandler edge() {
		return this.edgeHandler;
	}

	@Override
	public void handleEvent(Event event) {
		var reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_CONFIG:
			this.edgeHandler.setEdgeConfigFromEvent(reader);
			break;
		}
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		throw new UnsupportedOperationException("FileMetadata.getSerialNumberForEdge() is not implemented");
	}

	@Override
	public List<AlertingSetting> getUserAlertingSettings(String edgeId) {
		throw new UnsupportedOperationException("FileMetadata.getUserAlertingSettings() is not implemented");
	}

	@Override
	public AlertingSetting getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		throw new UnsupportedOperationException("FileMetadata.getUserAlertingSettings() is not implemented");
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<AlertingSetting> users) {
		throw new UnsupportedOperationException("FileMetadata.setUserAlertingSettings() is not implemented");
	}

}
