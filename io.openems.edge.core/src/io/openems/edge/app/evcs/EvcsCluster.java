package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_VALUE;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.evcs.EvcsCluster.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a evcs cluster.
 *
 * <pre>
  {
    "appId":"App.Evcs.Cluster",
    "alias":"Multiladepunkt-Management",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_CLUSTER_ID": "evcsCluster0",
      "EVCS_IDS": [ "evcs0", "evcs1", ...]
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Evcs.Cluster")
public class EvcsCluster extends AbstractOpenemsAppWithProps<EvcsCluster, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, EvcsCluster, BundleParameter>, Nameable {
		// Component-IDs
		EVCS_CLUSTER_ID(AppDef.of(EvcsCluster.class) //
				.setDefaultValue("evcsCluster0")), //
		// Properties
		ALIAS(alias()), //
		EVCS_IDS(AppDef.of(EvcsCluster.class) //
				.setTranslatedLabelWithAppPrefix(".evcsIds.label") //
				.setTranslatedDescriptionWithAppPrefix(".evcsIds.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelect, (app, prop, l, param, f) -> {
					f.setOptions(
							app.getComponentUtil().getEnabledComponentsOfStartingId("evcs").stream()
									.filter(t -> !t.id().startsWith("evcsCluster")).toList(),
							DEFAULT_COMPONENT_2_LABEL, DEFAULT_COMPONENT_2_VALUE) //
							.isMulti(true);
				}) //
				.setDefaultValue((app, property, l, parameter) -> new JsonArray()) //
				.bidirectional(EVCS_CLUSTER_ID, "evcs.ids", a -> a.componentManager)), //
		MAX_HARDWARE_POWER_LIMIT_PER_PHASE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".maxChargeFromGrid.short.label") //
				.setTranslatedDescriptionWithAppPrefix(".maxChargeFromGrid.description") //
				.setDefaultValue(7000) //
				.setRequired(true) //
				.appendIsAllowedToEdit(AppDef.ofLeastRole(Role.INSTALLER)) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> field.setInputType(NUMBER) //
						.setMin(0) //
						.setUnit(Unit.WATT, l)) //
				.bidirectional(EVCS_CLUSTER_ID, "hardwarePowerLimitPerPhase",
						ComponentManagerSupplier::getComponentManager, AppDef.multiplyWith(3)))), //
		;

		private final AppDef<? super EvcsCluster, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super EvcsCluster, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super EvcsCluster, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<EvcsCluster>, BundleParameter> getParamter() {
			return BundleParameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public EvcsCluster(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var evcsClusterId = this.getId(t, p, Property.EVCS_CLUSTER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var ids = this.getJsonArray(p, Property.EVCS_IDS);
			final var hardwarePowerLimitPerPhase = this.getInt(p, Property.MAX_HARDWARE_POWER_LIMIT_PER_PHASE);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsClusterId, alias, "Evcs.Cluster.PeakShaving",
							JsonUtils.buildJsonObject() //
									.add("evcs.ids", ids) //
									.addProperty("hardwarePowerLimitPerPhase",
											hardwarePowerLimitPerPhase / EvcsProps.NUMBER_OF_PHASES) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected EvcsCluster getApp() {
		return this;
	}

	/**
	 * Creates a dependency for a {@link EvcsCluster}.
	 * 
	 * @param target                     the target of the configuration
	 * @param cm                         the {@link ComponentManager}
	 * @param componentUtil              the {@link ComponentUtil}
	 * @param hardwarePowerLimitPerPhase the
	 *                                   {@link Property#MAX_HARDWARE_POWER_LIMIT_PER_PHASE}
	 * @param addedEvcsIdsArray          the ids of the evcss which got added
	 * @return a {@link List} of the {@link DependencyDeclaration}
	 * @throws OpenemsNamedException on error
	 */
	public static List<DependencyDeclaration> dependency(//
			final ConfigurationTarget target, //
			final ComponentManager cm, //
			final ComponentUtil componentUtil, //
			final OptionalInt hardwarePowerLimitPerPhase, //
			final String... addedEvcsIdsArray //
	) throws OpenemsNamedException {
		return dependency(target, cm, componentUtil, hardwarePowerLimitPerPhase, Collections.emptyList(),
				addedEvcsIdsArray);
	}

	/**
	 * Creates a dependency for a {@link EvcsCluster}.
	 * 
	 * @param target                     the target of the configuration
	 * @param cm                         the {@link ComponentManager}
	 * @param componentUtil              the {@link ComponentUtil}
	 * @param hardwarePowerLimitPerPhase the
	 *                                   {@link Property#MAX_HARDWARE_POWER_LIMIT_PER_PHASE}
	 * @param removeEvcsIds              the evcs ids which got removed
	 * @param addedEvcsIdsArray          the ids of the evcss which got added
	 * @return a {@link List} of the {@link DependencyDeclaration}
	 * @throws OpenemsNamedException on error
	 */
	public static List<DependencyDeclaration> dependency(//
			final ConfigurationTarget target, //
			final ComponentManager cm, //
			final ComponentUtil componentUtil, //
			final OptionalInt hardwarePowerLimitPerPhase, //
			final List<String> removeEvcsIds, //
			final String... addedEvcsIdsArray //
	) throws OpenemsNamedException {
		final var clusterComponents = new ArrayList<OpenemsComponent>();
		// this is currently not type safe so for example any component can have the id
		// evcs% but in order to get the type in a test we would have to instantiate a
		// evcs component and because the interface is in a different package it would
		// be a bad practice to have from the core package a dependency to the interface
		// package and there is currently no way to set the natures of a
		// OpenemsComponent for a test component.
		final var evcsComponents = componentUtil.getEnabledComponentsOfStartingId("evcs") //
				.stream().filter(t -> {
					final var isCluster = t.id().startsWith("evcsCluster");
					if (isCluster) {
						clusterComponents.add(t);
					}
					return !isCluster;
				}).collect(Collectors.toList());

		final var potentialIdsInCluster = new TreeSet<String>();
		potentialIdsInCluster.addAll(evcsComponents.stream() //
				.map(OpenemsComponent::id).toList());
		if (target == ConfigurationTarget.DELETE) {
			potentialIdsInCluster.removeAll(Arrays.asList(addedEvcsIdsArray));
		} else {
			potentialIdsInCluster.addAll(Arrays.asList(addedEvcsIdsArray));
		}
		potentialIdsInCluster.removeAll(removeEvcsIds);

		final var shouldDependencyExist = potentialIdsInCluster.size() > 1;

		// still add dependency when deleting the app so that it will also be deleted
		if (target != ConfigurationTarget.DELETE && !shouldDependencyExist) {
			return Collections.emptyList();
		}

		// if there is exactly one cluster component overwrite its configuration
		final var clusterId = clusterComponents.size() == 1 ? clusterComponents.get(0).id() : "evcsCluster0";

		final var existingEvcsIds = cm.getEdgeConfig().getComponent(clusterId) //
				.flatMap(t -> t.getProperty("evcs.ids")) //
				.flatMap(JsonUtils::getAsOptionalJsonArray) //
				.orElse(new JsonArray());

		// create dependency
		final var existingCluster = DependencyUtil.getAppWhichHasComponent(cm, clusterId);

		final var dependencyBuilder = AppDependencyConfig.create();
		if (existingCluster != null) {
			dependencyBuilder.setSpecificInstanceId(existingCluster.instanceId);
		} else {
			dependencyBuilder.setAppId("App.Evcs.Cluster");
		}
		final var newEvcsIds = JsonUtils.stream(existingEvcsIds) //
				.map(t -> {
					if (!t.isJsonPrimitive()) {
						return null;
					}
					if (!t.getAsJsonPrimitive().isString()) {
						return null;
					}
					return t.getAsString();
				}) //
				.filter(Objects::nonNull) //
				.collect(Collectors.toList());

		final var evcsIds = evcsComponents.stream() //
				.map(OpenemsComponent::id) //
				.collect(Collectors.toList());
		final var addedEvcsIds = Arrays.stream(addedEvcsIdsArray) //
				.collect(Collectors.toList());
		if (target != ConfigurationTarget.DELETE) {
			evcsIds.addAll(addedEvcsIds);
		}
		for (var id : evcsIds) {
			if (newEvcsIds.stream().anyMatch(t -> t.equals(id))) {
				continue;
			}
			newEvcsIds.add(id);
		}
		if (target == ConfigurationTarget.DELETE) {
			newEvcsIds.removeAll(addedEvcsIds);
		}

		newEvcsIds.removeAll(removeEvcsIds);

		dependencyBuilder.setProperties(JsonUtils
				.buildJsonObject(existingCluster != null ? existingCluster.properties.deepCopy() : new JsonObject()) //
				.add(Property.EVCS_IDS.name(), newEvcsIds.stream() //
						.map(JsonPrimitive::new) //
						.collect(JsonUtils.toJsonArray())) //
				.addProperty(Property.EVCS_CLUSTER_ID.name(), clusterId) //
				.onlyIf(hardwarePowerLimitPerPhase.isPresent(),
						c -> c.addProperty(Property.MAX_HARDWARE_POWER_LIMIT_PER_PHASE.name(),
								hardwarePowerLimitPerPhase.getAsInt())) //
				.build());

		return Lists.newArrayList(new DependencyDeclaration("CLUSTER", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				shouldDependencyExist ? DependencyDeclaration.DeletePolicy.IF_MINE
						: DependencyDeclaration.DeletePolicy.ALWAYS, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				dependencyBuilder.build()));
	}

}
