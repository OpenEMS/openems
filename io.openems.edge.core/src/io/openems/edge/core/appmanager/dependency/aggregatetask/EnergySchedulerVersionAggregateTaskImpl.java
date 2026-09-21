package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;

@Component(//
		service = { //
				AggregateTask.class, //
				EnergySchedulerVersionAggregateTask.class, //
				EnergySchedulerVersionAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class EnergySchedulerVersionAggregateTaskImpl implements EnergySchedulerVersionAggregateTask {

	private final ComponentManager componentManager;

	@Activate
	public EnergySchedulerVersionAggregateTaskImpl(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	private final Set<Version> requiredVersions = EnumSet.noneOf(Version.class);

	@Override
	public void aggregate(EnergySchedulerVersionConfiguration current, EnergySchedulerVersionConfiguration old) {
		if (current != null) {
			this.requiredVersions.add(current.version());
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		final var requiredVersion = getHighestRequiredVersion(this.requiredVersions, otherAppConfigurations);
		if (requiredVersion == null) {
			return;
		}

		this.applyVersion(user, requiredVersion);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		// reverting is not possible
	}

	static Version getHighestRequiredVersion(Set<Version> requiredVersions,
			List<AppConfiguration> otherAppConfigurations) {
		final var allRequiredVersions = EnumSet.noneOf(Version.class);
		allRequiredVersions.addAll(requiredVersions);

		for (var appConfig : otherAppConfigurations) {
			final var schedulerVersionConfig = appConfig.getConfiguration(EnergySchedulerVersionAggregateTask.class);
			if (schedulerVersionConfig != null) {
				allRequiredVersions.add(schedulerVersionConfig.version());
			}
		}

		return allRequiredVersions.stream().max(Version::compareTo).orElse(null);
	}

	private void applyVersion(User user, Version version) throws OpenemsNamedException {
		if (version == this.getCurrentVersion()) {
			return;
		}

		var property = new Property("version", version.name());
		var request = new UpdateComponentConfig.Request(EnergyScheduler.SINGLETON_COMPONENT_ID, List.of(property));
		this.componentManager.handleUpdateComponentConfigRequest(user, request);
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new EnergySchedulerVersionExecutionConfiguration(this.requiredVersions.stream() //
				.max(Version::compareTo) //
				.orElse(null));
	}

	private record EnergySchedulerVersionExecutionConfiguration(//
			Version targetVersion //
	) implements AggregateTask.AggregateTaskExecutionConfiguration {

		@Override
		public String identifier() {
			return "EnergySchedulerVersion";
		}

		@Override
		public JsonElement toJson() {
			if (this.targetVersion == null) {
				return JsonNull.INSTANCE;
			}
			return JsonUtils.buildJsonObject().addProperty("targetVersion", this.targetVersion.name()).build();
		}
	}

	@Override
	public void validate(//
			List<String> errors, //
			AppConfiguration appConfiguration, //
			EnergySchedulerVersionConfiguration config, //
			Map<OpenemsAppInstance, AppConfiguration> allConfigurations //
	) {
		if (config == null) {
			return;
		}

		final var currentVersion = this.getCurrentVersion();
		if (currentVersion == null) {
			return;
		}
		if (config.version() == currentVersion) {
			return;
		}

		errors.add("Expected EnergyScheduler version '" + config.version().name() + "' but was '"
				+ currentVersion.name() + "'");
	}

	private Version getCurrentVersion() {
		return this.componentManager.getEdgeConfig() //
				.getComponent(EnergyScheduler.SINGLETON_COMPONENT_ID) //
				.map(component -> component.getProperties().get("version")) //
				.filter(JsonElement::isJsonPrimitive) //
				.map(JsonElement::getAsString) //
				.map(versionName -> {
					try {
						return Version.valueOf(versionName);
					} catch (IllegalArgumentException e) {
						return null;
					}
				}) //
				.orElse(null);
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateEnergySchedulerVersion");
	}

	@Override
	public void reset() {
		this.requiredVersions.clear();
	}
}
