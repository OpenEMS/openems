package io.openems.backend.oem.fenecon;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.common.event.EventReader;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

@Component
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class UpdateProducttypeHandler implements EventHandler, DebugLoggable {

	private static final Map<String, String> APPID_TO_PRODUCTTYPE = ImmutableMap.<String, String>builder()//
			.put("App.FENECON.Home", "home") //
			.put("App.FENECON.Home.20", "home-20") //
			.put("App.FENECON.Home.30", "home-30") //
			.put("App.FENECON.Home6", "home-6") //
			.put("App.FENECON.Home10.Gen2", "home-10") //
			.put("App.FENECON.Home15", "home-15") //
			.put("App.FENECON.Commercial.92", "commercial-92") //
			.put("App.FENECON.Commercial.92.ClusterMaster", "commercial-92-cluster") //
			.put("App.FENECON.Commercial.92.ClusterSlave", "commercial-92-cluster-slave") //
			.put("App.FENECON.Industrial.S.ISK110", "Industrial S") //
			.put("App.FENECON.Industrial.S.ISK011", "Industrial S") //
			.put("App.FENECON.Industrial.S.ISK010", "Industrial S") //
			.put("App.FENECON.Industrial.L.ILK710", "Industrial L") //
			.build();

	private final Logger log = LoggerFactory.getLogger(UpdateProducttypeHandler.class);

	private final LatestTaskPerKeyExecutor<String> updateProducttypeExecutor = new LatestTaskPerKeyExecutor<String>(
			(ThreadPoolExecutor) Executors.newFixedThreadPool(1));

	@Deactivate
	private void deactivate() {
		this.updateProducttypeExecutor.shutdown();
	}

	@Override
	public void handleEvent(Event event) {
		final var eventReader = new EventReader(event);
		final var edge = (Edge) eventReader.getProperty(Edge.Events.OnSetConfig.EDGE);
		final var edgeConfig = (EdgeConfig) eventReader.getProperty(Edge.Events.OnSetConfig.CONFIG);

		this.updateProducttypeExecutor.execute(edge.getId(), () -> {
			final var appManager = edgeConfig.getComponent("_appManager").orElse(null);
			if (appManager == null) {
				return;
			}

			final var apps = appManager.getProperty("apps") //
					.filter(JsonElement::isJsonArray) //
					.map(JsonElement::getAsJsonArray) //
					.orElseGet(JsonArray::new);

			final var producttypes = JsonUtils.stream(apps) //
					.map(JsonElement::getAsJsonObject) //
					.map(t -> t.get("appId").getAsString()) //
					.map(APPID_TO_PRODUCTTYPE::get) //
					.filter(Objects::nonNull) //
					.toList();

			if (producttypes.isEmpty()) {
				return;
			}

			if (producttypes.size() > 1) {
				this.log.info("More than 1 producttype detected for " + edge.getId());
				return;
			}

			edge.setProducttype(producttypes.get(0));
		});
	}

	@Override
	public String debugLog() {
		return "[" + this.getClass().getSimpleName() + "] " + this.updateProducttypeExecutor.debugLog();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return null;
	}

}
