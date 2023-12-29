package io.openems.backend.metadata.file;

import static java.util.stream.Collectors.joining;

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
import io.openems.backend.common.metadata.UserAlertingSettings;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.SimpleEdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

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
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class MetadataFile extends AbstractMetadata implements Metadata, EventHandler {

	private static final String USER_ID = "admin";
	private static final String USER_NAME = "Administrator";
	private static final Role USER_GLOBAL_ROLE = Role.ADMIN;

	private static Language LANGUAGE = Language.DE;

	private final Logger log = LoggerFactory.getLogger(MetadataFile.class);
	private final Map<String, MyEdge> edges = new HashMap<>();
	private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();

	@Reference
	private EventAdmin eventAdmin;

	private User user = this.generateUser();
	private String path = "";

	public MetadataFile() {
		super("Metadata.File");
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
		return this.user = this.generateUser();
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
		this.user = this.generateUser();
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
			}

			final var previousUser = this.user;
			final var hasMultipleEdges = edges.size() > 1;
			if (previousUser.hasMultipleEdges() != hasMultipleEdges) {
				this.user = new User(previousUser.getId(), previousUser.getName(), previousUser.getToken(),
						previousUser.getLanguage(), previousUser.getGlobalRole(), previousUser.getEdgeRoles(),
						hasMultipleEdges);
			}
		}
		this.setInitialized();
	}

	private User generateUser() {
		return new User(MetadataFile.USER_ID, MetadataFile.USER_NAME, UUID.randomUUID().toString(),
				MetadataFile.LANGUAGE, MetadataFile.USER_GLOBAL_ROLE, this.edges.size() > 1);
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
	public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
		throw new UnsupportedOperationException("FileMetadata.registerUser() is not implemented");
	}

	@Override
	public void updateUserLanguage(User user, Language locale) throws OpenemsNamedException {
		MetadataFile.LANGUAGE = locale;
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
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
		throw new UnsupportedOperationException("FileMetadata.getUserAlertingSettings() is not implemented");
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		throw new UnsupportedOperationException("FileMetadata.getUserAlertingSettings() is not implemented");
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> users) {
		throw new UnsupportedOperationException("FileMetadata.setUserAlertingSettings() is not implemented");
	}

	@Override
	public List<EdgeMetadata> getPageDevice(User user, PaginationOptions paginationOptions)
			throws OpenemsNamedException {
		var pagesStream = this.edges.values().stream();
		final var query = paginationOptions.getQuery();
		if (query != null) {
			pagesStream = pagesStream.filter(//
					edge -> StringUtils.containsWithNullCheck(edge.getId(), query) //
							|| StringUtils.containsWithNullCheck(edge.getComment(), query) //
							|| StringUtils.containsWithNullCheck(edge.getProducttype(), query) //
			);
		}
		final var searchParams = paginationOptions.getSearchParams();
		if (searchParams != null) {
			if (searchParams.searchIsOnline()) {
				pagesStream = pagesStream.filter(edge -> edge.isOnline() == searchParams.isOnline());
			}
			if (searchParams.productTypes() != null) {
				pagesStream = pagesStream.filter(edge -> searchParams.productTypes().contains(edge.getProducttype()));
			}
			// TODO sum state filter
		}
		return pagesStream //
				.sorted((s1, s2) -> s1.getId().compareTo(s2.getId())) //
				.skip(paginationOptions.getPage() * paginationOptions.getLimit()) //
				.limit(paginationOptions.getLimit()) //
				.peek(t -> user.setRole(t.getId(), Role.ADMIN)) //
				.map(myEdge -> {
					return new EdgeMetadata(//
							myEdge.getId(), //
							myEdge.getComment(), //
							myEdge.getProducttype(), //
							myEdge.getVersion(), //
							Role.ADMIN, //
							myEdge.isOnline(), //
							myEdge.getLastmessage(), //
							null, // firstSetupProtocol
							Level.OK);
				}).toList();
	}

	@Override
	public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
		final var edge = this.edges.get(edgeId);
		if (edge == null) {
			return null;
		}
		user.setRole(edgeId, Role.ADMIN);

		return new EdgeMetadata(//
				edge.getId(), //
				edge.getComment(), //
				edge.getProducttype(), //
				edge.getVersion(), //
				Role.ADMIN, //
				edge.isOnline(), //
				edge.getLastmessage(), //
				null, // firstSetupProtocol
				Level.OK //
		);
	}

	@Override
	public void logGenericSystemLog(GenericSystemLog systemLog) {
		this.logInfo(this.log,
				"%s on %s executed %s [%s]".formatted(systemLog.user().getId(), systemLog.edgeId(), systemLog.teaser(),
						systemLog.getValues().entrySet().stream() //
								.map(t -> t.getKey() + "=" + t.getValue()) //
								.collect(joining(", "))));
	}

}
