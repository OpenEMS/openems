package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;

@Component(//
		service = { //
				AggregateTask.class, //
				EvseClusterTask.class, //
				EvseClusterTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class EvseClusterTaskImpl implements EvseClusterTask {

	private static final String CTRL_IDS = "ctrl.ids";
	private static final String EVSE_CLUSTER_FACTORY_ID = "Evse.Controller.Cluster";

	private final ComponentManager componentManager;

	private final Map<String, Integer> evseIdDeltas = new LinkedHashMap<>();

	@Activate
	public EvseClusterTaskImpl(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public AggregateTaskExecuteConstraints getExecuteConstraints() {
		return new AggregateTaskExecuteConstraints(Set.of(//
				// Needs to run after the AggregateTask.ComponentAggregateTask
				ComponentAggregateTask.class //
		));
	}

	private void execute(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		var evseIdsToAdd = this.getEvseIdsToAdd();
		var evseIdsToDelete = new LinkedList<>(this.getEvseIdsToDelete());

		var otherEvseIds = AppConfiguration
				.flatMap(otherAppConfigurations, EvseClusterTask.class, ClusterConfiguration::evseIds)//
				.toList();
		evseIdsToDelete.removeAll(otherEvseIds);

		if (evseIdsToAdd.isEmpty() && evseIdsToDelete.isEmpty()) {
			return;
		}

		var clusterControllers = this.componentManager.getEdgeConfig().getComponentsByFactory(EVSE_CLUSTER_FACTORY_ID);
		if (clusterControllers.isEmpty()) {
			if (evseIdsToAdd.isEmpty()) {
				return;
			}
			throw new OpenemsException("No Cluster Controller!");
		}

		var clusterController = clusterControllers.getFirst();

		Optional<JsonElement> idsProperty = clusterController.getProperty(CTRL_IDS);
		List<String> evseIds = idsProperty
				.map(s -> new LinkedList<>(JsonSerializerUtil.stringSerializer().toListSerializer().deserialize(s)))
				.orElse(new LinkedList<>());

		if (!evseIdsToAdd.isEmpty()) {
			evseIds.addAll(evseIdsToAdd);
		}
		if (!evseIdsToDelete.isEmpty()) {
			evseIds.removeAll(evseIdsToDelete);
		}

		evseIds = new LinkedList<>(new LinkedHashSet<>(evseIds));

		var jsonArray = JsonUtils.generateJsonArray(evseIds, s -> new JsonPrimitive(Objects.requireNonNull(s)));
		this.componentManager.handleUpdateComponentConfigRequest(user, //
				new UpdateComponentConfig.Request(clusterController.getId(),
						List.of(new Property(CTRL_IDS, jsonArray))));
	}

	@Override
	public void aggregate(ClusterConfiguration instance, ClusterConfiguration oldConfig) {
		if (instance != null) {
			this.addDelta(instance.evseIds(), 1);
		}
		if (oldConfig != null) {
			this.addDelta(oldConfig.evseIds(), -1);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations);
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new EvseClusterExecutionConfiguration(this.getEvseIdsToAdd(), this.getEvseIdsToDelete());
	}

	private void addDelta(List<String> evseIds, int delta) {
		for (var evseId : new LinkedHashSet<>(evseIds)) {
			var newDelta = this.evseIdDeltas.merge(evseId, delta, Integer::sum);
			if (newDelta == 0) {
				this.evseIdDeltas.remove(evseId);
			}
		}
	}

	private List<String> getEvseIdsToAdd() {
		return this.evseIdDeltas.entrySet().stream() //
				.filter(entry -> entry.getValue() > 0) //
				.map(Map.Entry::getKey) //
				.toList();
	}

	private List<String> getEvseIdsToDelete() {
		return this.evseIdDeltas.entrySet().stream() //
				.filter(entry -> entry.getValue() < 0) //
				.map(Map.Entry::getKey) //
				.toList();
	}

	private record EvseClusterExecutionConfiguration(//
			List<String> evseIdsToAdd, //
			List<String> evseIdsToDelete) implements AggregateTask.AggregateTaskExecutionConfiguration {

		private EvseClusterExecutionConfiguration {
			Objects.requireNonNull(evseIdsToAdd);
			Objects.requireNonNull(evseIdsToDelete);
		}

		@Override
		public String identifier() {
			return "EvseClusterTask";
		}

		@Override
		public JsonElement toJson() {
			if (this.evseIdsToAdd.isEmpty() && this.evseIdsToDelete.isEmpty()) {
				return JsonNull.INSTANCE;
			}
			return JsonUtils.buildJsonObject() //
					.add("evse.ids.toAdd",
							this.evseIdsToAdd.stream().map(JsonPrimitive::new).collect(JsonUtils.toJsonArray()))

					.add("evse.ids.toDelete",
							this.evseIdsToDelete.stream().map(JsonPrimitive::new).collect(JsonUtils.toJsonArray()))//
					.build();
		}
	}

	@Override
	public void validate(//
			final List<String> errors, //
			final AppConfiguration appConfiguration, //
			final ClusterConfiguration config, //
			final Map<OpenemsAppInstance, AppConfiguration> allConfigurations //
	) {
		var clusterControllers = this.componentManager.getEdgeConfig().getComponentsByFactory(EVSE_CLUSTER_FACTORY_ID);

		if (clusterControllers.isEmpty() && !config.evseIds().isEmpty()) {
			errors.add("cluster controller not exists");
			return;
		}
		if (clusterControllers.isEmpty()) {
			return;
		}

		var clusterController = clusterControllers.getFirst();
		var rawIds = clusterController.getProperty(CTRL_IDS)
				.map(ids -> JsonSerializerUtil.stringSerializer().toListSerializer().deserialize(ids))
				.orElse(List.of());

		var missing = new ArrayList<>(config.evseIds());

		missing.removeAll(rawIds);

		if (!missing.isEmpty()) {
			errors.add("EvseId '" + String.join(", ", missing) + "' " + (missing.size() > 1 ? "are" : "is")
					+ " not added on " + clusterController.getId());
		}
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateCluster");
	}

	@Override
	public void reset() {
		this.evseIdDeltas.clear();
	}
}
