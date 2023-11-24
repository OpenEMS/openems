package io.openems.edge.app.heat;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.RelayProps.createPhaseInformation;
import static io.openems.edge.app.common.props.RelayProps.phaseGroup;
import static io.openems.edge.app.common.props.RelayProps.relayContactDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.heat.HeatPump.HeatPumpParameter;
import io.openems.edge.app.heat.HeatPump.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCountFilters;

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
    	"CTRL_IO_HEAT_PUMP_ID": "ctrlIoHeatPump0",
    	"OUTPUT_CHANNEL_1": "io0/Relay2",
    	"OUTPUT_CHANNEL_2": "io0/Relay3"
    },
    "dependencies": [
    	{
        	"key": "RELAY",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatPump")
public class HeatPump extends AbstractOpenemsAppWithProps<HeatPump, Property, HeatPumpParameter> implements OpenemsApp {

	public record HeatPumpParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, HeatPump, HeatPumpParameter> {
		// Component-IDs
		CTRL_IO_HEAT_PUMP_ID(AppDef.componentId("ctrlIoHeatPump0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNEL_1(heatPumpRelayContactDef(1)), //
		OUTPUT_CHANNEL_2(heatPumpRelayContactDef(2)), //
		PHASE_GROUP(phaseGroup(OUTPUT_CHANNEL_1, OUTPUT_CHANNEL_2)), //
		;

		private final AppDef<? super HeatPump, ? super Property, ? super HeatPumpParameter> def;

		private Property(AppDef<? super HeatPump, ? super Property, ? super HeatPumpParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, HeatPump, HeatPumpParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super HeatPump, ? super Property, ? super HeatPumpParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<HeatPump>, HeatPumpParameter> getParamter() {
			return t -> {
				final var isHomeInstalled = PropsUtil.isHomeInstalled(t.app.appManagerUtil);

				return new HeatPumpParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 2, //
								List.of(RelayProps.feneconHomeFilter(t.language, isHomeInstalled, false)), //
								List.of(RelayProps.feneconHome2030PreferredRelays(isHomeInstalled, new int[] { 5, 6 }), //
										PreferredRelay.of(4, new int[] { 2, 3 }), //
										PreferredRelay.of(8, new int[] { 2, 3 }))) //
				);
			};
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public HeatPump(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ctrlIoHeatPumpId = this.getId(t, p, Property.CTRL_IO_HEAT_PUMP_ID);

			final var outputChannel1 = this.getString(p, Property.OUTPUT_CHANNEL_1);
			final var outputChannel2 = this.getString(p, Property.OUTPUT_CHANNEL_2);

			final var components = List.of(//
					new EdgeConfig.Component(ctrlIoHeatPumpId, alias, "Controller.Io.HeatPump.SgReady",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannel1", outputChannel1) //
									.addProperty("outputChannel2", outputChannel2) //
									.build()) //
			);

			final var componentIdOfRelay = outputChannel1.substring(0, outputChannel1.indexOf('/'));
			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay == null) {
				// relay may be created but not as an app
				return AppConfiguration.create() //
						.addTask(Tasks.component(components)) //
						.build();
			}

			final var dependencies = List.of(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(appIdOfRelay) //
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fenecon-fems/fems-app-power-to-heat/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(2, CheckRelayCountFilters.feneconHome(false)));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected HeatPump getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	private static <P extends BundleProvider & RelayContactInformationProvider> //
	AppDef<OpenemsApp, Nameable, P> heatPumpRelayContactDef(int contactPosition) {
		return AppDef.copyOfGeneric(relayContactDef(contactPosition, //
				Nameable.of("OUTPUT_CHANNEL_1"), Nameable.of("OUTPUT_CHANNEL_2")),
				b -> b.setTranslatedLabelWithAppPrefix(".outputChannel" + contactPosition + ".label") //
						.setTranslatedDescription("App.Heat.outputChannel.description") //
						.setRequired(true) //
						.setAutoGenerateField(false));
	}

}
