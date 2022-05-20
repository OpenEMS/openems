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
import io.openems.edge.app.pvinverter.KacoPvInverter.Property;
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

/**
 * Describes a App for Kaco PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Kaco",
    "alias":"KACO PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/">https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Kaco")
public class KacoPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT;
	}

	@Activate
	public KacoPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "PV-Inverter.KACO.blueplanet";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(bundle.getString("ipAddress")) //
								.setDescription(bundle.getString("App.PvInverter.ip.description")) //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel(bundle.getString("port")) //
								.setDescription(bundle.getString("App.PvInverter.port.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/") //
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
		return "data:image/png;base64,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";
	}

}
