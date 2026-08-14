package io.openems.edge.app.common.props;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.getExternMeter;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.getMeterIdFromAlias;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.isMeterNotFromCurrentApp;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.meterUsed;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.isHardwareInstalledForMasterBox;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_VALUE;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.Option.buildOption;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.OptionGroup.buildOptionGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.MeterIntegration;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDef.FieldValuesFunction;
import io.openems.edge.core.appmanager.AppDef.FieldValuesSupplier;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.FormlyBuilder;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder.SelectOption;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder.SelectOptionExpressions;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Static method collection for {@link AppDef AppDefs} for selecting different
 * kinds of {@link OpenemsComponent OpenemsComponents}.
 */
public final class ComponentProps {

	/**
	 * Creates a {@link AppDef} for a input to select a enabled
	 * {@link OpenemsComponent}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentManagerSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickComponentId() {
		return pickComponentId(app -> {
			final var componentManager = app.getComponentManager();
			return componentManager.getEnabledComponents();
		});
	}

	/**
	 * Creates a {@link AppDef} for a input to select a enabled
	 * {@link OpenemsComponent} of the given type.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <T>   the type of the component
	 * @param type  the type of the {@link OpenemsComponent OpenemsComponents}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, T extends OpenemsComponent> //
			AppDef<APP, Nameable, BundleProvider> pickComponentId(//
					final Class<T> type //
	) {
		return pickComponentId(type, null);
	}

	/**
	 * Creates a {@link AppDef} for a input to select a enabled
	 * {@link OpenemsComponent} of the given type and filtered by the given filter.
	 * 
	 * @param <APP>  the type of the {@link OpenemsApp}
	 * @param <T>    the type of the component
	 * @param type   the type of the {@link OpenemsComponent OpenemsComponents}
	 * @param filter the filter of the components
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, T extends OpenemsComponent> //
			AppDef<APP, Nameable, BundleProvider> pickComponentId(//
					final Class<T> type, //
					final Predicate<T> filter //
	) {
		return pickComponentId(app -> {
			final var componentUtil = app.getComponentUtil();
			var components = componentUtil.getEnabledComponentsOfType(type).stream();
			if (filter != null) {
				components = components.filter(filter);
			}
			return components.toList();
		});
	}

	private static <APP extends OpenemsApp> AppDef<APP, Nameable, BundleProvider> pickComponentId(//
			final Function<APP, List<? extends OpenemsComponent>> supplyComponents //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabel("component.id.singular") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(supplyComponents.apply(app), //
							DEFAULT_COMPONENT_2_LABEL, DEFAULT_COMPONENT_2_VALUE);
				}).setDefaultValue((app, property, l, parameter) -> {
					final var components = supplyComponents.apply(app);
					if (components.isEmpty()) {
						return JsonNull.INSTANCE;
					}
					return new JsonPrimitive(components.get(0).id());
				}));
	}

	/**
	 * Creates a {@link AppDef} for a input to select a enabled
	 * {@link OpenemsComponent} with the given starting id.
	 * 
	 * @param <APP>      the type of the {@link OpenemsApp}
	 * @param startingId the starting id of the components e. g. evcs for all evcss:
	 *                   evcs0, evcs1, ...
	 * @param filter     the filter to apply on the component list
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickComponentId(//
					String startingId, //
					final Predicate<OpenemsComponent> filter //
	) {
		return pickComponentId(app -> {
			final var componentUtil = app.getComponentUtil();
			final var components = componentUtil.getEnabledComponentsOfStartingId(startingId);
			if (filter == null) {
				return components;
			}
			return components.stream() //
					.filter(filter) //
					.toList();
		});
	}

	/**
	 * Creates a {@link AppDef} for a input to select a enabled
	 * {@link OpenemsComponent} with the given starting id.
	 * 
	 * @param <APP>      the type of the {@link OpenemsApp}
	 * @param startingId the starting id of the components e. g. evcs for all evcss:
	 *                   evcs0, evcs1, ...
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickComponentId(//
					String startingId //
	) {
		return pickComponentId(startingId, null);
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link ManagedSymmetricEss}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickManagedSymmetricEssId() {
		return ComponentProps.<APP, ManagedSymmetricEss>pickComponentId(ManagedSymmetricEss.class) //
				.setTranslatedLabel("essId.label") //
				.setTranslatedDescription("essId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select an {@link ElectricityMeter}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickElectricityMeterId() {
		return ComponentProps.<APP, ElectricityMeter>pickComponentId(ElectricityMeter.class) //
				.setTranslatedLabel("meterId.label") //
				.setTranslatedDescription("meterId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select an {@link ElectricityMeter}
	 * with the {@link MeterType} {@link MeterType#CONSUMPTION_METERED} and that are
	 * unused.
	 * 
	 * @param <APP>                the type of the {@link OpenemsApp}
	 * @param ignoreIdsToCheck     a list of the id of a component that should be
	 *                             ignored to check.
	 * @param meterIdsToNotInclude a list of meterIds that shouldn't be included
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickUnusedElectricityConsumptionMeterId(
					Function<APP, List<String>> ignoreIdsToCheck, List<String> meterIdsToNotInclude) {

		return pickComponentId(app -> {
			final var componentUtil = app.getComponentUtil();
			List<String> ignoreIds = ignoreIdsToCheck.apply(app);

			var components = componentUtil.getEnabledComponentsOfType(ElectricityMeter.class).stream().filter(meter -> {
				var toIgnore = meterIdsToNotInclude.stream().anyMatch(m -> meter.id().equals(m));
				return meter.getMeterType() == MeterType.CONSUMPTION_METERED && !toIgnore;
			});

			components = components.filter(meter -> {
				if (!ignoreIds.contains(meter.id())) {
					ignoreIds.add(meter.id());
				}
				return !componentUtil.anyComponentUses(meter.id(), ignoreIds);
			});

			return components.toList();
		});

	}

	/**
	 * Creates a {@link AppDef} for a input to select an {@link ElectricityMeter}
	 * with the {@link MeterType} {@link MeterType#GRID}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickElectricityGridMeterId() {
		return ComponentProps
				.<APP, ElectricityMeter>pickComponentId(ElectricityMeter.class,
						meter -> meter.getMeterType() == MeterType.GRID) //
				.setTranslatedLabel("gridMeterId.label") //
				.setTranslatedDescription("gridMeterId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus'.
	 * 
	 * @param <APP>  the type of the {@link OpenemsApp}
	 * @param filter the filter to apply on the component list
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickModbusId(//
					final Predicate<OpenemsComponent> filter //
	) {
		return AppDef.copyOfGeneric(ComponentProps.pickComponentId("modbus", filter), def -> {
			def.setTranslatedLabel("communication.modbusId") //
					.setTranslatedDescription("communication.modbusId.description");
			final var oldDefaultValue = def.getDefaultValue();
			def.setDefaultValue((app, property, l, parameter) -> {
				// TODO should be configured in oem bundle
				if (PropsUtil.isHome10Installed(app.getAppManagerUtil())) {
					return new JsonPrimitive("modbus1");
				}

				if (PropsUtil.isHome20Or30Installed(app.getAppManagerUtil())
						|| PropsUtil.isHomeGen2Installed(app.getAppManagerUtil())) {
					// external modbus interface
					return new JsonPrimitive("modbus2");
				}

				if (PropsUtil.isCommercial92Installed(app.getAppManagerUtil())) {
					// external modbus interface
					return new JsonPrimitive("modbus3");
				}

				return oldDefaultValue.get(app, property, l, parameter);
			});
			def.wrapField((app, property, l, parameter, field) -> {
				if (PropsUtil.isHomeInstalled(app.getAppManagerUtil())
						|| PropsUtil.isCommercial92Installed(app.getAppManagerUtil())) {
					field.readonly(true);
				}
			});
		});
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus'.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickModbusId() {
		return pickModbusId(null);
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus' and the factoryId 'Bridge.Modbus.Serial'.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickSerialModbusId() {
		return pickModbusId(c -> c.serviceFactoryPid().equals("Bridge.Modbus.Serial"));
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus' and the factoryId 'Bridge.Modbus.Tcp'.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> pickTcpModbusId() {
		return pickModbusId(c -> c.serviceFactoryPid().equals("Bridge.Modbus.Tcp"));
	}

	/**
	 * Creates a {@link AppDef} for a input to select component ids with a specific
	 * order. Used for e. g. in ModbusTcpApi's or EVCS Cluster.
	 * 
	 * @param <APP>                   the type of the {@link OpenemsApp}
	 * @param supplyComponents        the method to get the selectable components
	 *                                from
	 * @param expressionFunction      the function to get the expressions of one
	 *                                {@link SelectOption}
	 * @param additionalFieldSupplier the additional fields which are inserted after
	 *                                the component selection; can be used to
	 *                                display additional information inside the
	 *                                modal
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp> //
			AppDef<APP, Nameable, BundleProvider> pickOrderedArrayIds(//
					final Function<APP, List<? extends OpenemsComponent>> supplyComponents, //
					final FieldValuesFunction<APP, Nameable, BundleProvider, OpenemsComponent, SelectOptionExpressions> expressionFunction, //
					final List<FieldValuesSupplier<APP, Nameable, BundleProvider, FormlyBuilder<?>>> additionalFieldSupplier //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("component.id.plural") //
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.setPopupInput(property, DisplayType.STRING);

					final var arrayBuilder = new ReorderArrayBuilder(property) //
							.setLabel(TranslationUtil.getTranslation(parameter.bundle(),
									"component.addAdditionalComponent"));
					final var components = supplyComponents.apply(app);
					components.stream()//
							.map(c -> new SelectOption(c.alias(), c.id(),
									expressionFunction == null ? null
											: expressionFunction.apply(app, property, l, parameter, c))) //
							.forEach(arrayBuilder::addSelectOption);

					final var fields = JsonUtils.buildJsonArray() //
							.add(arrayBuilder.build());

					additionalFieldSupplier.stream() //
							.map(t -> t.get(app, property, l, parameter)) //
							.map(FormlyBuilder::build) //
							.forEach(fields::add);

					field.setFieldGroup(fields.build());
				}));
	}

	/**
	 * Creates a {@link AppDef} for an input to select component ids with a specific
	 * order. Used for e.g. in ModbusTcpApi's or EVCS Cluster.
	 * 
	 * @param <APP>                   the type of the {@link OpenemsApp}
	 * @param <T>                     the type of the selectable components
	 * @param type                    the class of the selectable components
	 * @param filter                  the filter to apply on the component list
	 * @param expressionFunction      the function to get the expressions of one
	 *                                {@link SelectOption}
	 * @param additionalFieldSupplier the additional fields which are inserted after
	 *                                the component selection; can be used to
	 *                                display additional information inside the
	 *                                modal
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, T extends OpenemsComponent> //
			AppDef<APP, Nameable, BundleProvider> pickOrderedArrayIds(//
					final Class<T> type, //
					final Predicate<T> filter, //
					final FieldValuesFunction<APP, Nameable, BundleProvider, OpenemsComponent, SelectOptionExpressions> expressionFunction, //
					final List<FieldValuesSupplier<APP, Nameable, BundleProvider, FormlyBuilder<?>>> additionalFieldSupplier //
	) {
		return pickOrderedArrayIds(app -> {
			final var componentUtil = app.getComponentUtil();
			var components = componentUtil.getEnabledComponentsOfType(type).stream();
			if (filter != null) {
				components = components.filter(filter);
			}
			return components.toList();
		}, expressionFunction, additionalFieldSupplier);
	}

	/**
	 * Creates a {@link AppDef} for a selection to show if the element is measured
	 * internal or external.
	 * 
	 * @param isElementMeasured the {@link Nameable} IS_ELEMENT_MEASURED
	 * @param <APP>             the type of the app, which must implement *
	 *                          OpenemsApp, ComponentUtilSupplier and *
	 *                          AppManagerUtilSupplier
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier> //
			AppDef<APP, Nameable, BundleProvider> howMeasured(//
					Nameable isElementMeasured //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), de -> de //
				.setTranslatedLabel("howMeasured") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					if (isHomeExceptGen1(app) || isTechbaseGen3AndHomeOrCommercial(app)) {
						field.setOptions(OptionsFactory.of(MeterIntegration.class), l);
					} else {
						field.setOptions(OptionsFactory.of(MeterIntegration.class, MeterIntegration.INTERN), l);
					}
					field.onlyShowIf(Exp.currentModelValue(isElementMeasured).notNull());
				}));
	}

	/**
	 * Creates a {@link AppDef} for a selection of all valid consumption meters if
	 * the element is extern measured.
	 * 
	 * @param isElementMeasured the {@link Nameable} IS_ELEMENT_MEASURED
	 * @param howMeasured       the {@link Nameable} HOW_MEASURED
	 * @param <APP>             the type of the app, which must implement * *
	 *                          OpenemsApp, ComponentUtilSupplier and * *
	 *                          ComponentManagerSupplier
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> //
			AppDef<APP, Nameable, BundleProvider> externMeterIdsForMeterIntegration(//
					Nameable isElementMeasured, //
					Nameable howMeasured //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> MeterIntegrationUtil.getExternDefaultValue(app, l))//
				.setTranslatedLabel("meterId.label")//
				.setTranslatedDescription("meterId.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectGroupFromNameable, (app, property, l, parameter, field) -> {
					List<String> ignoreIds = new ArrayList<>(ComponentUtil.CORE_COMPONENT_IDS);
					field.addOption(buildOptionGroup("Meter",
							translate(parameter.bundle(), "App.Meter.consumptionMeter"))
							.addOptions(getExternMeter(app, Arrays.asList(//
									getMeterIdFromAlias(app.getComponentUtil(),
											TranslationUtil.getTranslation(parameter.bundle(),
													"App.IntegratedSystem.emergencyMeter.alias")),
									getMeterIdFromAlias(app.getComponentUtil(),
											TranslationUtil.getTranslation(parameter.bundle(), "internalMeterAlias")))), //
									meter -> buildOption(meter.id())
											.setTitleExpression(MeterIntegrationUtil
													.getTitleExpression(app.getComponentUtil(), l, meter, ignoreIds))
											.onlyIf(meterUsed(app.getComponentUtil(), meter.id(), ignoreIds),
													b -> b.setDisabledExpression(isMeterNotFromCurrentApp(meter)))
											.build())
							.build());
					field.setMissingOptionsText(translate(parameter.bundle(), "noMeter"));
					field.onlyShowIf(MeterIntegrationUtil.checkMeasuredAndExtern(isElementMeasured, howMeasured))
							.build();
				}));
	}

	private static <APP extends OpenemsApp & AppManagerUtilSupplier> boolean isHomeExceptGen1(APP app) {
		return PropsUtil.isHomeInstalled(app.getAppManagerUtil())
				&& app.getAppManagerUtil().getInstantiatedAppsOf("App.FENECON.Home").isEmpty();
	}

	private static <APP extends OpenemsApp & AppManagerUtilSupplier> boolean isTechbaseGen3AndHomeOrCommercial(
			APP app) {
		final var deviceHardware = app.getAppManagerUtil()
				.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);
		return isHardwareInstalledForMasterBox(deviceHardware)
				&& PropsUtil.isProductTypeWithCompatibleMasterboxInstalled(app.getAppManagerUtil());
	}

	private ComponentProps() {
	}

}
