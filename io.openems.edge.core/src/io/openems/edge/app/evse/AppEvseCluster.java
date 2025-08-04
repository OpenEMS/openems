package io.openems.edge.app.evse;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_VALUE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.Evse.Controller.Cluster")
public class AppEvseCluster extends
		AbstractOpenemsAppWithProps<AppEvseCluster, AppEvseCluster.Property, BundleParameter> implements OpenemsApp {

	public static enum Property implements Type<Property, AppEvseCluster, BundleParameter>, Nameable {
		// Component-IDs
		EVSE_CLUSTER_ID(AppDef.componentId("ctrlEvseCluster0")), //
		// Properties
		ALIAS(alias()), //
		EVSE_IDS(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".evseIds.label") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelect, (app, prop, l, param, f) -> {
					f.setOptions(app.getComponentUtil().getEnabledComponentsOfStartingId("ctrlEvseSingle"),
							DEFAULT_COMPONENT_2_LABEL, DEFAULT_COMPONENT_2_VALUE) //
							.isMulti(true);
				}) //
				.setDefaultValue((app, property, l, parameter) -> new JsonArray()) //
				.bidirectional(EVSE_CLUSTER_ID, "ctrl.ids", ComponentManagerSupplier::getComponentManager))), //
		;

		private final AppDef<? super AppEvseCluster, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppEvseCluster, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppEvseCluster, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppEvseCluster, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppEvseCluster>, BundleParameter> getParamter() {
			return BundleParameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public AppEvseCluster(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil//
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected AppEvseCluster getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var evseClusterId = this.getId(t, p, Property.EVSE_CLUSTER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);

			final var evseIds = this.getJsonArray(p, Property.EVSE_IDS);

			var components = Lists
					.newArrayList(new EdgeConfig.Component(evseClusterId, alias, "Evse.Controller.Cluster", //
							JsonUtils.buildJsonObject() //
									.add("ctrl.ids", evseIds) //
									.build()));

			return AppConfiguration.create()//
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanSee(Role.ADMIN)//
				.setCanDelete(Role.ADMIN)//
				.build();
	}

	/**
	 * Creates a {@link DependencyDeclaration} for a {@link AppEvseCluster}.
	 * 
	 * @return the {@link DependencyDeclaration}
	 */
	public static List<DependencyDeclaration> dependency() {
		return Lists.newArrayList(new DependencyDeclaration("CLUSTER", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				AppDependencyConfig.create() //
						.setAppId("App.Evse.Controller.Cluster") //
						.build()));
	}
}
