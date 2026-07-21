package io.openems.edge.app.meter;

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
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.ModbusType;
import io.openems.edge.app.meter.SocomecMeter.Property;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.appmanager.formly.Exp;

/**
 * Describes a App for a Socomec meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Socomec",
    "alias":"Socomec Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"TYPE": "PRODUCTION",
    	"MODBUS_ID": "modbus1",
    	"MODBUS_UNIT_ID": 6
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = SocomecMeter.APP_METER_SOCOMEC)
public class SocomecMeter extends AbstractOpenemsAppWithProps<SocomecMeter, Property, Parameter.BundleParameter>
		implements OpenemsApp, ComponentUtilSupplier, AppManagerUtilSupplier {

	public static final String APP_METER_SOCOMEC = "App.Meter.Socomec";

	public enum Property implements Type<Property, SocomecMeter, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter1")), //
		TCP_MODBUS_ID(AppDef.componentId("modbus2")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type(MeterType.GRID))), //
		INTEGRATION_TYPE(AppDef.copyOfGeneric(CommunicationProps.modbusType(), def -> def//
				.setRequired(true)//
				.setDefaultValue(ModbusType.RTU))), //
		IP(MeterProps.ip() //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(INTEGRATION_TYPE)//
							.equal(Exp.staticValue(ModbusType.TCP)));
				})), //
		PORT(MeterProps.port() //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(INTEGRATION_TYPE)//
							.equal(Exp.staticValue(ModbusType.TCP)));
				})), //
		MODBUS_ID(AppDef.copyOfGeneric(ComponentProps.pickSerialModbusId(), def -> def//
				.setRequired(true)//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(INTEGRATION_TYPE)//
							.equal(Exp.staticValue(ModbusType.RTU)));
				})//
				.setAutoGenerateField(false))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), def -> def//
				.setRequired(true)//
				.setAutoGenerateField(false)//
				.setDefaultValue(6))), //
		INVERT(MeterProps.invert(METER_ID)), //
		MODBUS_GROUP(AppDef.copyOfGeneric(CommunicationProps.modbusGroup(//
				MODBUS_ID, MODBUS_ID.def(), MODBUS_UNIT_ID, MODBUS_UNIT_ID.def(), INTEGRATION_TYPE)));

		private final AppDef<? super SocomecMeter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super SocomecMeter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, SocomecMeter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super SocomecMeter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SocomecMeter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public SocomecMeter(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var meterId = this.getId(t, p, Property.METER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var type = this.getString(p, Property.TYPE);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
			final var integrationType = this.getEnum(p, ModbusType.class, Property.INTEGRATION_TYPE);
			final var invert = this.getBoolean(p, Property.INVERT);

			final var components = new ArrayList<ComponentDef>();

			final var modbusId = switch (integrationType) {
			case RTU -> this.getString(p, Property.MODBUS_ID);
			case TCP -> {
				final var ip = this.getString(p, Property.IP);
				final var port = this.getInt(p, Property.PORT);
				final var tcpModbusId = this.getId(t, p, Property.TCP_MODBUS_ID);
				components.add(new ComponentDef(tcpModbusId, //
						TranslationUtil.translate(AbstractOpenemsApp.getTranslationBundle(l), "App.Meter.alias"), //
						"Bridge.Modbus.Tcp", //
						ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
								.addProperty("ip", ip) //
								.addProperty("port", port) //
								.build()), //
						ComponentDef.Configuration.defaultConfig()));
				yield tcpModbusId;
			}
			};

			components.add(new ComponentDef(meterId, alias, "Meter.Socomec.Threephase", //
					ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.addProperty("invert", invert) //
							.build()), //
					ComponentDef.Configuration.defaultConfig()));

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.build();
		};
	}

	@Override
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected SocomecMeter getApp() {
		return this;
	}

}
