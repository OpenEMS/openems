package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.FieldGroupBuilder;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;
import io.openems.edge.evcs.api.PhaseRotation;

public final class EvcsProps {

	public static final int NUMBER_OF_PHASES = 3;

	private EvcsProps() {
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

	private static void field(//
			OpenemsApp app, //
			Nameable property, //
			Nameable acceptProperty, //
			Language language, //
			BundleProvider parameter, //
			FieldGroupBuilder field //
	) {
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
				.add(JsonFormlyUtil.buildInputFromNameable(property) //
						.setLabel(TranslationUtil.getTranslation(parameter.bundle(),
								"App.Evcs.Cluster.maxChargeFromGrid.short.label"))
						.setInputType(NUMBER) //
						.setMin(0) //
						.isRequired(true) //
						.setUnit(Unit.WATT, language) //
						.build())
				.add(JsonFormlyUtil.buildText() //
						.setText(TranslationUtil.getTranslation(parameter.bundle(), //
								"App.Evcs.Cluster.maxGrid.text3"))
						.build())
				.add(JsonFormlyUtil.buildCheckboxFromNameable(acceptProperty) //
						.isRequired(true) //
						.requireTrue(language) //
						.setLabel(TranslationUtil.getTranslation(parameter.bundle(), "acceptCondition.label")) //
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
	public static <T extends OpenemsApp & ComponentManagerSupplier> AppDef<T, Nameable, BundleProvider> clusterMaxHardwarePower(
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
	public static <T extends OpenemsApp & ComponentManagerSupplier & ComponentUtilSupplier> AppDef<T, Nameable, BundleProvider> clusterMaxHardwarePowerSingleCp(
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
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evcs.phaseRotation.label") //
				.setTranslatedDescription("App.Evcs.phaseRotation.description") //
				.setDefaultValue(PhaseRotation.L1_L2_L3) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Arrays.stream(PhaseRotation.values()) //
							.map(PhaseRotation::name) //
							.toList());
				}));
	}
}
