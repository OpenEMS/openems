package io.openems.edge.app.common.props;

import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_VALUE;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

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
	AppDef<APP, Nameable, Parameter.BundleParameter> pickComponentId() {
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
	 * @param <PA>  the type of the parameters
	 * @param type  the type of the {@link OpenemsComponent OpenemsComponents}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, T extends OpenemsComponent, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickComponentId(//
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
	 * @param <PA>   the type of the parameters
	 * @param type   the type of the {@link OpenemsComponent OpenemsComponents}
	 * @param filter the filter of the components
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, T extends OpenemsComponent, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickComponentId(//
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

	private static <APP extends OpenemsApp, PA extends BundleProvider> AppDef<APP, Nameable, PA> pickComponentId(//
			final Function<APP, List<? extends OpenemsComponent>> supplyComponents //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def.setLabel("Component-ID") //
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
	 * @param <PA>       the type of the parameters
	 * @param startingId the starting id of the components e. g. evcs for all evcss:
	 *                   evcs0, evcs1, ...
	 * @param filter     the filter to apply on the component list
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickComponentId(//
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
	 * @param <PA>       the type of the parameters
	 * @param startingId the starting id of the components e. g. evcs for all evcss:
	 *                   evcs0, evcs1, ...
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickComponentId(//
			String startingId //
	) {
		return pickComponentId(startingId, null);
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link ManagedSymmetricEss}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickManagedSymmetricEssId() {
		return ComponentProps.<APP, ManagedSymmetricEss, PA>pickComponentId(ManagedSymmetricEss.class) //
				.setTranslatedLabel("essId.label") //
				.setTranslatedDescription("essId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select an {@link ElectricityMeter}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickElectricityMeterId() {
		return ComponentProps.<APP, ElectricityMeter, PA>pickComponentId(ElectricityMeter.class) //
				.setTranslatedLabel("meterId.label") //
				.setTranslatedDescription("meterId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select an {@link ElectricityMeter}
	 * with the {@link MeterType} {@link MeterType#GRID}.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier, PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickElectricityGridMeterId() {
		return ComponentProps
				.<APP, ElectricityMeter, PA>pickComponentId(ElectricityMeter.class,
						meter -> meter.getMeterType() == MeterType.GRID) //
				.setTranslatedLabel("gridMeterId.label") //
				.setTranslatedDescription("gridMeterId.description");
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus'.
	 * 
	 * @param <APP>  the type of the {@link OpenemsApp}
	 * @param <PA>   the type of the parameters
	 * @param filter the filter to apply on the component list
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier, //
			PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickModbusId(//
			final Predicate<OpenemsComponent> filter //
	) {
		return AppDef.copyOfGeneric(ComponentProps.pickComponentId("modbus", filter), def -> {
			def.setTranslatedLabel("communication.modbusId") //
					.setTranslatedDescription("communication.modbusId.description");
			final var oldDefaultValue = def.getDefaultValue();
			def.setDefaultValue((app, property, l, parameter) -> {
				if (PropsUtil.isHomeInstalled(app.getAppManagerUtil())) {
					return new JsonPrimitive("modbus1");
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
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier, //
			PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickModbusId() {
		return pickModbusId(null);
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus' and the factoryId 'Bridge.Modbus.Serial'.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier, //
			PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickSerialModbusId() {
		return pickModbusId(c -> c.serviceFactoryPid().equals("Bridge.Modbus.Serial"));
	}

	/**
	 * Creates a {@link AppDef} for a input to select a {@link OpenemsComponent}
	 * with the starting id 'modbus' and the factoryId 'Bridge.Modbus.Tcp'.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @param <PA>  the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & AppManagerUtilSupplier, //
			PA extends BundleProvider> //
	AppDef<APP, Nameable, PA> pickTcpModbusId() {
		return pickModbusId(c -> c.serviceFactoryPid().equals("Bridge.Modbus.Tcp"));
	}

	private ComponentProps() {
	}

}
