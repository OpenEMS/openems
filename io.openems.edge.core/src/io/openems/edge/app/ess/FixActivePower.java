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
import io.openems.edge.app.enums.Phase;
import io.openems.edge.app.ess.FixActivePower.Property;
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
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a fix active power app.
 *
 * <pre>
  {
    "appId":"App.Ess.FixActivePower",
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
@Component(name = "App.Ess.FixActivePower")
public class FixActivePower extends AbstractOpenemsAppWithProps<FixActivePower, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, FixActivePower, Parameter.BundleParameter>, Nameable {
		// Components
		CTRL_FIX_ACTIVE_POWER_ID(AppDef.componentId("ctrlFixActivePower0")), //

		// Properties
		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		;

		private final AppDef<? super FixActivePower, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super FixActivePower, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super FixActivePower, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FixActivePower>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public FixActivePower(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
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
			final var ctrlFixActivePowerId = this.getId(t, p, Property.CTRL_FIX_ACTIVE_POWER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var essId = this.getString(p, Property.ESS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlFixActivePowerId, alias, "Controller.Ess.FixActivePower", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess_id", essId) //
									.onlyIf(t == ConfigurationTarget.ADD, //
											b -> b.addProperty("mode", "MANUAL_OFF") //
													.addProperty("hybridEssMode", "TARGET_DC") //
													.addProperty("power", 0) //
													.addProperty("relationship", "EQUALS") //
													.addProperty("phase", Phase.ALL)) //
									.build()) //
			);

			// TODO improve scheduler configuration
			final var schedulerIds = Lists.newArrayList(//
					ctrlFixActivePowerId, //
					"ctrlPrepareBatteryExtension0", //
					"ctrlEmergencyCapacityReserve0", //
					"ctrlGridOptimizedCharge0" //
			);
			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.scheduler(schedulerIds)) //
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected FixActivePower getApp() {
		return this;
	}
}
