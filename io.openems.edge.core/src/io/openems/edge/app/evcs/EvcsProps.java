package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.enums.EMobilityArchitectureType;
import io.openems.edge.app.enums.KebaHardwareType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.EMobilityApp;
import io.openems.edge.core.appmanager.MetaSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.FieldGroupBuilder;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;
import io.openems.edge.meter.api.PhaseRotation;

public final class EvcsProps {

	public static final int NUMBER_OF_PHASES = 3;

	private EvcsProps() {
	}

	/**
	 * Creates a {@link AppDef} for configuring the reaad only of a evcs app.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> readOnly() {
		return AppDef.copyOfGeneric(defaultDef())//
				.setTranslatedLabel("App.Evcs.readOnly.label") //
				.setTranslatedDescription("App.Evcs.readOnly.description") //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setDefaultValue(false);
	}

	/**
	 * Creates a {@link AppDef} for selecting the number of charge points.
	 * 
	 * @param maxValue the max number of charge points
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> numberOfChargePoints(//
			final int maxValue //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evcs.numberOfChargingStations.label") //
				.setDefaultValue(1) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> //
				field.setOptions(IntStream.rangeClosed(1, maxValue) //
						.<Integer>mapToObj(value -> value) //
						.toList(), JsonPrimitive::new, JsonPrimitive::new)));
	}

	private static <T extends OpenemsApp & MetaSupplier> void field(//
			T app, //
			Nameable property, //
			Nameable acceptProperty, //
			Language language, //
			BundleProvider parameter, //
			FieldGroupBuilder field //
	) {
		final var gridConnectionPointFuseLimit = app.getMeta().getGridConnectionPointFuseLimit();

		field.hideKey();
		field.setPopupInput(property, DisplayType.NUMBER);
		field.setFieldGroup(JsonUtils.buildJsonArray() //
				.add(JsonFormlyUtil.buildText() //
						.setText(TranslationUtil.getTranslation(parameter.bundle(), //
								"App.Evcs.Cluster.maxGrid.text1"))
						.build())
				.add(JsonFormlyUtil.buildText() //
						.setText(TranslationUtil.getTranslation(parameter.bundle(), //
								"App.Evcs.Cluster.maxGrid.text2"))
						.build())
				.add(JsonFormlyUtil.buildText() //
						.setText(TranslationUtil.getTranslation(parameter.bundle(), //
								"App.Evcs.Cluster.maxGrid.text3"))
						.build())
				.add(JsonFormlyUtil.buildInputFromNameable(property) //
						.setLabel(TranslationUtil.getTranslation(parameter.bundle(),
								"App.Evcs.Cluster.maxChargeFromGrid.short.label"))
						.setInputType(NUMBER) //
						.setMin(0) //
						.setMax(gridConnectionPointFuseLimit * 230 * 3).isRequired(true) //
						.setDefaultValue(Math.round(gridConnectionPointFuseLimit * 0.9F) * 230 * 3)//
						.setUnit(Unit.WATT, language) //
						.build())
				.build());
	}

	/**
	 * Creates a {@link AppDef} for the
	 * {@link EvcsCluster.Property#MAX_HARDWARE_POWER_LIMIT_PER_PHASE}.
	 * 
	 * @param <T>            the type of the {@link OpenemsApp}
	 * @param acceptProperty the property of the accept field
	 * @return the {@link AppDef}
	 */
	public static <T extends OpenemsApp & ComponentManagerSupplier & MetaSupplier> AppDef<T, Nameable, BundleProvider> clusterMaxHardwarePower(
			Nameable acceptProperty) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evcs.Cluster.maxChargeFromGrid.label") //
				.setAllowedToSave(false) //
				.setIsAllowedToSee((app, property, l, parameter, user) -> {
					final var componentManager = app.getComponentManager();
					if (isClusterInstalled(componentManager)) {
						return false;
					}
					return true;
				}) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter,
						field) -> field(app, property, acceptProperty, l, parameter, field)));
	}

	/**
	 * Creates a {@link AppDef} for the
	 * {@link EvcsCluster.Property#MAX_HARDWARE_POWER_LIMIT_PER_PHASE} for a single
	 * charge point.
	 * 
	 * @param <T>            the type of the {@link OpenemsApp}
	 * @param acceptProperty the property of the accept field
	 * @param evcsIdProperty the property of the evcs id
	 * @return the {@link AppDef}
	 */
	public static <T extends OpenemsApp & ComponentManagerSupplier & ComponentUtilSupplier & MetaSupplier> AppDef<T, Nameable, BundleProvider> clusterMaxHardwarePowerSingleCp(
			Nameable acceptProperty, //
			Nameable evcsIdProperty //
	) {
		return EvcsProps.<T>clusterMaxHardwarePower(acceptProperty) //
				.setIsAllowedToSee((app, property, l, parameter, user) -> {
					final var componentManager = app.getComponentManager();
					if (isClusterInstalled(componentManager)) {
						return false;
					}
					final var existingEvcs = getEvcsComponents(app.getComponentUtil());
					return !existingEvcs.isEmpty();
				}).wrapField((app, property, l, parameter, field) -> {
					final var existingEvcs = EvcsProps.getEvcsComponents(app.getComponentUtil());
					if (existingEvcs.isEmpty()) {
						return;
					}

					final var expression = existingEvcs.stream().map(OpenemsComponent::id) //
							.map(Exp::staticValue) //
							.collect(Exp.toArrayExpression()) //
							.every(v -> v.notEqual(Exp.currentModelValue(evcsIdProperty)));

					field.onlyShowIf(expression);
				});
	}

	/**
	 * Gets the currently installed evcs components.
	 * 
	 * <p>
	 * Note: only checks if the component id starts with evcs it does not check the
	 * type of the component.
	 * 
	 * @param componentUtil the {@link ComponentUtil}
	 * @return a list of the components
	 */
	public static List<OpenemsComponent> getEvcsComponents(ComponentUtil componentUtil) {
		return componentUtil.getEnabledComponentsOfStartingId("evcs") //
				.stream().filter(t -> !t.id().startsWith("evcsCluster")).toList();
	}

	private static final boolean isClusterInstalled(ComponentManager componentManager) {
		try {
			AppManagerImpl appManager = componentManager.getComponent(AppManager.SINGLETON_COMPONENT_ID);
			if (appManager.getInstantiatedApps().stream() //
					.anyMatch(t -> t.appId.equals("App.Evcs.Cluster"))) {
				return true;
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates a {@link AppDef} for a {@link PhaseRotation}.
	 *
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> phaseRotation() {
		return AppDef.copyOfGeneric(
				CommonProps.phaseRotation().setTranslatedDescription("App.Evcs.phaseRotation.description")); //
	}

	/**
	 * Creates a {@link AppDef} for a {@link KebaHardwareType}.
	 * 
	 * @param evcsId {@link Nameable} of evcs id
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hardwareType(Nameable evcsId) {
		return AppDef.copyOfGeneric(defaultDef())//
				.setTranslatedLabel("App.Evcs.Keba.hardwareType.label")
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(KebaHardwareType.class), l);
				})//
				.wrapField((app, property, l, parameter, field) -> {
					field.readonlyIf(Exp.currentModelValue(evcsId).notNull());
				})//
				.setRequired(true) //
				.setDefaultValue(KebaHardwareType.P30);
	}

	/**
	 * Creates a {@link AppDef} for a {@link EMobilityArchitectureType}.
	 * 
	 * @param evcsId {@link Nameable} of evcs id
	 * @param <T>    type of app
	 * @return the {@link AppDef}
	 */
	public static <T extends OpenemsApp & AppManagerUtilSupplier & EMobilityApp> AppDef<T, Nameable, BundleProvider> architectureType(
			Nameable evcsId) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.Evcs.Keba.architectureType.label")//
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.readonlyIf(Exp.currentModelValue(evcsId).notNull());
					var availableArchitectureTypes = getAvailableArchitectureTypes(app.supportedArchitectureTypes(),
							getExistingArchitectureType(app));
					field.setOptions(
							OptionsFactory.of(availableArchitectureTypes.toArray(EMobilityArchitectureType[]::new)), l);
				})//
				.setRequired(true)//
				.setDefaultValue((app, property, l, parameter) -> {
					var supportedArchitectureTypes = app.supportedArchitectureTypes();
					return new JsonPrimitive(
							resolveArchitectureTypeDefault(supportedArchitectureTypes, getExistingArchitectureType(app))
									.name());
				}));
	}

	private static <T extends OpenemsApp & AppManagerUtilSupplier> EMobilityArchitectureType getExistingArchitectureType(
			T app) {
		var appManagerUtil = app.getAppManagerUtil();
		if (appManagerUtil == null) {
			return null;
		}
		var apps = appManagerUtil.getInstantiatedAppsByCategories(OpenemsAppCategory.EVCS);
		if (apps == null) {
			return null;
		}

		final var serializer = JsonSerializerUtil.enumSerializerFromObjectNullable("ARCHITECTURE_TYPE",
				EMobilityArchitectureType.class);
		final var configuredArchitectureType = apps.stream() //
				.map(instance -> serializer.deserializeNullable(instance.properties)) //
				.filter(Objects::nonNull) //
				.findFirst() //
				.orElse(null);
		if (configuredArchitectureType != null) {
			return configuredArchitectureType;
		}

		return apps.stream() //
				.map(instance -> {
					final var installedApp = appManagerUtil.findAppById(instance.appId).orElse(null);
					if (!(installedApp instanceof EMobilityApp eMobilityApp)) {
						return null;
					}

					final var supportedArchitectureTypes = eMobilityApp.supportedArchitectureTypes();
					return supportedArchitectureTypes.size() == 1 ? supportedArchitectureTypes.getFirst() : null;
				}) //
				.filter(Objects::nonNull) //
				.findFirst() //
				.orElse(null);
	}

	private static List<EMobilityArchitectureType> getAvailableArchitectureTypes(
			List<EMobilityArchitectureType> supportedArchitectureTypes,
			EMobilityArchitectureType existingArchitectureType) {
		if (existingArchitectureType == null) {
			return supportedArchitectureTypes;
		}
		if (supportedArchitectureTypes.contains(existingArchitectureType)) {
			return List.of(existingArchitectureType);
		}
		return List.of();
	}

	private static EMobilityArchitectureType resolveArchitectureTypeDefault(
			List<EMobilityArchitectureType> supportedArchitectureTypes,
			EMobilityArchitectureType existingArchitectureType) {
		if (supportedArchitectureTypes.contains(EMobilityArchitectureType.EVCS)
				&& supportedArchitectureTypes.contains(EMobilityArchitectureType.EVSE)) {
			return existingArchitectureType != null ? existingArchitectureType : EMobilityArchitectureType.EVCS;
		}
		if (supportedArchitectureTypes.contains(EMobilityArchitectureType.EVCS)) {
			return EMobilityArchitectureType.EVCS;
		}
		if (supportedArchitectureTypes.contains(EMobilityArchitectureType.EVSE)) {
			return EMobilityArchitectureType.EVSE;
		}
		return EMobilityArchitectureType.EVCS;
	}
}
