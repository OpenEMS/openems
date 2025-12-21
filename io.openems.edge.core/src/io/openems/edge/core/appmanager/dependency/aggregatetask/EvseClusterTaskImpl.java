package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

	private final ComponentManager componentManager;

	private List<String> evseIdsToAdd = new LinkedList<String>();
	private List<String> evseIdsToDelete = new LinkedList<String>();

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

	private void execute(List<AppConfiguration> otherAppConfigurations, User user) throws OpenemsNamedException {
		if (this.evseIdsToAdd.isEmpty() && this.evseIdsToDelete.isEmpty()) {
			return;
		}

		var clusterControllers = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Evse.Controller.Cluster");
		if (clusterControllers.isEmpty()) {
			if (this.evseIdsToAdd.isEmpty()) {
				return;
			}
			throw new OpenemsException("No Cluster Controller!");
		}

		var clusterController = clusterControllers.getFirst();

		Optional<JsonElement> idsProperty = clusterController.getProperty("ctrl.ids");
		List<String> evseIds = idsProperty.map(s -> {
			return new LinkedList<>(JsonSerializerUtil.stringSerializer().toListSerializer().deserialize(s));
		}).orElse(new LinkedList<>());

		if (!this.evseIdsToAdd.isEmpty()) {
			evseIds.addAll(this.evseIdsToAdd);
		}
		if (!this.evseIdsToDelete.isEmpty()) {
			evseIds.removeAll(this.evseIdsToDelete);
		}

		evseIds = new LinkedList<>(new LinkedHashSet<>(evseIds));

		var jsonArray = JsonUtils.generateJsonArray(evseIds, input -> new JsonPrimitive(input));
		this.componentManager.handleUpdateComponentConfigRequest(user, //
				new UpdateComponentConfig.Request(clusterController.getId(),
						List.of(new Property("ctrl.ids", jsonArray))));
	}

	@Override
	public void aggregate(ClusterConfiguration instance, ClusterConfiguration oldConfig) {
		if (instance != null) {
			this.evseIdsToAdd.addAll(instance.evseIds());
		}
		if (oldConfig != null) {
			this.evseIdsToDelete.addAll(oldConfig.evseIds());
		}
		this.evseIdsToAdd.removeAll(this.evseIdsToDelete);
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(otherAppConfigurations, user);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(otherAppConfigurations, user);
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new EvseClusterExecutionConfiguration(this.evseIdsToAdd, this.evseIdsToDelete);
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
					.add("evse.ids.toAdd", this.evseIdsToAdd.stream().map(t -> {
						return new JsonPrimitive(t);
					}).collect(JsonUtils.toJsonArray()))

					.add("evse.ids.toDelete", this.evseIdsToAdd.stream().map(t -> {
						return new JsonPrimitive(t);
					}).collect(JsonUtils.toJsonArray()))//
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
		var clusterControllers = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Evse.Controller.Cluster");

		if (clusterControllers.isEmpty() && !config.evseIds().isEmpty()) {
			errors.add("cluster controller not exists");
			return;
		}

		var clusterController = clusterControllers.getFirst();
		var rawIds = clusterController.getProperty("ctrl.ids").get().getAsJsonArray().asList().stream()
				.map(je -> je.getAsString()).toList();

		var missing = new ArrayList<>(config.evseIds());

		missing.removeAll(rawIds);

		if (!missing.isEmpty()) {
			errors.add("EvseId '" + missing.stream().collect(Collectors.joining(", ")) + "' "
					+ (missing.size() > 1 ? "are" : "is") + " not added on " + clusterController.getId());
		}
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateCluster");
	}

	@Override
	public void reset() {
		this.evseIdsToAdd = new LinkedList<>();
		this.evseIdsToDelete = new LinkedList<>();
	}
}
