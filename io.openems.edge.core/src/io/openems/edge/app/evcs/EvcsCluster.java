package io.openems.edge.app.evcs;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import io.openems.edge.app.evcs.EvcsCluster.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

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
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Evcs.Cluster")
public class EvcsCluster extends AbstractOpenemsAppWithProps<EvcsCluster, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, EvcsCluster, BundleParameter>, Nameable {
		// Component-IDs
		EVCS_CLUSTER_ID(AppDef.of(EvcsCluster.class) //
				.setDefaultValue("evcsCluster0")), //
		// Properties
		ALIAS(AppDef.of(EvcsCluster.class) //
				.setDefaultValueToAppName()), //
		EVCS_IDS(AppDef.of(EvcsCluster.class) //
				.setLabel("EVCS-IDs") //
				.setTranslatedDescriptionWithAppPrefix(".evcsIds.description") //
				.setField(JsonFormlyUtil::buildSelect, (app, prop, l, param, f) -> {
					f.setOptions(
							app.componentUtil.getEnabledComponentsOfStartingId("evcs").stream()
									.filter(t -> !t.id().startsWith("evcsCluster")).collect(Collectors.toList()),
							JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_LABEL,
							JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_VALUE) //
							.isRequired(true) //
							.isMulti(true);
				}) //
				.bidirectional(EVCS_CLUSTER_ID, "evcs.ids", a -> a.componentManager)) //
		;

		private final AppDef<EvcsCluster, Property, BundleParameter> def;

		private Property(AppDef<EvcsCluster, Property, BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<EvcsCluster, Property, BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<EvcsCluster>, BundleParameter> getParamter() {
			return BundleParameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public EvcsCluster(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			var evcsClusterId = this.getId(t, p, Property.EVCS_CLUSTER_ID);

			var alias = this.getString(p, l, Property.ALIAS);
			var ids = this.getJsonArray(p, Property.EVCS_IDS);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsClusterId, alias, "Evcs.Cluster.PeakShaving",
							JsonUtils.buildJsonObject() //
									.add("evcs.ids", ids) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-multiladepunkt-management/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected EvcsCluster getApp() {
		return this;
	}

}
