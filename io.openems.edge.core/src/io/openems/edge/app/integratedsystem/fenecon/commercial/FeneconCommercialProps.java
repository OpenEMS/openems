package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import io.openems.common.channel.Unit;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.GridCode;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.accordiongroup.AccordionBuilder;
import io.openems.edge.core.appmanager.formly.builder.accordiongroup.AccordionGroupBuilder;
import io.openems.edge.core.appmanager.formly.enums.InputType;

public final class FeneconCommercialProps {

	/**
	 * Creates a {@link AppDef} for input to set the battery Start/Stop target.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> batteryStartStopTarget() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("Battery Start/Stop Target") //
				.setDefaultValue("AUTO") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(List.of("START", "AUTO"));
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)));
	}

	/**
	 * Creates a {@link AppDef} for a hidden input for a number.
	 *
	 * @param translationKeyLabel       the translationKey for the label
	 * @param translationKeyDescription the translationKey for the description
	 * @param translationLabelParameter the possible parameter values of the
	 *                                  translationKey label, possible null
	 * @param unit                      the unit
	 * @param min                       the min value
	 * @param max                       the max value
	 * @param steps                     the interval between numbers, if null there
	 *                                  is none
	 * @param <N>                       Number
	 * @return the {@link AppDef}
	 */
	public static <N extends Number> AppDef<OpenemsApp, Nameable, BundleProvider> hiddenInput(//
			String translationKeyLabel, //
			String translationKeyDescription, //
			String translationLabelParameter, //
			Unit unit, //
			Integer min, //
			Integer max, //
			Double steps //
	) {
		var appDef = AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setAutoGenerateField(false) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> { //
					field.setInputType(InputType.NUMBER) //
							.setUnit(unit, l);

					if (min != null) {
						field.setMin(min);
					}
					if (max != null) {
						field.setMax(max);
					}
					if (steps != null) {
						field.setStep(steps);
					}
				}));

		if (translationLabelParameter == null) {
			appDef.setTranslatedLabel(translationKeyLabel);
		} else {
			appDef.setTranslatedLabel(translationKeyLabel, translationLabelParameter);
		}

		if (min == null || max == null || unit == Unit.NONE) {
			appDef.setTranslatedDescription(translationKeyDescription);
		} else {
			appDef.setTranslatedDescription(translationKeyDescription, min.toString(), max.toString(), unit.symbol);
		}

		return appDef;

	}

	/**
	 * Creates a {@link AppDef} for a hidden select.
	 *
	 * @param translationKeyLabel       the translationKey for the label
	 * @param translationKeyDescription the tranlsationKey for the description
	 * @param defaultValue              the default value
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> hiddenCheckbox(//
			String translationKeyLabel, //
			String translationKeyDescription, //
			boolean defaultValue //
	) {
		var appDef = AppDef.copyOfGeneric(defaultDef(), //
				def -> def //
						.setTranslatedLabel(translationKeyLabel) //
						.setDefaultValue(defaultValue) //
						.setAutoGenerateField(false)) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable);
		return addDescriptionIfNotNull(appDef, translationKeyDescription);
	}

	/**
	 * Creates a {@link AppDef} for a hidden Select.
	 *
	 * @param translationKeyLabel       the translationKey for the label
	 * @param translationKeyDescription the translationKey for the description, can
	 *                                  be null
	 * @param defaultValue              the default value
	 * @param optionsFactory            the {@link OptionsFactory} of the enum
	 * @return the link {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> hiddenSelect(//
			String translationKeyLabel, //
			String translationKeyDescription, //
			String defaultValue, //
			OptionsFactory optionsFactory) {
		var appDef = AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel(translationKeyLabel) //
				.setDefaultValue(defaultValue) //
				.setAutoGenerateField(false) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(optionsFactory, l);
				}));
		return addDescriptionIfNotNull(appDef, translationKeyDescription);
	}

	/**
	 * Creates a {@link AppDef} for an accordion.
	 *
	 * @param translationKeyLabel the translationKey for the label
	 * @param consumer            consumer for addition settings
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> accordion(//
			String translationKeyLabel, //
			AppDef.FieldValuesConsumer<OpenemsApp, Nameable, BundleProvider, AccordionBuilder> consumer) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel(translationKeyLabel) //
				.setAutoGenerateField(false) //
				.setField(JsonFormlyUtil::buildAccordionFromNameable, consumer));
	}

	/**
	 * Creates a {@link AppDef} for an accordion group.
	 * 
	 * @param consumer consumer for addition settings
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> accordionGroup(
			AppDef.FieldValuesConsumer<OpenemsApp, Nameable, BundleProvider, AccordionGroupBuilder> consumer) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setField(JsonFormlyUtil::buildAccordionGroupFromNameable, consumer));
	}

	/**
	 * Creates an {@link AppDef} for an Accordion Group with the extended settings
	 * of VDE 4110.
	 * 
	 * @param gridCode the GRID_CODE
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> vde4110Settings(Nameable gridCode) {
		return accordionGroup((app, property, language, parameter, field) -> {
			var accordions = GoodWePropertiesConfig.getAccordionsOnTopLevel();

			var defs = FeneconCommercialProps.getExtendedGoodWeProperties();
			var jsonAccordions = JsonUtils.buildJsonArray();
			accordions.forEach((accordion) -> {
				var def = defs.get(accordion.name());
				if (def != null) {
					jsonAccordions.add(def //
							.getField() //
							.get(app, Nameable.of(accordion.name()), language, parameter) //
							.build());
				}

			});
			field.setFieldGroup(jsonAccordions.build()) //
					.setMulti(false) //
					.onlyShowIf(Exp.currentModelValue(gridCode).equal(Exp.staticValue(GridCode.VDE_4110))) //
					.hideKey();

		});
	}

	/**
	 * Gets alle the properties of the
	 * io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverter in accordions.
	 *
	 * @return a {@link TreeMap} with the property name and the associated AppDef
	 */
	public static HashMap<String, AppDef<OpenemsApp, Nameable, BundleProvider>> getExtendedGoodWeProperties() {
		final var goodWeDefs = new HashMap<String, AppDef<OpenemsApp, Nameable, BundleProvider>>();
		final var properties = GoodWePropertiesConfig.getProperties();
		final var rpmAccordions = GoodWePropertiesConfig.getRpmAccordions();
		final var otherAccordions = GoodWePropertiesConfig.getAccordionsOnTopLevel();
		final var accordionGroups = GoodWePropertiesConfig.getAccordionGroups();

		properties.forEach((prop) -> goodWeDefs.put(prop.name(), prop.getAppDef()));
		rpmAccordions.forEach((accordion) -> goodWeDefs.put(accordion.name(), accordion.getAppDef(goodWeDefs)));
		accordionGroups.forEach((group) -> goodWeDefs.put(group.name(), group.getAppDef(goodWeDefs)));
		otherAccordions.forEach((accordion) -> goodWeDefs.put(accordion.name(), accordion.getAppDef(goodWeDefs)));

		return goodWeDefs;
	}

	private static final AppDef<OpenemsApp, Nameable, BundleProvider> addDescriptionIfNotNull(//
			AppDef<OpenemsApp, Nameable, BundleProvider> appDef, //
			String translationKeyDescription //
	) {
		if (translationKeyDescription != null) {
			return appDef.setTranslatedDescription(translationKeyDescription);
		}
		return appDef;
	}

	private FeneconCommercialProps() {
	}
}
