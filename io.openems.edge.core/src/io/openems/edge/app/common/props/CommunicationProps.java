package io.openems.edge.app.common.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.Case;
import io.openems.edge.core.appmanager.JsonFormlyUtil.DefaultValueOptions;
import io.openems.edge.core.appmanager.JsonFormlyUtil.ExpressionBuilder;
import io.openems.edge.core.appmanager.JsonFormlyUtil.ExpressionBuilder.Operator;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public final class CommunicationProps {

	private CommunicationProps() {
	}

	/**
	 * Creates a {@link AppDef} for a ip-address.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> ip() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("ipAddress") //
						.setDefaultValue("192.168.178.85") //
						.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
						f.setValidation(JsonFormlyUtil.InputBuilder.Validation.IP)));
	}

	/**
	 * Creates a {@link AppDef} for a port.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> port() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("port") //
						.setTranslatedDescription("port.description") //
						.setDefaultValue(502) //
						.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
						f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
								.setMin(0)));
	}

	/**
	 * Creates a {@link AppDef} for a modbusUnitId.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> modbusUnitId() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("modbusUnitId") //
						.setTranslatedDescription("modbusUnitId.description") //
						.setDefaultValue(0) //
						.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
						f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
								.setMin(0) //
								.onlyPositiveNumbers()));
	}

	/**
	 * Creates a {@link AppDef} group of a {@link ComponentProps#pickModbusId()} and
	 * a {@link CommunicationProps#modbusUnitId()} to check if the current selected
	 * modbus unit id already got selected.
	 * 
	 * @param <APP>           the type of the app
	 * @param <PROP>          the type of the properties
	 * @param <PARAM>         the type of the parameters
	 * @param modbusId        the {@link Nameable} of the modbus id
	 * @param modbusIdDef     the {@link AppDef} of the modbus id
	 * @param modbusUnitId    the {@link Nameable} of the modbus unit id
	 * @param modbusUnitIdDef the {@link AppDef} of the modbus unit id
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & ComponentManagerSupplier & ComponentUtilSupplier, //
			PROP extends Nameable, PARAM extends BundleParameter> //
	AppDef<APP, PROP, PARAM> modbusGroup(//
			PROP modbusId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusIdDef, //
			PROP modbusUnitId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusUnitIdDef //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
				final var componentManager = app.getComponentManager();
				final var componentUtil = app.getComponentUtil();

				final var components = componentManager.getEdgeConfig()
						.getComponentIdsByFactory("Bridge.Modbus.Serial");

				final var cases = new ArrayList<Case>();

				final var defaultValue = Optional.ofNullable(modbusUnitIdDef.getDefaultValue()) //
						.map(f -> f.get(app, modbusUnitId, l, parameter)) //
						.flatMap(JsonUtils::getAsOptionalInt) //
						.orElse(0);

				for (var componentId : components) {
					final var alreadyUsedIds = componentUtil.getUsedModbusUnitIds(componentId);
					if (alreadyUsedIds.length == 0) {
						continue;
					}

					final var strings = Arrays.stream(alreadyUsedIds) //
							.distinct() //
							.sorted() //
							.mapToObj(Integer::toString) //
							.toArray(String[]::new);
					final var expression = ExpressionBuilder.of(modbusId, Operator.NEQ, componentId) //
							.or(ExpressionBuilder.ofNotIn(modbusUnitId, strings).inBrackets());

					final var collectedStrings = Arrays.stream(strings).collect(Collectors.joining(", "));
					final String errorMessage;
					if (strings.length == 1) {
						errorMessage = TranslationUtil.getTranslation(parameter.getBundle(),
								"modbusUnitId.alreadTaken.singular", collectedStrings, componentId);
					} else {
						errorMessage = TranslationUtil.getTranslation(parameter.getBundle(),
								"modbusUnitId.alreadTaken.plural", collectedStrings, componentId);
					}
					field.setCustomValidation(componentId, expression, errorMessage, modbusUnitId);

					cases.add(new Case(new JsonPrimitive(componentId),
							new JsonPrimitive(getFirstPossibleValue(defaultValue, alreadyUsedIds))));
				}

				field.setFieldGroup(JsonUtils.buildJsonArray() //
						.add(modbusIdDef.getField().get(app, modbusId, l, parameter).build())
						.add(modbusUnitIdDef.getField().get(app, modbusUnitId, l, parameter) //
								.onlyIf(!cases.isEmpty(),
										b -> b.setDefaultValueCases(
												new DefaultValueOptions(modbusId, cases.toArray(Case[]::new)))) //
								.build())
						.build()) //
						.hideKey();
			});
		});
	}

	private static int getFirstPossibleValue(int start, int[] skipValues) {
		for (int j = 0; j < skipValues.length; j++) {
			if (start == skipValues[j]) {
				return getFirstPossibleValue(start + 1, skipValues);
			}
		}
		return start;
	}

}
