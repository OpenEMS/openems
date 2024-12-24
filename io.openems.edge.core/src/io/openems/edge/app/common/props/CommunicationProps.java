package io.openems.edge.app.common.props;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;
import static io.openems.edge.core.appmanager.formly.enums.Validation.IP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.ModbusType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.HostSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Case;
import io.openems.edge.core.appmanager.formly.DefaultValueOptions;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.core.appmanager.formly.expression.Variable;
import io.openems.edge.core.host.NetworkConfiguration;

public final class CommunicationProps {

	private CommunicationProps() {
	}

	/**
	 * Creates a {@link AppDef} for a {@link ModbusType}.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> modbusType() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("communication.modbusIntegrationType") //
				.setDefaultValue(ModbusType.TCP) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> field
						.setOptions(OptionsFactory.of(ModbusType.class), l)));
	}

	/**
	 * Creates a {@link AppDef} for a ip-address.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> ip() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("communication.ipAddress") //
				.setDefaultValue("192.168.178.85") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setValidation(IP)));
	}

	/**
	 * Creates a {@link AppDef} for a ip-address that excludes all IPs of system
	 * itself.
	 * 
	 * @param <APP>   the type of the app
	 * @param <PROP>  the type of the properties
	 * @param <PARAM> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & ComponentManagerSupplier & HostSupplier, //
			PROP extends Nameable, PARAM extends BundleProvider> //
	AppDef<APP, PROP, PARAM> excludingIp() {
		return AppDef.copyOfGeneric(ip(),
				def -> def.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> {
					try {
						var ips = app.getHost().getSystemIPs();
						final var exclusionPattern = ips.stream().map(ip -> ip.getHostAddress())//
								.map(ip -> ip.replace(".", "\\.")) //
								.collect(Collectors.joining("|"));

						final var regex = "^(?!.*(?:" + exclusionPattern + ")$)"
								+ NetworkConfiguration.PATTERN_INET4ADDRESS;
						f.setValidation(regex, TranslationUtil.getTranslation(param.bundle(), "communication.excludingIp"));
					} catch (OpenemsNamedException e) {
						f.setValidation(IP);
					}
				}));
	}

	/**
	 * Creates a {@link AppDef} for a port.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> port() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("communication.port") //
				.setTranslatedDescription("communication.port.description") //
				.setDefaultValue(502) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(NUMBER) //
						.setMin(0)));
	}

	/**
	 * Creates a {@link AppDef} for a modbusUnitId.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> modbusUnitId() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("communication.modbusUnitId") //
				.setTranslatedDescription("communication.modbusUnitId.description") //
				.setDefaultValue(0) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(NUMBER) //
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
			PROP extends Nameable, PARAM extends BundleProvider> //
	AppDef<APP, PROP, PARAM> modbusGroup(//
			PROP modbusId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusIdDef, //
			PROP modbusUnitId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusUnitIdDef //
	) {
		return modbusGroup(modbusId, modbusIdDef, modbusUnitId, modbusUnitIdDef, null);
	}

	/**
	 * Creates a {@link AppDef} group of a {@link ComponentProps#pickModbusId()} and
	 * a {@link CommunicationProps#modbusUnitId()} to check if the current selected
	 * modbus unit id already got selected.
	 * 
	 * @param <APP>                the type of the app
	 * @param <PROP>               the type of the properties
	 * @param <PARAM>              the type of the parameters
	 * @param modbusId             the {@link Nameable} of the modbus id
	 * @param modbusIdDef          the {@link AppDef} of the modbus id
	 * @param modbusUnitId         the {@link Nameable} of the modbus unit id
	 * @param modbusUnitIdDef      the {@link AppDef} of the modbus unit id
	 * @param connectionModubsType if set add a default value of 1 if
	 *                             {@link ModbusType#TCP} is selected
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & ComponentManagerSupplier & ComponentUtilSupplier, //
			PROP extends Nameable, PARAM extends BundleProvider> //
	AppDef<APP, PROP, PARAM> modbusGroup(//
			PROP modbusId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusIdDef, //
			PROP modbusUnitId, //
			AppDef<? super APP, ? super PROP, ? super PARAM> modbusUnitIdDef, //
			PROP connectionModubsType //
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

					final var usedIdStrings = Arrays.stream(alreadyUsedIds) //
							.distinct() //
							.sorted() //
							.mapToObj(Exp::staticValue) //
							.toArray(Variable[]::new);

					// checks if the current modbus component is selected
					final var expression = Exp.currentModelValue(modbusId).notEqual(Exp.staticValue(componentId)) //
							// checks if the current selected unit id is not the initial value
							.or(Exp.initialModelValue(modbusUnitId) //
									.equal(Exp.currentValue(modbusUnitId))) //
							// checks if the current selected unit id is not already used
							.or(Exp.array(usedIdStrings).every(v -> v.notEqual(Exp.currentModelValue(modbusUnitId))));

					final var filteredArray = Exp.array(usedIdStrings) //
							.filter(v -> v.notEqual(Exp.initialModelValue(modbusUnitId)) //
									.or(Exp.initialModelValue(modbusId) //
											.notEqual(Exp.currentModelValue(modbusId))));

					final var message = Exp.ifElse(filteredArray.length().equal(Exp.staticValue(1)), //
							StringExpression.of(TranslationUtil.getTranslation(parameter.bundle(),
									"communication.modbusUnitId.alreadTaken.singular",
									filteredArray.join(", ").insideTranslation(), componentId)), //
							StringExpression.of(TranslationUtil.getTranslation(parameter.bundle(),
									"communication.modbusUnitId.alreadTaken.plural",
									filteredArray.join(", ").insideTranslation(), componentId)));

					field.setCustomValidation(componentId, expression, message, modbusUnitId);

					cases.add(new Case(new JsonPrimitive(componentId),
							new JsonPrimitive(getFirstPossibleValue(defaultValue, alreadyUsedIds))));
				}

				final var modbusIdFieldBuilder = modbusIdDef.getField().get(app, modbusId, l, parameter);
				final var overridenDefaultForModbusId = cases.stream() //
						.filter(c -> c.value().equals(modbusIdFieldBuilder.getDefaultValue())) //
						.findFirst() //
						.map(Case::defaultValue) //
						.orElse(new JsonPrimitive(defaultValue));

				field.setFieldGroup(JsonUtils.buildJsonArray() //
						.add(modbusIdFieldBuilder.build())
						.add(modbusUnitIdDef.getField().get(app, modbusUnitId, l, parameter) //
								.onlyIf(!cases.isEmpty(), b -> {
									if (connectionModubsType != null) {
										b.setDefaultValueCases(
												new DefaultValueOptions(modbusId, cases.toArray(Case[]::new)),
												new DefaultValueOptions(connectionModubsType,
														new Case(new JsonPrimitive(ModbusType.TCP.name()),
																new JsonPrimitive(1))));
									} else {
										b.setDefaultValueCases(
												new DefaultValueOptions(modbusId, cases.toArray(Case[]::new)));
									}
								}) //
								.setDefaultValue(overridenDefaultForModbusId) //
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
