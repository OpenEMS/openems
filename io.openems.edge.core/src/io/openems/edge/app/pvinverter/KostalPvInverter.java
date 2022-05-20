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
import io.openems.edge.app.pvinverter.KostalPvInverter.Property;
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
 * Describes a App for Kostal PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Kostal",
    "alias":"Kostal PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502",
    	"MODBUS_UNIT_ID": "71"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-kostal-pv-wechselrichter/">https://fenecon.de/fems-2-2/fems-app-kostal-pv-wechselrichter/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Kostal")
public class KostalPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

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
	public KostalPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
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

			var factoryIdInverter = "PV-Inverter.Kostal";
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
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(bundle.getString("modbusUnitId")) //
								.setDescription(bundle.getString("modbusUnitId.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(71) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-kostal-pv-wechselrichter/") //
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
				+ "FrmxlxxdBotiX50C0+HtYsn9drdoYkLjk+FPBMoWMgL8/fdWO7O4NAm58PiV2UVlvSqPg2Zq7XZTncHi9P34tI/zyqQvVU1jZaAX"
				+ "+Btb7Gxb2ufH9dM65dDbQjyrKBgIf+ZmNQc3vsbDvaOSslenieTN6dP+6gd/oAvVfGZTpLhaGl84K0A12U7Z4+OpTYEeV5QsJCXx"
				+ "qIdfxueC3k4OfJR1mdFZSoXauMRorE2lVxv4+u8on8b3wvT+rbG8A95YVCwkJfOF1tPOBy7/mA2eFsD3GzMVn4wsN0+ECqcRgtBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEKSOQ8j/Ax4nCBWPTln0AAAAAElFTkSuQmCC";
	}

}
