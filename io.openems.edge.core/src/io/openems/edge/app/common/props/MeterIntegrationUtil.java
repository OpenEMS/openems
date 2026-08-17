package io.openems.edge.app.common.props;

import static io.openems.edge.app.common.components.CommonComponents.externMeter;
import static io.openems.edge.app.common.components.CommonComponents.internMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.isHardwareInstalledForMasterBox;
import static io.openems.edge.core.appmanager.AbstractOpenemsApp.getTranslationBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.types.MeterType;
import io.openems.common.utils.StringUtils;
import io.openems.edge.app.enums.MeterIntegration;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.enums.Operator;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.meter.api.ElectricityMeter;

public final class MeterIntegrationUtil {

	private static final String INTERNAL_METER_MODBUS_ID = "modbus0";
	private static final int INTERNAL_METER_MODBUS_UNIT_ID = 3;

	private MeterIntegrationUtil() {
	}

	/**
	 * Checks if the element is measured and the measurement is set to extern.
	 * 
	 * @param isElementMeasured the property that indicates if the element is
	 *                          measured
	 * @param howMeasured       the property that indicates how the element is
	 *                          measured
	 * @return a BooleanExpression that evaluates to true if the element is measured
	 *         and the measurement is set to extern
	 */
	public static BooleanExpression checkMeasuredAndExtern(//
			Nameable isElementMeasured, //
			Nameable howMeasured //
	) {
		BooleanExpression condition1 = Exp.currentModelValue(isElementMeasured).notNull();
		BooleanExpression condition2 = BooleanExpression.of(Exp.currentModelValue(howMeasured), Operator.EQ,
				Exp.staticValue(MeterIntegration.EXTERN));
		return condition1.and(condition2);
	}

	/**
	 * Checks if the intern meter is used by any component except the core
	 * components.
	 * 
	 * @param componentUtil  the ComponentUtil
	 * @param deviceHardware the {@link OpenemsAppInstance} device hardware instance
	 * @return true if the intern meter is used by any component except the core
	 *         components, false otherwise
	 */
	public static boolean isInternMeterUsedByComponent(//
			ComponentUtil componentUtil, //
			OpenemsAppInstance deviceHardware //
	) {
		final String meterId;
		if (isHardwareInstalledForMasterBox(deviceHardware)) {
			meterId = getMasterboxMeterId(componentUtil);
		} else {
			meterId = getMeterIdFromModbusConfig(componentUtil);
		}

		if (meterId == null) {
			return false;
		}
		var ignoreIds = new ArrayList<>(ComponentUtil.CORE_COMPONENT_IDS);

		return meterUsed(componentUtil, meterId, ignoreIds);
	}

	private static String getMeterIdFromModbusConfig(ComponentUtil componentUtil) {
		var meterList = componentUtil.getEnabledComponentsOfType(ElectricityMeter.class);
		return meterList.stream() //
				.filter(meter -> {
					var props = meter.getComponentContext().getProperties();
					if (props.get("modbus.id") != null && props.get("modbusUnitId") != null) {
						return props.get("modbus.id").equals(INTERNAL_METER_MODBUS_ID)
								&& props.get("modbusUnitId").equals(INTERNAL_METER_MODBUS_UNIT_ID);
					}
					return false;
				}) //
				.map(OpenemsComponent::id) //
				.findAny() //
				.orElse(null);
	}

	private static String getMasterboxMeterId(ComponentUtil componentUtil) {
		return Objects.requireNonNull(componentUtil.getComponent("meter1", "Fenecon.MasterBox2V0.Meter").orElse(null))
				.getId();
	}

	/**
	 * Gets the next available meter id. If the modbus config of the intern meter is
	 * already used, it returns the corresponding meter id. Otherwise, it generates
	 * a new meter id with the prefix "meter" and a number that is not used by any
	 * existing meter.
	 * 
	 * @param app   the app
	 * @param <APP> the type of the app, which must implement OpenemsApp,
	 *              ComponentUtilSupplier and ComponentManagerSupplier
	 * @return the meter id corresponding to the modbus config of the intern meter
	 *         if it exists, otherwise a new meter id
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> String getNextMeterId(//
			APP app //
	) {
		var meterId = getMeterIdFromModbusConfig(app.getComponentUtil());
		if (meterId != null) {
			return meterId;
		}
		return app.getComponentUtil().getNextAvailableId("meter", app.getComponentManager().getAllComponents().stream()
				.map(OpenemsComponent::id).filter(id -> id.startsWith("meter")).toList());
	}

	/**
	 * Gets the default value for the extern meter. It checks if there is an
	 * available extern meter that is not used by any component except the core
	 * components. If there is such a meter, it returns its id as a JsonPrimitive.
	 * Otherwise, it returns JsonNull.
	 * 
	 * @param app   the app
	 * @param l     the language
	 * @param <APP> the type of the app, which must implement OpenemsApp,
	 *              ComponentUtilSupplier and ComponentManagerSupplier
	 * @return the id of an available extern meter as a JsonPrimitive, or JsonNull
	 *         if there is no such meter
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> JsonElement getExternDefaultValue(//
			APP app, //
			Language l //
	) {
		var bundle = getTranslationBundle(l);

		var meterList = getExternMeter(app,
				Arrays.asList(
						getMeterIdFromAlias(app.getComponentUtil(),
								TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.emergencyMeter.alias")),
						getMeterIdFromAlias(app.getComponentUtil(),
								TranslationUtil.getTranslation(bundle, "internalMeterAlias"))));
		var meter = meterList.stream() //
				.filter(Objects::nonNull)
				.filter(m -> !meterUsed(app.getComponentUtil(), m.id(),
						new ArrayList<>(ComponentUtil.CORE_COMPONENT_IDS))) //
				.findFirst();
		return meter.isPresent() ? new JsonPrimitive(meter.get().id()) : JsonNull.INSTANCE;
	}

	/**
	 * Gets the title expression for the meter. If the meter is used by any
	 * component except the core components, it shows a warning message with the
	 * name of one of the components that uses the meter. Otherwise, it shows only
	 * the meter id and alias.
	 * 
	 * @param componentUtil the ComponentUtil
	 * @param l             the language
	 * @param meter         the meter to get the title expression for
	 * @param ignoreIds     a list of component ids to ignore when checking if the
	 *                      meter is used, this is used to avoid showing the warning
	 *                      message for the component that is currently being
	 *                      configured
	 * @return a StringExpression that shows the meter id and alias, and if the
	 *         meter is used by any component except the core components, it also
	 *         shows a warning message with the name of one of the components that
	 *         uses the meter
	 */
	public static StringExpression getTitleExpression(//
			ComponentUtil componentUtil, //
			Language l, //
			ElectricityMeter meter, //
			List<String> ignoreIds //
	) {

		ignoreIds.add(meter.id());
		var componentUsing = componentUtil.getComponentUsing(meter.id(), ignoreIds).stream().findFirst();
		String showingString = "";
		if (componentUsing.isPresent()) {
			var componentString = componentUsing.get().alias().isEmpty() ? componentUsing.get().id()
					: componentUsing.get().alias();
			showingString = "\\'" + componentString + "\\'";
		}
		String display = meter.id();
		if (!meter.alias().isEmpty()) {
			display += " - " + meter.alias();
		}
		StringExpression used = StringExpression.of(display + " - "
				+ TranslationUtil.getTranslation(getTranslationBundle(l), "meterAlreadyUsed", showingString));
		StringExpression notUsed = StringExpression.of(display);
		if (meterUsed(componentUtil, meter.id(), ignoreIds)) {
			BooleanExpression exp = isMeterNotFromCurrentApp(meter);
			return Exp.ifElse(exp, used, notUsed);
		}
		return notUsed;
	}

	/**
	 * Checks if the meter is used by any component except the ones with the ids in
	 * the ignoreIds list. This is used to check if the meter is used by any
	 * component except the core components and the component that is currently
	 * being configured.
	 * 
	 * @param componentUtil the ComponentUtil
	 * @param meterId       the id of the meter to check
	 * @param ignoreIds     a list of component ids to ignore when checking if the
	 *                      meter is used
	 * @return true if the meter is used by any component except the ones with the
	 *         ids in the ignoreIds list, false otherwise
	 */
	public static boolean meterUsed(//
			ComponentUtil componentUtil, //
			String meterId, //
			List<String> ignoreIds //
	) {
		return componentUtil.anyComponentUses(meterId, Stream.concat(ignoreIds.stream(), Stream.of(meterId)).toList());
	}

	/**
	 * Gets a sorted list of extern meters that are valid consumption meters.
	 * 
	 * @param app                  the app
	 * @param meterIdsToNotInclude a list of meter ids to not include in the result,
	 *                             this is used to avoid showing meters like the
	 *                             internal meter and the emergency meter
	 * @param <APP>                the type of the app, which must implement
	 *                             OpenemsApp, ComponentUtilSupplier and
	 *                             ComponentManagerSupplier
	 * @return a sorted list of extern meters that are valid consumption meters
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> List<ElectricityMeter> getExternMeter(//
			APP app, //
			List<String> meterIdsToNotInclude //
	) {
		var components = app.getComponentUtil().getEnabledComponentsOfType(ElectricityMeter.class).stream()
				.filter(meter -> isValidConsumptionMeter(app.getComponentManager(), meter, meterIdsToNotInclude))
				.sorted((Comparator.comparingInt(meter -> StringUtils.parseNumberFromName(meter.id()).orElse(0))));

		return components.toList();
	}

	/**
	 * Gets the meter id corresponding to the given alias. If there is no meter with
	 * the given alias, it returns null.
	 * 
	 * @param componentUtil the ComponentUtil
	 * @param meterAlias    the alias of the meter to get the id for
	 * @return the meter id corresponding to the given alias, or an empty string if
	 *         there is no meter with the given alias
	 */
	public static String getMeterIdFromAlias(//
			ComponentUtil componentUtil, //
			String meterAlias //
	) {
		Optional<ElectricityMeter> optionalMeter = findMeterId(componentUtil, meterAlias);
		return optionalMeter.map(OpenemsComponent::id).orElse(null);
	}

	/**
	 * Checks if the meter is not from the current app.
	 * 
	 * @param meter the meter to check
	 * @return a BooleanExpression that evaluates to true if the meter is not from
	 *         the current app
	 */
	public static BooleanExpression isMeterNotFromCurrentApp(ElectricityMeter meter) {
		return Exp.initialModelValue(Nameable.of("METER_ID")).notEqual(Exp.staticValue(meter.id()));
	}

	/**
	 * Resolves the intern meter dependency for the given app. If the intern meter
	 * is used by any component, it throws an exception. Otherwise, it adds the
	 * intern meter dependency to the dependencies list and returns the meter id.
	 * 
	 * @param app            the app
	 * @param l              the language
	 * @param t              the configuration target
	 * @param deviceHardware the {@link OpenemsAppInstance} device hardware instance
	 * @param dependencies   the list of dependencies to add the intern meter
	 *                       dependency
	 * @param <APP>          the type of the app, which must implement OpenemsApp,
	 *                       ComponentUtilSupplier and ComponentManagerSupplier
	 * @return the meter id of the intern meter
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> String resolveInternMeterDependencyAndGetMeterId(//
			APP app, //
			Language l, //
			ConfigurationTarget t, //
			OpenemsAppInstance deviceHardware, //
			List<DependencyDeclaration> dependencies //
	) throws OpenemsError.OpenemsNamedException {

		if (MeterIntegrationUtil.isInternMeterUsedByComponent(app.getComponentUtil(), deviceHardware) //
				&& t.isAddOrUpdate()) {
			throw new OpenemsException("Intern meter already in use");
		}

		String meterId = isHardwareInstalledForMasterBox(deviceHardware) ? getMasterboxMeterId(app.getComponentUtil())
				: MeterIntegrationUtil.getNextMeterId(app);

		dependencies.add(internMeter(l, meterId));
		return meterId;
	}

	/**
	 * Resolves the extern meter dependency for the given meter id. If there is an
	 * app that has a component with the given meter id, it adds the corresponding
	 * extern meter dependency to the dependencies list.
	 * 
	 * @param app     the app
	 * @param meterId the id of the meter to resolve the dependency for
	 * @param <APP>   the type of the app, which must implement OpenemsApp,
	 *                ComponentUtilSupplier and ComponentManagerSupplier
	 * @return the {@link DependencyDeclaration} of the extern meter dependency
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> DependencyDeclaration retrieveExternMeterDependency(//
			APP app, //
			String meterId //
	) throws OpenemsException {
		final var appIdOfMeter = DependencyUtil.getInstanceIdOfAppWhichHasComponent(app.getComponentManager(), meterId);

		if (appIdOfMeter == null) {
			throw new OpenemsException("No app found with component id " + meterId);
		}
		return externMeter(appIdOfMeter);
	}

	private static boolean isValidConsumptionMeter(//
			ComponentManager componentManager, //
			ElectricityMeter meter, //
			List<String> meterIdsToNotInclude //
	) {

		final var edge = componentManager.getEdgeConfig();
		final var meterComponent = edge.getComponents().get(meter.id());
		if (meterComponent == null) {
			return false;
		}
		final var factoryId = meterComponent.getFactoryId();
		final var natureIds = edge.getFactories().get(factoryId);
		var isChargingOrHeating = natureIds != null && Arrays.stream(natureIds.getNatureIds())
				.anyMatch(natureId -> natureId.equals("io.openems.edge.evcs.api.Evcs")
						|| natureId.equals("io.openems.edge.heat.api.Heat")
						|| natureId.equals("io.openems.edge.evse.api.Chargepoint")
						|| natureId.equals("io.openems.edge.meter.api.SinglePhaseMeter"));

		var toIgnore = meterIdsToNotInclude.stream() //
				.filter(Objects::nonNull) //
				.anyMatch(m -> meter.id().equals(m));
		return meter.getMeterType() == MeterType.CONSUMPTION_METERED && !isChargingOrHeating && !toIgnore;
	}

	private static Optional<ElectricityMeter> findMeterId(//
			ComponentUtil componentUtil, //
			String meterAlias //
	) {
		var meterList = componentUtil.getEnabledComponentsOfType(ElectricityMeter.class);
		return meterList.stream().filter(m -> m.alias().equals(meterAlias)).findFirst();
	}
}
