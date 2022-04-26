package io.openems.edge.app.evcs;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evcs.EvcsCluster.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

/**
 * Describes a evcs cluster.
 *
 * <pre>
  {
    "appId":"App.Evcs.Cluster",
    "alias":"Multiladepunkt-Management",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_CLUSTER_ID": "evcsCluster0",
      "EVCS_IDS": [ "evcs0", "evcs1", ...]
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-multiladepunkt-eigenverbrauch-2/">https://fenecon.de/fems-2-2/fems-app-multiladepunkt-eigenverbrauch-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Evcs.Cluster")
public class EvcsCluster extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		ALIAS, //
		EVCS_CLUSTER_ID, //
		EVCS_IDS //
		;
	}

	@Activate
	public EvcsCluster(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			var evcsClusterId = this.getId(t, p, Property.EVCS_CLUSTER_ID, "evcsCluster0");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName());

			var ids = EnumUtils.getAsJsonArray(p, Property.EVCS_IDS);

			var components = Lists.newArrayList(new EdgeConfig.Component(evcsClusterId, alias,
					"Evcs.Cluster.PeakShaving", JsonUtils.buildJsonObject() //
							.add("evcs.ids", ids) //
							.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.EVCS_IDS).setLabel("EVCS-IDs") //
								.setDescription("IDs of EVCS devices.") //
								.setOptions(this.componentUtil.getEnabledComponentsOfStartingId("evcs"),
										t -> t.alias() == null || t.alias().isEmpty() ? t.id()
												: t.id() + ": " + t.alias(),
										OpenemsComponent::id)
								.isRequired(true) //
								.isMulti(true) //
								.build())
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "Multiladepunkt-Management";
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
