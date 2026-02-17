package io.openems.edge.app.ess;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.ess.AppSohCycle.Property;
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
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Installs the SoH Cycle controller for a managed ESS with safe defaults.
 */
@Component(name = AppSohCycle.APP_ESS_SOH_CYCLE)
public class AppSohCycle extends AbstractOpenemsAppWithProps<AppSohCycle, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static final String APP_ESS_SOH_CYCLE = "App.Ess.SohCycle";
	private static final String CONTROLLER_ESS_SOH_CYCLE_FACTORY_ID = "Controller.Ess.SoH.Cycle";
	public static final String CTRL_ESS_SOH_CYCLE_0 = "ctrlEssSohCycle0";

	public enum Property implements Type<Property, AppSohCycle, Parameter.BundleParameter>, Nameable {
		// Components
		CTRL_ESS_SOH_CYCLE_ID(AppDef.componentId(CTRL_ESS_SOH_CYCLE_0)), //

		// Properties
		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		IS_RUNNING(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".isRunning.label")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable)//
				.bidirectional(CTRL_ESS_SOH_CYCLE_ID, "isRunning", //
						ComponentManagerSupplier::getComponentManager))), //
		LOG_VERBOSITY(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".logVerbosity.label") //
				.setDefaultValue("NONE") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectFromNameable,
						(app, property, l, parameter, field) -> field.setOptions(List.of("NONE", "DEBUG_LOG")))//
				.bidirectional(CTRL_ESS_SOH_CYCLE_ID, "logVerbosity", //
						ComponentManagerSupplier::getComponentManager))), // , //
		REFERENCE_CYCLE_ENABLED(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".referenceCycleEnabled.label")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable)//
				.setRequired(true)//
				.bidirectional(CTRL_ESS_SOH_CYCLE_ID, "referenceCycleEnabled", //
						ComponentManagerSupplier::getComponentManager))), // , //
		;

		private final AppDef<? super AppSohCycle, ? super Property, ? super BundleParameter> def;

		Property(AppDef<? super AppSohCycle, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super AppSohCycle, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppSohCycle>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppSohCycle(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil//
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.ESS };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlEssSohCycleId = this.getId(t, p, Property.CTRL_ESS_SOH_CYCLE_ID);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var essId = this.getString(p, Property.ESS_ID);
			final var isRunning = this.getBoolean(p, Property.IS_RUNNING);
			final var logVerbosity = this.getString(p, Property.LOG_VERBOSITY);
			final var referenceCycleEnabled = this.getBoolean(p, Property.REFERENCE_CYCLE_ENABLED);

			final var controllerComponent = new EdgeConfig.Component(ctrlEssSohCycleId, alias,
					CONTROLLER_ESS_SOH_CYCLE_FACTORY_ID, JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.addProperty("ess.id", essId) //
							.addProperty("isRunning", isRunning) //
							.addProperty("logVerbosity", logVerbosity) //
							.addProperty("referenceCycleEnabled", referenceCycleEnabled) //
							.build());
			return AppConfiguration.create() //
					.addTask(Tasks.component(controllerComponent)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent(ctrlEssSohCycleId,
							CONTROLLER_ESS_SOH_CYCLE_FACTORY_ID, this.getAppId())))
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected AppSohCycle getApp() {
		return this;
	}
}
