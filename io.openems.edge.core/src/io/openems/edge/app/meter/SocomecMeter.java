package io.openems.edge.app.meter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.meter.SocomecMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;

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
@Component(name = "App.Meter.Socomec")
public class SocomecMeter extends AbstractMeterApp<Property> implements OpenemsApp {

	public enum Property implements Nameable {
		// Component-IDs
		METER_ID, //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type(MeterType.GRID))), //
		MODBUS_ID(AppDef.copyOfGeneric(ComponentProps.pickModbusId(),
				def -> def.wrapField((app, property, l, parameter, field) -> {
					if (PropsUtil.isHomeInstalled(app.getAppManagerUtil())) {
						field.readonly(true);
					}
					field.isRequired(true);
				}).setAutoGenerateField(false))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), //
				def -> def.setAutoGenerateField(false) //
						.setDefaultValue(7) //
						.wrapField((app, property, l, parameter, field) -> field.isRequired(true)))), //
		MODBUS_GROUP(AppDef.copyOfGeneric(CommunicationProps.modbusGroup(//
				MODBUS_ID, MODBUS_ID.def(), MODBUS_UNIT_ID, MODBUS_UNIT_ID.def())));

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

	@Activate
	public SocomecMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var modbusId = this.getValueOrDefault(p, Property.MODBUS_ID, "modbus1");
			var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var type = this.getValueOrDefault(p, Property.TYPE, "PRODUCTION");
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(meterId, alias, "Meter.Socomec.Threephase", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("type", type) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.TYPE) //
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Meter.mountType.label")) //
								.setOptions(this.buildMeterOptions(language)) //
								.setDefaultValue("PRODUCTION") //
								.build()) //
						.add(JsonFormlyUtil.buildSelect(Property.MODBUS_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusId")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "modbusId.description")) //
								.setOptions(this.componentUtil.getEnabledComponentsOfStartingId("modbus"),
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_LABEL,
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_VALUE) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusUnitId")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.Meter.modbusUnitId.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(6) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

}
