package io.openems.edge.app.ess;

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
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.ess.FixReactivePower.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

/**
 * Describes a fix reactive power app.
 *
 * <pre>
  {
    "appId":"App.Ess.FixReactivePower",
    "alias":"Leistungsvorgabe",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ESS_ID": "ess0"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Ess.FixReactivePower")
public class FixReactivePower extends AbstractOpenemsAppWithProps<FixReactivePower, Property, BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, FixReactivePower, BundleParameter>, Nameable {
		// Components
		CTRL_FIX_REACTIVE_POWER_ID(AppDef.componentId("ctrlFixReactivePower0")), //

		// Properties
		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		;

		private final AppDef<? super FixReactivePower, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super FixReactivePower, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super FixReactivePower, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FixReactivePower>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public FixReactivePower(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
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
			final var ctrlFixReactivePowerId = this.getId(t, p, Property.CTRL_FIX_REACTIVE_POWER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var essId = this.getString(p, Property.ESS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlFixReactivePowerId, alias, "Controller.Symmetric.FixReactivePower", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.onlyIf(t == ConfigurationTarget.ADD, //
											b -> b.addProperty("mode", "MANUAL_OFF") //
													.addProperty("power", 0)) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(//
							new SchedulerComponent(ctrlFixReactivePowerId, "Controller.Symmetric.FixReactivePower",
									this.getAppId()))) //
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
	protected FixReactivePower getApp() {
		return this;
	}
}
