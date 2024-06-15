package io.openems.edge.app.heat;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.RelayProps.createPhaseInformation;
import static io.openems.edge.app.common.props.RelayProps.relayContactDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.heat.CombinedHeatAndPower.CombinedHeatAndPowerParameter;
import io.openems.edge.app.heat.CombinedHeatAndPower.Property;
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
 * Describes a App for a Heating Element.
 *
 * <pre>
  {
    "appId":"App.Heat.CHP",
    "alias":"Blockheizkraftwerk (BHKW)",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_CHP_SOC_ID": "ctrlChpSoc0",
    	"OUTPUT_CHANNEL": "io0/Relay1"
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
@Component(name = "App.Heat.CHP")
public class CombinedHeatAndPower
		extends AbstractOpenemsAppWithProps<CombinedHeatAndPower, Property, CombinedHeatAndPowerParameter>
		implements OpenemsApp {

	public record CombinedHeatAndPowerParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, CombinedHeatAndPower, CombinedHeatAndPowerParameter> {
		// Component-IDs
		CTRL_CHP_SOC_ID(AppDef.componentId("ctrlChpSoc0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNEL(chpRelayContactDef(1)), //
		;

		private final AppDef<? super CombinedHeatAndPower, ? super Property, ? super CombinedHeatAndPowerParameter> def;

		private Property(
				AppDef<? super CombinedHeatAndPower, ? super Property, ? super CombinedHeatAndPowerParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, CombinedHeatAndPower, CombinedHeatAndPowerParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super CombinedHeatAndPower, ? super Property, ? super CombinedHeatAndPowerParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<CombinedHeatAndPower>, CombinedHeatAndPowerParameter> getParamter() {
			return t -> {
				final var isHomeInstalled = PropsUtil.isHomeInstalled(t.app.appManagerUtil);

				return new CombinedHeatAndPowerParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 1, //
								List.of(RelayProps.feneconHomeFilter(t.language, isHomeInstalled, false)), //
								List.of(RelayProps.feneconHome2030PreferredRelays(isHomeInstalled, new int[] { 5 }), //
										PreferredRelay.of(4, new int[] { 1 }), //
										PreferredRelay.of(8, new int[] { 1 }))) //
				);
			};
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public CombinedHeatAndPower(//
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
			final var chpId = this.getId(t, p, Property.CTRL_CHP_SOC_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var outputChannelAddress = this.getString(p, Property.OUTPUT_CHANNEL);

			final var components = List.of(//
					new EdgeConfig.Component(chpId, alias, "Controller.CHP.SoC", JsonUtils.buildJsonObject() //
							.addProperty("inputChannelAddress", "_sum/EssSoc")
							.addProperty("outputChannelAddress", outputChannelAddress) //
							.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("lowThreshold", 20)) //
							.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("highThreshold", 80)) //
							.build()) //
			);

			var componentIdOfRelay = outputChannelAddress.substring(0, outputChannelAddress.indexOf('/'));

			var instanceIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (instanceIdOfRelay == null) {
				// relay may be created but not as a app
				return AppConfiguration.create() //
						.addTask(Tasks.component(components)) //
						.build();
			}

			var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(instanceIdOfRelay) //
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addDependencies(dependencies) //
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
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(1, CheckRelayCountFilters.feneconHome(false)));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected CombinedHeatAndPower getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	private static <P extends BundleProvider & RelayContactInformationProvider> //
	AppDef<OpenemsApp, Nameable, P> chpRelayContactDef(int contactPosition) {
		return AppDef.copyOfGeneric(relayContactDef(contactPosition), def -> //
		def.setTranslatedLabelWithAppPrefix(".outputChannel.label") //
				.setTranslatedDescription("App.Heat.outputChannel.description"));
	}

}
