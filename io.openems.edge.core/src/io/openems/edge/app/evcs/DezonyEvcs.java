package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.Map;
import java.util.OptionalInt;
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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.DezonyEvcs.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.HostSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppStatus;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

/**
 * Describes a dezony IQ evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.Dezony",
    "alias":"dezoney IQ Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "CTRL_EVCS_ID": "ctrlEvcs0",
      "IP":"192.168.25.11",
      "PORT":"5000"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Dezony")
public class DezonyEvcs extends AbstractOpenemsAppWithProps<DezonyEvcs, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier {

	public enum Property implements Type<Property, DezonyEvcs, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		// Properties
		ALIAS(alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp(), //
				def -> def.setDefaultValue("192.168.50.88") //
						.setRequired(true))), //
		PORT(AppDef.copyOfGeneric(CommunicationProps.port(), //
				def -> def.setDefaultValue(5000) //
						.setRequired(true))), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(EvcsProps.clusterMaxHardwarePowerSingleCp(MAX_HARDWARE_POWER_ACCEPT_PROPERTY, EVCS_ID)), //
		UNOFFICIAL_APP_WARNING(CommonProps.installationHintOfUnofficialApp()), //
		;

		private final AppDef<? super DezonyEvcs, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super DezonyEvcs, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, DezonyEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super DezonyEvcs, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<DezonyEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;

	@Activate
	public DezonyEvcs(@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference Host host //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var controllerAlias = TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l),
					"App.Evcs.controller.alias");

			// values the user enters
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);
			final var port = this.getInt(p, Property.PORT);

			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, "Evcs.Dezony", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.addProperty("port", port) //
							.build()), //
					new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs", JsonUtils.buildJsonObject() //
							.addProperty("evcs.id", evcsId) //
							.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(
							new SchedulerComponent(ctrlEvcsId, "Controller.Evcs", this.getAppId()))) //
					.addDependencies(EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
							maxHardwarePowerPerPhase, evcsId)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected OpenemsAppStatus getStatus() {
		return OpenemsAppStatus.BETA;
	}

	@Override
	protected DezonyEvcs getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public Host getHost() {
		return this.host;
	}

}
