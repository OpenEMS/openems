package io.openems.edge.app.api;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder.SelectOptionExpressions;

public final class ModbusApiProps {

	/**
	 * Creates a {@link AppDef} to select {@link ModbusSlave} Components for a
	 * ModbusApi.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> pickModbusIds() {
		return AppDef.copyOfGeneric(ComponentProps.pickOrderedArrayIds(ModbusSlave.class, component -> {
			if ("_meta".equals(component.id())) {
				return false;
			}
			return true;
		}, (app, property, l, parameter, component) -> {
			if ("_sum".equals(component.id())) {
				final var lockedExpression = Exp.currentModelValue(property).asArray() //
						.elementAt(0).equal(Exp.staticValue(component.id()));
				return new SelectOptionExpressions(lockedExpression);
			}
			return null;
		}, List.of((app, property, language, parameter) -> {
			return JsonFormlyUtil.buildText() //
					.setText(TranslationUtil.getTranslation(parameter.bundle(), "App.Api.Modbus.changeComponentHint"));
		})), def -> def //
				.setTranslatedLabel("component.id.plural") //
		);
	}

	/**
	 * Creates a {@link AppDef} to select {@link ModbusSlave} Components for a
	 * ModbusApi.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> apiTimeout() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.apiTimeout.label") //
				.setTranslatedDescription("App.Api.apiTimeout.description") //
				.setDefaultValue(60) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setMin(0);
				})); //
	}

	/**
	 * Creates a {@link AppDef} to select Port Name for ModbusRTU Api.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> portName() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.ModbusRtu.portName.label") //
				.setTranslatedDescription("App.Api.ModbusRtu.portName.description") //
				.setDefaultValue((app, property, l, parameter) -> {
					SerialPort[] ports = SerialPort.getCommPorts();
					if (ports.length > 0) {
						return new JsonPrimitive(ports[0].getSystemPortName());
					}
					return JsonNull.INSTANCE;
				}).setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					SerialPort[] ports = SerialPort.getCommPorts();
					var portNames = new ArrayList<String>();
					for (var port : ports) {
						portNames.add(port.getSystemPortName());
					}
					field.setOptions(portNames);
				})); //
	}

	/**
	 * Creates a {@link AppDef} to select baudrate for ModbusRTU Api.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> baudrate() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.ModbusRtu.baudrate.label") //
				.setTranslatedDescription("App.Api.ModbusRtu.baudrate.description") //
				.setDefaultValue(9600) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setMin(0);
				})); //
	}

	/**
	 * Creates a {@link AppDef} to select Databits for ModbusRTU Api.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> databits() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.ModbusRtu.databits.label") //
				.setTranslatedDescription("App.Api.ModbusRtu.databits.description") //
				.setDefaultValue(8));
	}

	/**
	 * Creates a {@link AppDef} to select Stopbits for ModbusRTU Api.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> stopbits() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.ModbusRtu.stopbits.label") //
				.setTranslatedDescription("App.Api.ModbusRtu.stopbits.description") //
				.setDefaultValue("ONE") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, prop, l, params, field) -> {
					field.setOptions(List.of("ONE", "ONE_POINT_FIVE", "TWO"));
				}));
	}

	/**
	 * Creates a {@link AppDef} to select Parity for ModbusRTU Api.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> parity() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.ModbusRtu.parity.label") //
				.setTranslatedDescription("App.Api.ModbusRtu.parity.description") //
				.setDefaultValue("NONE") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, prop, l, params, field) -> {
					field.setOptions(List.of("NONE", "ODD", "EVEN", "MARK", "SPACE"));
				}));
	}

	/**
	 * Creates a {@link AppDef} to select Component Ids for ModbusApi.
	 * 
	 * @param <APP>         the type of the {@link OpenemsApp}
	 * @param componentName the component name
	 * @param essDefault    should storage systems be added to default
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> AppDef<APP, Nameable, BundleProvider> componentIds(
			Nameable componentName, boolean essDefault) {
		return AppDef.copyOfGeneric(ModbusApiProps.pickModbusIds(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					final var jsonArrayBuilder = JsonUtils.buildJsonArray() //
							.add("_sum");

					if (essDefault) {
						// add ess ids
						app.getComponentUtil().getEnabledComponentsOfStartingId("ess").stream() //
								.sorted((o1, o2) -> o1.id().compareTo(o2.id())) //
								.forEach(ess -> jsonArrayBuilder.add(ess.id()));
					}

					return jsonArrayBuilder.build();
				}) //
				.bidirectional(componentName, "component.ids", ComponentManagerSupplier::getComponentManager) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN))); //

	}

	private ModbusApiProps() {
	}

}
