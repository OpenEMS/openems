package io.openems.edge.app.loadcontrol;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.app.loadcontrol.AppBraiinsOsDiy.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

@Component(name = "App.LoadControl.BraiinsOs")
public class AppBraiinsOsDiy extends AbstractOpenemsAppWithProps<AppBraiinsOsDiy, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, AppBraiinsOsDiy, BundleParameter>, Nameable {
		COMPONENT_ID(AppDef.componentId("ctrlBraiinsSingle0")), //
		ALIAS(alias()), //
		PHASE(BraiinsOsProps.singleOrAllPhase()), //
		IP(BraiinsOsProps.ip()), //
		READ_ONLY(BraiinsOsProps.readOnly()), //
		DEFAULT_CONSUMPTION(BraiinsOsProps.consumption()), //
		USERNAME(BraiinsOsProps.username()), //
		PASSWORD(BraiinsOsProps.password(COMPONENT_ID)), //
		DISCLAIMER_CONFIRMATION(BraiinsOsProps.disclaimerConfirmation(COMPONENT_ID)), //
		;

		private final AppDef<? super AppBraiinsOsDiy, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super AppBraiinsOsDiy, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super AppBraiinsOsDiy, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppBraiinsOsDiy>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, AppBraiinsOsDiy, Parameter.BundleParameter> self() {
			return this;
		}

	}

	@Activate
	public AppBraiinsOsDiy(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.LOAD_CONTROL };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected AppBraiinsOsDiy getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var controllerId = this.getId(t, p, Property.COMPONENT_ID);
			final var alias = this.getString(p, Property.ALIAS);
			final var phase = this.getEnum(p, Phase.class, Property.PHASE);
			final var ip = this.getString(p, Property.IP);
			final var readOnly = this.getBoolean(p, Property.READ_ONLY);
			final var username = this.getString(p, Property.USERNAME);
			final var password = this.getString(p, Property.PASSWORD);
			final var defaultConsumption = this.getInt(p, Property.DEFAULT_CONSUMPTION);

			final var props = JsonUtils.buildJsonObject() //
					.addProperty("id", controllerId) //
					.addProperty("phase", phase) //
					.addProperty("ip", ip) //
					.addProperty("readOnly", readOnly) //
					.addProperty("defaultConsumptionW", defaultConsumption) //
					.addProperty("username", username) //
					.onlyIf(password != null && !password.equals(CommonProps.MASKED_PASSWORD), b -> {
						b.addProperty("password", password);
					});

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, alias, "Controller.BraiinsOS.Single", props.build()));

			return AppConfiguration.create() //
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
		return OpenemsAppPermissions.create() //
				.setCanInstall(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.setCanSee(Role.ADMIN) //
				.build();
	}

}
