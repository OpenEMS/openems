package io.openems.backend.metadata.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.FileUtils;
import io.openems.common.utils.JsonKeys;
import io.openems.common.utils.JsonUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

/**
 * This implementation of MetadataService reads Edges configuration from a file.
 * The path of the config is used to determine the location of the file which has to be JSON encoded with the following structure:
 * <pre>
 *  {
 *    "edges" : [{
 *    	"edgeId": string,
 *    	"apiKey": string,
 *    	"comment": string,
 *    	"version": string,
 *    	"productType": string,
 *    	"ipv4": string,
 *    	"soc": string,
 *    }]
 *  }
 *  </pre>
 */
@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.File", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class File extends AbstractOpenemsBackendComponent implements Metadata {

	private final Logger log = LoggerFactory.getLogger(File.class);
	private final Map<String, Edge> edges = new HashMap<>();
	private String path = "";

	public File() {
		super("Metadata.File");
	}

	@Activate
	void activate(Config config) {
		log.info("Activate [path=" + config.path() + "]");
		this.path = config.path();

		// Read the data async
		CompletableFuture.runAsync(this::refreshData);
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
		Optional<Optional<Entry<String, Edge>>> edgeId = Optional.of(this.edges.entrySet().stream().filter(
			e -> e.getValue().getApikey().equals(apikey)).findFirst());
		if (edgeId.isPresent() && edgeId.get().isPresent()) {
			return Optional.of(edgeId.get().get().getValue().getId());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public synchronized Optional<Edge> getEdge(String edgeId) {
		this.refreshData();
		Edge edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge);
	}

	/**
	 * In case there is a path for a configuration file configured, this method extracts the JSON-encoded information
	 * and fills the fields of the class.<br>
	 * See also: {@link File#edges}
	 */
	private synchronized void refreshData() {
		if (!this.edges.isEmpty()) {
			return;
		}

		// parse to JSON
		try {
			// throws exception in case the file could not be read
			StringBuilder sb = FileUtils.checkAndGetFileContent(this.path);
			JsonElement config = JsonUtils.parse(sb != null ? sb.toString() : null);
			JsonArray jEdges = JsonUtils.getAsJsonArray(config, JsonKeys.EDGES.value());
			for (JsonElement jEdge : jEdges) {
				// handle the user
				// handle the connected edges
				String edgeId = JsonUtils.getAsString(jEdge, JsonKeys.EDGE_ID.value());
				Edge edge = new Edge(
					edgeId,
					JsonUtils.getAsString(jEdge, JsonKeys.API_KEY.value()),
					JsonUtils.getAsString(jEdge, JsonKeys.COMMENT.value()),
					State.ACTIVE,
					JsonUtils.getAsString(jEdge, JsonKeys.VERSION.value()),
					JsonUtils.getAsString(jEdge, JsonKeys.PRODUCT_TYPE.value()),
					new EdgeConfig(),
					JsonUtils.getAsInt(jEdge, JsonKeys.SOC.value()),
					JsonUtils.getAsString(jEdge, JsonKeys.IPV4.value()),
					Level.OK
				);
				this.edges.put(edgeId, edge);
			}
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse JSON-file [" + this.path + "]: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
