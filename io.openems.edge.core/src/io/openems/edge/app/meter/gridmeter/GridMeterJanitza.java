package io.openems.edge.app.meter.gridmeter;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkIndustrialL;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.ModbusType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.meter.JanitzaMeter;
import io.openems.edge.app.meter.MeterProps;
import io.openems.edge.app.meter.gridmeter.GridMeterJanitza.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes an app for a Janitza grid meter.
 *
 * <pre>
 {
 "appId":"App.GridMeter.Janitza",
 "alias":"Janitza Netzzähler",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "METER_ID": "meter1",
 "MODBUS_ID": "modbus2",
 "TYPE": "PRODUCTION",
 "MODEL": "Meter.Janitza.UMG96RME",
 "IP": "10.4.0.12",
 "MODBUS_UNIT_ID": 1
 },
 "appDescriptor": {
 "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 }
 }
 * </pre>
 */
@Component(name = "App.GridMeter.Janitza")
public class GridMeterJanitza extends AbstractOpenemsAppWithProps<GridMeterJanitza, Property, Parameter.BundleParameter>
		implements OpenemsApp, ComponentUtilSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, GridMeterJanitza, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(alias()), //
		MODEL(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Meter.Janitza.productModel") //
				.setDefaultValue(JanitzaMeter.JanitzaModel.UMG_96_RME) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelect, (app, property, l, parameter, field) -> {
					field.setOptions(
							OptionsFactory.of(JanitzaMeter.JanitzaModel.class, JanitzaMeter.JanitzaModel.UMG_511), l);
				}))), //
		INTEGRATION_TYPE(CommunicationProps.modbusType()//
				.setRequired(true)), //
		IP(MeterProps.ip() //
				.setDefaultValue("10.4.0.12") //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf((Exp.currentModelValue(INTEGRATION_TYPE)//
							.equal(Exp.staticValue(ModbusType.TCP))));
				})), //
		PORT(MeterProps.port() //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf((Exp.currentModelValue(INTEGRATION_TYPE).equal(Exp.staticValue(ModbusType.TCP))));
				})), //
		INVERT(MeterProps.invert(METER_ID)//
				.setIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN))), //
		;

		private AppDef<? super GridMeterJanitza, ? super Property, ? super BundleParameter> def;

		Property(AppDef<? super GridMeterJanitza, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, GridMeterJanitza, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super GridMeterJanitza, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<GridMeterJanitza>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public GridMeterJanitza(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var meterId = this.getId(t, p, Property.METER_ID, "meter0");

			final var alias = this.getString(p, l, Property.ALIAS);
			final var factoryId = this.getString(p, Property.MODEL);
			final var invert = this.getBoolean(p, Property.INVERT);

			final var components = new ArrayList<EdgeConfig.Component>();

			final var ip = this.getString(p, Property.IP);
			final var port = this.getInt(p, Property.PORT);
			final var tcpModbusId = this.getId(t, p, Property.MODBUS_ID);

			components.add(new EdgeConfig.Component(tcpModbusId,
					TranslationUtil.translate(AbstractOpenemsApp.getTranslationBundle(l), "App.Meter.alias"),
					"Bridge.Modbus.Tcp", //
					JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.addProperty("port", port) //
							.build()));

			components.add(new EdgeConfig.Component(meterId, alias, factoryId, //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", tcpModbusId) //
							.addProperty("type", GRID) //
							.addProperty("invert", invert)//
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.GRID_METER };
	}

	@Override
	protected GridMeterJanitza getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(checkIndustrialL());
	}

}
