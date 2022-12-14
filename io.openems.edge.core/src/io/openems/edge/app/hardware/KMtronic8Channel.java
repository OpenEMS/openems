package io.openems.edge.app.hardware;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.hardware.KMtronic8Channel.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for KMtronic 8-Channel Relay.
 *
 * <pre>
  {
    "appId":"App.Hardware.KMtronic8Channel",
    "alias":"FEMS Relais 8-Kanal",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"IO_ID": "io0",
    	"MODBUS_ID": "modbus10",
    	"IP": "192.168.1.199"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Hardware.KMtronic8Channel")
public class KMtronic8Channel extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Component-IDs
		IO_ID, //
		MODBUS_ID, //
		// Properties
		ALIAS, //
		IP;
	}

	@Activate
	public KMtronic8Channel(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.1.199");

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus10");
			var ioId = this.getId(t, p, Property.IO_ID, "io1");

			var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ioId, alias, "IO.KMtronic", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.build()), //
					new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build())//
			);

			var ips = Lists.newArrayList(//
					new InterfaceConfiguration("eth0") //
							.addIp("Relay", "192.168.1.198/28") //
			);

			return new AppConfiguration(//
					comp, //
					null, //
					ip.startsWith("192.168.1.") ? ips : null //
			);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".ip.description")) //
								.setDefaultValue("192.168.1.199") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
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
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HARDWARE };
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
