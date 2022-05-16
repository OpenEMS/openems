package io.openems.edge.app.hardware;

import java.util.EnumMap;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.hardware.KMtronic8Channel.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

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
    "appDescriptor": {}
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Hardware.KMtronic8Channel")
public class KMtronic8Channel extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		IO_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP;
	}

	@Activate
	public KMtronic8Channel(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName());
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.1.199");

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus10");
			var ioId = this.getId(t, p, Property.IO_ID, "io1");

			List<Component> comp = Lists.newArrayList(//
					new EdgeConfig.Component(ioId, alias, "IO.KMtronic", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.build()), //
					new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build())//
			);
			return new AppConfiguration(comp, null, Lists.newArrayList("192.168.1.198/28"));
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel("IP-Address") //
								.setDescription("The IP address of the Relay.") //
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
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "FEMS Relais 8-Kanal";
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
