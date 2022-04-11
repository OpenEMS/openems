package io.openems.backend.metadata.edge.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Edge.State;
import io.openems.backend.common.metadata.EdgeMetadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

/**
 * This implementation of EdgeMetadata reads Edges configuration from a file.
 * The layout of the file is as follows:
 *
 * <pre>
 * {
 *   edges: {
 *     [edgeId: string]: {
 *       comment: string,
 *       apikey: string
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * Combine this service with an AuthenticationMetdata to retrieve Users and Permissions.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "EdgeMetadata.File", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class FileEdgeMetadata extends AbstractOpenemsBackendComponent implements EdgeMetadata {

	public FileEdgeMetadata(String name) {
		super("EdgeBackendMetadata.File");
	}

	private final Logger log = LoggerFactory.getLogger(FileEdgeMetadata.class);
	private final Map<String, MyEdge> edges = new HashMap<>();

	private User user;
	private String path = "";

	@Activate
	private void activate(Config config) {
		this.log.info("Activate [path=" + config.path() + "]");
		this.path = config.path();

		this.refreshData();
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
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
	public synchronized Collection<Edge> getAllEdges() {
		this.refreshData();
		return Collections.unmodifiableCollection(this.edges.values());
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
							entry.getKey(), // Edge-ID
							JsonUtils.getAsString(edge, "apikey"), //
							JsonUtils.getAsString(edge, "setuppassword"), //
							JsonUtils.getAsString(edge, "comment"), //
							State.ACTIVE, // State
							"", // Version
							"", // Product-Type
							Level.OK, // Sum-State
							new EdgeConfig() // Config
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
	}
}
