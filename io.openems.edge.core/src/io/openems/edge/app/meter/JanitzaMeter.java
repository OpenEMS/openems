package io.openems.edge.app.meter;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;

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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.ModbusType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.meter.JanitzaMeter.Property;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a App for a Janitza meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Janitza",
    "alias":"Janitza Zähler",
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
@Component(name = "App.Meter.Janitza")
public class JanitzaMeter extends AbstractOpenemsAppWithProps<JanitzaMeter, Property, Parameter.BundleParameter>
		implements OpenemsApp, ComponentUtilSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, JanitzaMeter, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter1")), //
		MODBUS_ID(AppDef.componentId("modbus2")), //
		// Properties
		ALIAS(alias()), //
		MODEL(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".productModel") //
				.setDefaultValue(JanitzaModel.UMG_96_RME.getValue()) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelect, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(JanitzaModel.class), l);
				}))), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type(MeterType.GRID), def -> def //
				.setRequired(true))), //
		INTEGRATION_TYPE(CommunicationProps.modbusType() //
				.setRequired(true)), //
		IP(MeterProps.ip() //
				.setDefaultValue("10.4.0.12") //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf((Exp.currentModelValue(INTEGRATION_TYPE) //
							.equal(Exp.staticValue(ModbusType.TCP))));
				})), //
		PORT(MeterProps.port() //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf((Exp.currentModelValue(INTEGRATION_TYPE) //
							.equal(Exp.staticValue(ModbusType.TCP))));
				})), //
		SELECTED_MODBUS_ID(AppDef.copyOfGeneric(ComponentProps.pickSerialModbusId(), def -> def //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					if (PropsUtil.isHomeInstalled(app.getAppManagerUtil())) {
						field.readonly(true);
					}
					field.onlyShowIf(Exp.currentModelValue(INTEGRATION_TYPE) //
							.equal(Exp.staticValue(ModbusType.RTU)));
				})) //
				.setAutoGenerateField(false)), //
		MODBUS_UNIT_ID(MeterProps.modbusUnitId() //
				.setRequired(true) //
				.setDefaultValue(7) //
				.setAutoGenerateField(false)), //
		MODBUS_GROUP(CommunicationProps.modbusGroup(//
				SELECTED_MODBUS_ID, SELECTED_MODBUS_ID.def(), //
				MODBUS_UNIT_ID, MODBUS_UNIT_ID.def(), INTEGRATION_TYPE)), //
		;

		private AppDef<? super JanitzaMeter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super JanitzaMeter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, JanitzaMeter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super JanitzaMeter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<JanitzaMeter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public JanitzaMeter(//
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
			final var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			final var alias = this.getString(p, l, Property.ALIAS);
			final var factorieId = this.getString(p, Property.MODEL);
			final var type = this.getEnum(p, MeterType.class, Property.TYPE);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
			final var integrationType = this.getEnum(p, ModbusType.class, Property.INTEGRATION_TYPE);

			final var components = new ArrayList<EdgeConfig.Component>();

			final var modbusId = switch (integrationType) {
			case RTU -> this.getString(p, Property.SELECTED_MODBUS_ID);
			case TCP -> {
				final var ip = this.getString(p, Property.IP);
				final var port = this.getInt(p, Property.PORT);
				final var tcpModbusId = this.getId(t, p, Property.MODBUS_ID);

				components.add(new EdgeConfig.Component(tcpModbusId, "bridge", "Bridge.Modbus.Tcp", //
						JsonUtils.buildJsonObject() //
								.addProperty("ip", ip) //
								.addProperty("port", port) //
								.build()));

				yield tcpModbusId;
			}
			};

			components.add(new EdgeConfig.Component(meterId, alias, factorieId, //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	protected JanitzaMeter getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	public enum JanitzaModel implements TranslatableEnum {
		UMG_96_RME("Meter.Janitza.UMG96RME", "Janitza Netzanalysator UMG 96RM-E"), //
		UMG_604("Meter.Janitza.UMG604", "Janitza Netzanalysator UMG 604-PRO"), //
		UMG_511("Meter.Janitza.UMG511", "Janitza Netzqualitätsanalysator UMG 511"), //
		;

		private final String value;
		private final String translation;

		private JanitzaModel(String value, String translation) {
			this.value = value;
			this.translation = translation;
		}

		@Override
		public String getTranslation(Language language) {
			return this.translation;
		}

		@Override
		public String getValue() {
			return this.value;
		}

	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

}
