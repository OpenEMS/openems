package io.openems.edge.app.heat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.heat.CombinedHeatAndPower.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DefaultEnum;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

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
    	"CTRL_CHP_SOC_ID": "ctrlChpSoc0"
    },
    "appDescriptor": {
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.CHP")
public class CombinedHeatAndPower extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		// User values
		ALIAS("Blockheizkraftwerk"), //
		// Components
		CTRL_CHP_SOC_ID("ctrlChpSoc0");

		private String defaultValue;

		private Property(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String getDefaultValue() {
			return this.defaultValue;
		}

	}

	@Activate
	public CombinedHeatAndPower(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			final var bhcId = this.getId(t, p, Property.CTRL_CHP_SOC_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS);

			var outputChannelAddress = "io0/Relay1";

			if (!t.isDeleteOrTest()) {
				var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(bhcId), new int[] { 1 },
						new int[] { 1 });
				if (relays == null) {
					throw new OpenemsException("Not enough relays available!");
				}
				outputChannelAddress = relays[0];
			}
			List<Component> comp = new ArrayList<>();

			comp.add(new EdgeConfig.Component(bhcId, alias, "Controller.CHP.SoC", JsonUtils.buildJsonObject() //
					.addProperty("inputChannelAddress", "_sum/EssSoc")
					.addProperty("outputChannelAddress", outputChannelAddress) //
					.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("lowThreshold", 20)) //
					.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("highThreshold", 80)) //
					.build()));//

			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableNames(new Validator.MapBuilder<>(new TreeMap<String, Map<String, ?>>()) //
						.put(CheckRelayCount.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 1) //
										.build())
						.build());
	}

	@Override
	public String getName() {
		return "Blockheizkraftwerk (BHKW)";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

}
