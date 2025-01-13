package io.openems.edge.app.ess;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.ess.Limiter14a.Limiter14aBundle;
import io.openems.edge.app.ess.Limiter14a.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

@Component(name = "App.Ess.Limiter14a")
public class Limiter14a extends AbstractOpenemsAppWithProps<Limiter14a, Property, Limiter14aBundle>
		implements OpenemsApp {

	public record Limiter14aBundle(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, Limiter14a, Limiter14aBundle> {
		CTRL_ESS_LIMITER_14A_ID(AppDef.componentId("ctrlEssLimiter14a0")), //

		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		INPUT_CHANNEL_ADDRESS(RelayProps.relayContactDef(1)), //
		;

		private final AppDef<? super Limiter14a, ? super Property, ? super Limiter14aBundle> def;

		private Property(AppDef<? super Limiter14a, ? super Property, ? super Limiter14aBundle> def) {
			this.def = def;
		}

		@Override
		public Type<Property, Limiter14a, Limiter14aBundle> self() {
			return this;
		}

		@Override
		public AppDef<? super Limiter14a, ? super Property, ? super Limiter14aBundle> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<Limiter14a>, Limiter14aBundle> getParamter() {
			return t -> new Limiter14aBundle(//
					createResourceBundle(t.language), //
					RelayProps.createPhaseInformation(t.app.componentUtil, 1, emptyList(), emptyList()) //
			);
		}

	}

	@Activate
	public Limiter14a(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
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
	protected Limiter14a getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var id = this.getId(t, p, Property.CTRL_ESS_LIMITER_14A_ID);

			final var alias = this.getString(p, Property.ALIAS);
			final var essId = this.getString(p, Property.ESS_ID);
			final var inputAddress = this.getString(p, Property.INPUT_CHANNEL_ADDRESS);

			final var components = List.of(//
					new EdgeConfig.Component(id, alias, "Controller.Ess.Limiter14a", //
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", essId) //
									.addProperty("inputChannelAddress", inputAddress) //
									.build()));

			final var componentIdOfRelay = inputAddress.substring(0, inputAddress.indexOf('/'));
			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(
							new SchedulerComponent(id, "Controller.Ess.Limiter14a", this.getAppId()))) //
					.onlyIf(appIdOfRelay != null, c -> c.addDependency(new DependencyDeclaration("RELAY", //
							DependencyDeclaration.CreatePolicy.NEVER, //
							DependencyDeclaration.UpdatePolicy.NEVER, //
							DependencyDeclaration.DeletePolicy.NEVER, //
							DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
							DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
							DependencyDeclaration.AppDependencyConfig.create() //
									.setSpecificInstanceId(appIdOfRelay) //
									.build())))//
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

}
