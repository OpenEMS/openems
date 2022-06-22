package io.openems.edge.app.pvinverter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.SmaPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for SMA PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Sma",
    "alias":"SMA PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502",
    	"MODBUS_UNIT_ID": "126"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-sma-pv-wechselrichter/">https://fenecon.de/fems-2-2/fems-app-sma-pv-wechselrichter/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Sma")
public class SmaPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT, //
		MODBUS_UNIT_ID;
	}

	@Activate
	public SmaPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "PV-Inverter.SMA.SunnyTripower";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);
			var inverter = AbstractOpenemsApp.getComponentWithFactoryId(components, factoryIdInverter);
			inverter.getProperties().put("modbusUnitId", JsonUtils.parse(Integer.toString(modbusUnitId)));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.ip.description")) //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel(TranslationUtil.getTranslation(bundle, "port")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.PvInverter.port.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusUnitId")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "modbusUnitId.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(126) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-sma-pv-wechselrichter/") //
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

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "AAASUVORK5CYII=";
	}

}
