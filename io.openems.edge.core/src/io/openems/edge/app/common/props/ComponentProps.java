package io.openems.edge.app.common.props;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_VALUE;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDef.FieldValuesFunction;
import io.openems.edge.core.appmanager.AppDef.FieldValuesSupplier;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
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
				if (PropsUtil.isHome20Or30Installed(app.getAppManagerUtil())) {
					return new JsonPrimitive("modbus2");
				}

				return oldDefaultValue.get(app, property, l, parameter);
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
	 * Creates a {@link AppDef} for a input to select component ids with a specific
	 * order. Used for e. g. in ModbusTcpApi's or EVCS Cluster.
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

	private ComponentProps() {
	}

}
