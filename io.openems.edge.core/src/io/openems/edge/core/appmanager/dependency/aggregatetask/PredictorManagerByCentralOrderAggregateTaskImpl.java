package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.Predictor;

@Component(//
		service = { //
				AggregateTask.class, //
				PredictorManagerByCentralOrderAggregateTask.class, //
				PredictorManagerByCentralOrderAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class PredictorManagerByCentralOrderAggregateTaskImpl implements PredictorManagerByCentralOrderAggregateTask {

	private record PredictorManagerByCentralOrderExecutionConfiguration(//
			List<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent> componentsToAdd, //
			List<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent> componentsToRemove //
	) implements AggregateTask.AggregateTaskExecutionConfiguration {

		private PredictorManagerByCentralOrderExecutionConfiguration {
			Objects.requireNonNull(componentsToAdd);
			Objects.requireNonNull(componentsToRemove);
		}

		@Override
		public String identifier() {
			return "PredictorManagerByCentralOrder";
		}

		@Override
		public JsonElement toJson() {
			if (this.componentsToAdd.isEmpty() && this.componentsToRemove.isEmpty()) {
				return JsonNull.INSTANCE;
			}
			return JsonUtils.buildJsonObject() //
					.add("componentsToAdd", this.componentsToAdd.stream() //
							.map(t -> {
								return JsonUtils.buildJsonObject() //
										.addProperty("factoryId", t.factoryId()) //
										.addProperty("componentId", t.componentId()) //
										.build();
							}) //
							.collect(toJsonArray()))
					.add("componentsToRemove", this.componentsToRemove.stream() //
							.map(t -> {
								return JsonUtils.buildJsonObject() //
										.addProperty("factoryId", t.factoryId()) //
										.addProperty("componentId", t.componentId()) //
										.build();
							}) //
							.collect(toJsonArray()))
					.build();
		}

	}

	private final PredefinedOrder<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent> order = new PredefinedOrder<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent>()
			.thenBy(t -> t.factoryId().equals("Predictor.ProfileClusteringModel")) //
			.thenBy(t -> t.factoryId().equals("Predictor.Production.LinearModel")) //
			.thenBy(t -> t.factoryId().equals("Predictor.PersistenceModel"));

	private final ComponentManager componentManager;
	private final AppManagerUtil appManagerUtil;

	@Activate
	public PredictorManagerByCentralOrderAggregateTaskImpl(//
			@Reference ComponentManager componentManager, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		this.componentManager = componentManager;
		this.appManagerUtil = appManagerUtil;
	}

	private List<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent> componentsToAdd;
	private List<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent> componentsToRemove;

	@Override
	public void aggregate(PredictorManagerByCentralOrderConfiguration currentConfiguration,
			PredictorManagerByCentralOrderConfiguration lastConfiguration) {
		if (currentConfiguration != null) {
			this.componentsToAdd.addAll(currentConfiguration.components());
		}
		if (lastConfiguration != null) {
			var schedulerIdDiff = new ArrayList<>(lastConfiguration.components());
			if (currentConfiguration != null) {
				schedulerIdDiff.removeAll(currentConfiguration.components());
			}
			this.componentsToRemove.addAll(schedulerIdDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations)
			throws OpenemsError.OpenemsNamedException {
		if (!this.anyCreateChanges()) {
			this.delete(user, otherAppConfigurations);
			return;
		}

		if (this.componentsToAdd.stream().anyMatch(Predicate.not(this.order::contains))) {
			throw new OpenemsException("Some PredictorManager components are not known.");
		}

		final var otherIds = new ArrayList<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent>();
		for (var appConfig : otherAppConfigurations) {
			final var predictorManagerConfig = appConfig
					.getConfiguration(PredictorManagerByCentralOrderAggregateTask.class);
			if (predictorManagerConfig == null) {
				continue;
			}

			otherIds.addAll(predictorManagerConfig.components());
		}

		if (otherIds.stream().anyMatch(Predicate.not(this.order::contains))) {
			throw new OpenemsException("Some PredictorManager components are not known.");
		}

		final var handledIds = new TreeSet<>(this.order);
		handledIds.addAll(this.componentsToAdd);
		handledIds.addAll(otherIds);

		var predictorManagerIds = this.getPredictorManagerIds();

		final var finalOrder = new ArrayList<>(predictorManagerIds);
		for (final var schedulerComponent : handledIds) {
			final var lower = handledIds.lower(schedulerComponent);
			final var higher = handledIds.higher(schedulerComponent);
			final var iLower = lower == null ? -1 : finalOrder.indexOf(lower.componentId());
			final var i = finalOrder.indexOf(schedulerComponent.componentId());
			final var iHigher = higher == null ? -1 : finalOrder.indexOf(higher.componentId());

			if (i != -1 //
					&& (iLower == -1 || iLower < i) //
					&& (iHigher == -1 || iHigher > i)) {
				// already correctly inserted
				continue;
			}

			final var insertIndex = Math.max(iLower + 1, iHigher);
			finalOrder.add(insertIndex, schedulerComponent.componentId());
			if (i != -1) {
				finalOrder.remove(Math.max(0, insertIndex >= i ? i : (i - 1)));
			}
		}

		this.setPredictorManagerIds(user, finalOrder);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations)
			throws OpenemsError.OpenemsNamedException {
		if (!this.anyDeleteChanges()) {
			return;
		}

		final var otherIds = otherAppConfigurations.stream() //
				.map(appConfig -> appConfig.getConfiguration(PredictorManagerByCentralOrderAggregateTask.class)) //
				.filter(Objects::nonNull) //
				.flatMap(t -> t.components().stream()) //
				.toList();

		final var removeComponents = new ArrayList<>(this.componentsToRemove);
		removeComponents.removeAll(otherIds);

		final var predictorManagerIds = this.getPredictorManagerIds();

		final var finalOrder = new ArrayList<>(predictorManagerIds);
		finalOrder.removeIf(t -> removeComponents.stream().anyMatch(c -> c.componentId().equals(t)));

		this.setPredictorManagerIds(user, finalOrder);
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new PredictorManagerByCentralOrderExecutionConfiguration(this.componentsToAdd, this.componentsToRemove);
	}

	@Override
	public void validate(//
			final List<String> errors, //
			final AppConfiguration appConfiguration, //
			final PredictorManagerByCentralOrderConfiguration configuration, //
			final Map<OpenemsAppInstance, AppConfiguration> allConfigurations //
	) {
		if (configuration.components().isEmpty()) {
			return;
		}

		final List<String> existingOrder;
		try {
			existingOrder = this.getPredictorManagerIds();
		} catch (OpenemsError.OpenemsNamedException e) {
			e.printStackTrace();
			errors.add("Could not get existing PredictorManager configuration: " + e.getMessage());
			return;
		}

		final var predictor = this.componentManager.getEnabledComponentsOfType(Predictor.class);
		if (predictor.size() <= 1) {
			return;
		}

		final var expectedOrder = new TreeSet<>(this.order);
		for (final var componentId : existingOrder) {
			final var instances = this.getAppsWithPredictorManagerComponent(componentId);

			if (instances.size() != 1) {
				// not created with app
				continue;
			}
			final var entry = instances.entrySet().iterator().next();
			final var schedulerComponents = entry.getValue();
			if (schedulerComponents.isEmpty()) {
				continue;
			}
			if (schedulerComponents.size() > 1) {
				errors.add("Multiple PredictorManagerComponents found with the same id");
				continue;
			}
			final var schedulerComponent = schedulerComponents.getFirst();
			expectedOrder.add(new PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent(componentId,
					schedulerComponent.factoryId()));
		}

		for (final var schedulerComponent : configuration.components()) {
			final var higher = expectedOrder.higher(schedulerComponent);
			final var lower = expectedOrder.lower(schedulerComponent);
			if (!expectedOrder.add(schedulerComponent)) {
				// check if id was configured between the expected ids
				final var highIndex = higher == null ? -1 : existingOrder.indexOf(higher.componentId());
				final var index = existingOrder.indexOf(schedulerComponent.componentId());
				final var lowIndex = lower == null ? -1 : existingOrder.indexOf(lower.componentId());

				if ((highIndex == -1 || highIndex > index) //
						&& (lowIndex != -1 || lowIndex < index)) {
					continue;
				}
			}

			errors.add("PredictorManager Expected '" //
					+ schedulerComponent.componentId() //
					+ "' to be configured between '" //
					+ (lower == null ? "TOP" : lower.componentId()) //
					+ "' and '" //
					+ (higher == null ? "BOTTOM" : higher.componentId()) //
					+ "'");
		}

	}

	@Override
	public String getGeneralFailMessage(Language l) {
		return "Update of PredictorManager configuration failed.";
	}

	@Override
	public void reset() {
		this.componentsToAdd = new ArrayList<>();
		this.componentsToRemove = new ArrayList<>();
	}

	@Override
	public AggregateTaskExecuteConstraints getExecuteConstraints() {
		return new AggregateTaskExecuteConstraints(Set.of(//
				// Needs to run after the AggregateTask.ComponentAggregateTask to also remove
				// ids in the scheduler of components which got deleted
				ComponentAggregateTask.class //
		));
	}

	private boolean anyCreateChanges() {
		return !this.componentsToAdd.isEmpty();
	}

	private boolean anyDeleteChanges() {
		return !this.componentsToRemove.isEmpty();
	}

	private List<String> getPredictorManagerIds() throws OpenemsError.OpenemsNamedException {

		var predictorManager = this.componentManager.getEdgeConfig()
				.getComponent(PredictorManager.SINGLETON_COMPONENT_ID)
				.orElseThrow(() -> new OpenemsException("Unable to find PredictorManager!"));

		final var ids = predictorManager.getProperties().get("predictor.ids");

		if (ids == null) {
			return emptyList();
		}

		final var array = JsonUtils.getAsJsonArray(ids);
		return List.of(JsonUtils.getAsStringArray(array));
	}

	private void setPredictorManagerIds(User user, List<String> ids) throws OpenemsError.OpenemsNamedException {
		var jsonIds = ids.stream().map(JsonPrimitive::new).collect(toJsonArray());
		final var request = new UpdateComponentConfig.Request(PredictorManager.SINGLETON_COMPONENT_ID, List.of(//
				new UpdateComponentConfigRequest.Property("predictor.ids", jsonIds) //
		));

		this.componentManager.handleUpdateComponentConfigRequest(user, request);
	}

	private Map<OpenemsAppInstance, List<PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent>> getAppsWithPredictorManagerComponent(
			String componentId) {
		return this.appManagerUtil.getInstantiatedApps().stream() //
		.<Map.Entry<OpenemsAppInstance, PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent>>mapMulti(
				(t, c) -> {
					try {
						final var configuration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE,
								t, Language.DEFAULT);

						final var predictorManagerConfig = configuration
								.getConfiguration(PredictorManagerByCentralOrderAggregateTask.class);
						if (predictorManagerConfig == null) {
							return;
						}
						for (var predictorManagerComponent : predictorManagerConfig.components()) {
							if (predictorManagerComponent.componentId().equals(componentId)) {
								c.accept(Map.entry(t, predictorManagerComponent));
							}
						}
					} catch (OpenemsError.OpenemsNamedException e) {
						// can not get app configuration
					}
				}) //
				.collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
	}

}
