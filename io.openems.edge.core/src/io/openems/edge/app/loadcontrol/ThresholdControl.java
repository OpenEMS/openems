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
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.loadcontrol.ThresholdControl.Property;
import io.openems.edge.app.loadcontrol.ThresholdControl.ThresholdControlControlParameter;
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
 * Describes a App for a Threshold Controller.
 *
 * <pre>
  {
    "appId":"App.LoadControl.ThresholdControl",
    "alias":"Schwellwertsteuerung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID": "ctrlIoChannelSingleThreshold0",
    	"OUTPUT_CHANNELS":['io1/Relay1', 'io1/Relay2']
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.LoadControl.ThresholdControl")
public class ThresholdControl
		extends AbstractOpenemsAppWithProps<ThresholdControl, Property, ThresholdControlControlParameter>
		implements OpenemsApp {

	public record ThresholdControlControlParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, ThresholdControl, ThresholdControlControlParameter> {
		// Component-IDs
		CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID(AppDef.componentId("ctrlIoChannelSingleThreshold0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNELS(AppDef.copyOfGeneric(relayContactDef(true, 1), def -> def//
				.setTranslatedLabelWithAppPrefix(".outputChannels.label") //
				.setTranslatedDescriptionWithAppPrefix(".outputChannels.description"))), //
		;

		private final AppDef<? super ThresholdControl, ? super Property, ? super ThresholdControlControlParameter> def;

		private Property(
				AppDef<? super ThresholdControl, ? super Property, ? super ThresholdControlControlParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, ThresholdControl, ThresholdControlControlParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super ThresholdControl, ? super Property, ? super ThresholdControlControlParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<ThresholdControl>, ThresholdControlControlParameter> getParamter() {
			return t -> {
				final var isHomeInstalled = PropsUtil.isHomeInstalled(t.app.appManagerUtil);

				return new ThresholdControlControlParameter(//
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
	public ThresholdControl(//
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

			final var ctrlIoChannelSingleThresholdId = this.getId(t, p, Property.CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));

			final var outputChannelAddress = this.getJsonArray(p, Property.OUTPUT_CHANNELS);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoChannelSingleThresholdId, alias,
							"Controller.IO.ChannelSingleThreshold", JsonUtils.buildJsonObject() //
									.onlyIf(t == ConfigurationTarget.ADD,
											j -> j.addProperty("inputChannelAddress", "_sum/EssSoc"))
									.add("outputChannelAddress", outputChannelAddress) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("threshold", 50)) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-schwellwert-steuerung/") //
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
	protected ThresholdControl getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
