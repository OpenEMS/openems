package io.openems.edge.app.meter;

import java.util.ArrayList;
import java.util.EnumMap;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.CarloGavazziMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a app for a Carlo Gavazzi meter.
 *
 * <pre>
  {
    "appId":"App.Meter.CarloGavazzi",
    "alias":"Carlo Gavazzi Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"TYPE": "PRODUCTION",
    	"MODBUS_UNIT_ID": 6
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-carlo-gavazzi-zaehler-2/">https://fenecon.de/fems-2-2/fems-app-carlo-gavazzi-zaehler-2/</a>
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.CarloGavazzi")
public class CarloGavazziMeter extends AbstractMeterApp<Property> implements OpenemsApp {

	public enum Property {
		// Components
		METER_ID, //
		// User-Values
		ALIAS, //
		TYPE, //
		MODBUS_ID, //
		MODBUS_UNIT_ID, //
		;
	}

	@Activate
	public CarloGavazziMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
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

			var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component(meterId, alias, "Meter.CarloGavazzi.EM300", //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.build()));

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
								.setDescription(TranslationUtil.getTranslation(bundle, "modbusUnitId.description")) //
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
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-carlo-gavazzi-zaehler-2/") //
				.build();
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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";
	}

}
