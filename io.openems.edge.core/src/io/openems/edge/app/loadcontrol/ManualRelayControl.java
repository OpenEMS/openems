package io.openems.edge.app.loadcontrol;

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
import io.openems.edge.app.loadcontrol.ManualRelayControl.ManualRelayControlParameter;
import io.openems.edge.app.loadcontrol.ManualRelayControl.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCountFilters;

/**
 * Describes a App for a manual relay control.
 *
 * <pre>
  {
    "appId":"App.LoadControl.ManualRelayControl",
    "alias":"Manuelle Relaissteuerung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_FIX_DIGITAL_OUTPUT_ID": "ctrlIoFixDigitalOutput0",
    	"OUTPUT_CHANNEL": "io1/Relay1"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.LoadControl.ManualRelayControl")
public class ManualRelayControl extends
		AbstractOpenemsAppWithProps<ManualRelayControl, Property, ManualRelayControlParameter> implements OpenemsApp {

	public record ManualRelayControlParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, ManualRelayControl, ManualRelayControlParameter> {
		// Component-IDs
		CTRL_IO_FIX_DIGITAL_OUTPUT_ID(AppDef.componentId("ctrlIoFixDigitalOutput0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNEL(AppDef.copyOfGeneric(relayContactDef(1), def -> def//
				.setTranslatedLabelWithAppPrefix(".outputChannel.label") //
				.setTranslatedDescriptionWithAppPrefix(".outputChannel.description"))), //
		;

		private final AppDef<? super ManualRelayControl, ? super Property, ? super ManualRelayControlParameter> def;

		private Property(
				AppDef<? super ManualRelayControl, ? super Property, ? super ManualRelayControlParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, ManualRelayControl, ManualRelayControlParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super ManualRelayControl, ? super Property, ? super ManualRelayControlParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<ManualRelayControl>, ManualRelayControlParameter> getParamter() {
			return t -> {
				final var isHomeInstalled = PropsUtil.isHomeInstalled(t.app.appManagerUtil);

				return new ManualRelayControlParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 2, //
								List.of(RelayProps.feneconHomeFilter(t.language, isHomeInstalled, true)), //
								List.of(PreferredRelay.of(4, new int[] { 1 }), //
										PreferredRelay.of(8, new int[] { 1 }))) //
				);
			};
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public ManualRelayControl(//
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
			final var ctrlIoFixDigitalOutputId = this.getId(t, p, Property.CTRL_IO_FIX_DIGITAL_OUTPUT_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var outputChannelAddress = this.getString(p, Property.OUTPUT_CHANNEL);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoFixDigitalOutputId, alias, "Controller.Io.FixDigitalOutput",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannelAddress", outputChannelAddress) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.LOAD_CONTROL };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(1, CheckRelayCountFilters.feneconHome(true)));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected ManualRelayControl getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
