package io.openems.edge.app.heat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

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
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DefaultEnum;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.Checkable;

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
	public CombinedHeatAndPower(@Reference ComponentManager componentManager, ComponentContext componentContext) {
		super(componentManager, componentContext);
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
					throw new OpenemsException("Not enought relays available!");
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
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	protected List<Checkable> getCompatibleCheckables() {
		return Lists.newArrayList(new CheckHome(this.componentManager));
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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";
	}

	@Override
	protected List<Checkable> getInstallableCheckables() {
		return Lists.newArrayList(new CheckRelayCount(this.componentUtil, 1));
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
