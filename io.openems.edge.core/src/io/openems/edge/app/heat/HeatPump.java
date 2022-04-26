package io.openems.edge.app.heat;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.heat.HeatPump.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

/**
 * Describes a App for a Heat Pump.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatPump",
    "alias":""SG-Ready" Wärmepumpe",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEAT_PUMP_ID": "ctrlIoHeatPump0"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-sg-ready-waermepumpe-2/">https://fenecon.de/fems-2-2/fems-app-sg-ready-waermepumpe-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatPump")
public class HeatPump extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		CTRL_IO_HEAT_PUMP_ID;
	}

	@Activate
	public HeatPump(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {
			final var ctrlIoHeatPumpId = this.getId(t, p, Property.CTRL_IO_HEAT_PUMP_ID, "ctrlIoHeatPump0");

			if (t.isDeleteOrTest()) {
				var comp = Lists.newArrayList(//
						new EdgeConfig.Component(ctrlIoHeatPumpId, this.getName(), "Controller.Io.HeatPump.SgReady",
								JsonUtils.buildJsonObject() //
										.build()));
				return new AppConfiguration(comp);
			}

			var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(ctrlIoHeatPumpId), new int[] { 2, 3 },
					new int[] { 2, 3 });
			if (relays == null) {
				throw new OpenemsException("Not enought relays available!");
			}
			var outputChannel1 = relays[0];
			var outputChannel2 = relays[1];

			var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoHeatPumpId, this.getName(), "Controller.Io.HeatPump.SgReady",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannel1", outputChannel1) //
									.addProperty("outputChannel2", outputChannel2) //
									.build()));
			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableNames(new Validator.MapBuilder<>(new TreeMap<String, Map<String, ?>>()) //
						.put(CheckRelayCount.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 2) //
										.build())
						.build());
	}

	@Override
	public String getName() {
		return "\"SG-Ready\" Wärmepumpe";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

}
