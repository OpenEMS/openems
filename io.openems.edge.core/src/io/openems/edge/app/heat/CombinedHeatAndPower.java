package io.openems.edge.app.heat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
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
import io.openems.edge.core.appmanager.JsonFormlyUtil;
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
    	"CTRL_CHP_SOC_ID": "ctrlChpSoc0",
    	"OUTPUT_CHANNEL": "io0/Relay1"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-blockheizkraftwerk-bhkw/">https://fenecon.de/fems-2-2/fems-app-blockheizkraftwerk-bhkw/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.CHP")
public class CombinedHeatAndPower extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		// User values
		ALIAS("Blockheizkraftwerk"), //
		OUTPUT_CHANNEL("io0/Relay1"), //
		// Components
		CTRL_CHP_SOC_ID("ctrlChpSoc0");

		private final String defaultValue;

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
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			final var bhcId = this.getId(t, p, Property.CTRL_CHP_SOC_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelAddress = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL);

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
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL) //
								.setOptions(this.componentUtil.getAllRelays() //
										.stream().map(r -> r.relays).flatMap(List::stream) //
										.collect(Collectors.toList())) //
								.setDefaultValueWithStringSupplier(() -> {
									var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(),
											new int[] { 1 }, new int[] { 1 });
									if (relays == null) {
										return Property.OUTPUT_CHANNEL.getDefaultValue();
									}
									return relays[0];
								}) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannel.label")) //
								.setDescription(bundle.getString(this.getAppId() + ".outputChannel.description")) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-blockheizkraftwerk-bhkw/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new Validator.CheckableConfig(CheckRelayCount.COMPONENT_NAME,
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 1) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "PcRTLyrfb+ypLpmFT5t5ehLcJJCsX140+Yp79DXvM7zPHxP8eBQC/ynqQSf9Deu+071lu/xtw9t9EtZDdT1tTwyECdtIJH11RtPD"
				+ "h5Zz4uMJztjlg5Slz8NXAeRZhWJr4t0AI8+fFVVK5+trF2Fz3HZ0MvQalkCPdDD2F6G29KIQ4+vY8oD6PnFySDRntrkEFrEY+m+M"
				+ "NCG7KDgy5Tvf6GyZiU+SxaXHpB6XQdM8Vol5x6uTJEwNWanEZQN97e849/VQ8tEXCSHBiGEr/znqOD/pra9+I3K9d/APkmmOrVbL"
				+ "gzl+YsnqnLXBSR+gBw+6ApwLn6m/MryyppV+ER+fxB3/tO0GeRcU7TO1676mjoC8mK9lJPoU8byqR8d4g8d/gEauHDRTBTJ3kF/F"
				+ "ldv/OZv/S3Bnm4ISFPHtHZA2maJ1y5sZWimbMY6lk/5yBDVPeKD5P038k6ALuJ2+kdjcXX1ikbfpRmo84h8c81AnLSBRNbX2Ekbx"
				+ "JYWdxA9bVt+cel45flLJ6hyCRfPv5K3KwgJ8H9Of5ZVVq94OPRnTt9GGnvoQVJaNmR8PT+QNl2Oq6cH4tKQeTJ+eeHZr6LjxeTEh"
				+ "0r99fTnfH/zc9+s3nQVZraA48p09DRtYboM4E9jL2F9XLxBganygxlbfW0NPVDElqHUs/wQz/NwMZ0RagJ2+r5/uefv+Xxl7SW4l"
				+ "9Lp2M55VrmllHuWHUyLcTq3Hr4YzeyiLL8S3ju8OPoYSSbSlNdkk1R/HEFWObegEkWC6ysbrhXnoNWdQpbTVD7fO+Akr3TAibioV"
				+ "vnKmy2MnlG+/yl/49P/Wb3lu3jJ2pSWTWay2A4KyvOXjFPlIfiw7IfJiV9n1a/ofC6trFmJz8O3m3af36BCeWZQdr4YbPXljsINm"
				+ "uW82DqZ1JtkmRfLaeE4Xvn0s7rUkGEfIRkXm/zA5p/JLaqsvui60NtyamUIfGaZ68X6jqN0/LvL3pip7yPxM+T2CpQh9+A+lX/jt"
				+ "1ZnvD9oIuk89RfcHrmlYztSm2hnfbN2Pu48iKN3pMzlLC1YvJdXGkIXnYeLrxwVh645/799VT27umYVvq/oYEaMV+WT3n8anSPsE"
				+ "Ht4qAzAGwYX+JvXX1G96aqsn9EaCGS/s8mM3s9yR+YLI+fI+iY1bhZsaaWVJaxPwpheeeFyXHx4DOL0UBmot5PJxapSuahyzcX4D"
				+ "JmMWzjKC5YeoEplvKD8RnEicBP966q6+4LK2kswaA0E7W4XpJ9ritZp+7O+eudjP3eatDKwxdXDgV866i2et9esN5KMi/KAQBvyJ"
				+ "LlPqO0vfb9y3RV4GlummRXOLzeUTjtrtNfV/Qkq+lnklZ/RWk0zUWxPfE/kH6w02tYdQ+46XZO0o748WMjOwH7OL0tHaVkZS6d8e"
				+ "JjXPZIuTo8uUiX2ZVJ/VH51UWXNypsj/2BBnvMapeP/qeSNmfxuCvo8GUwVBg/ghnr17v/9lb/uzkg1qMnSDzuSll0cOUEOIFIG8"
				+ "Es5LTItE3oniSsDSJu3Ht8ql3uWTVVeCd+8o4vWL/kUTAY+8X06nkszi6yfIcsNtEQ+iQ74jNYxoQb4L5H/c2rH5q9U/vDvaT6jN"
				+ "ViQ/SyXcMcuCrb68kXODZrlvOgDhI6erslGx5a/zAvoedvyCuRyz/KXqfAzZLiIGewtfpGqVi6prF2FPZvi0gC6vyOhuu7re+oiG"
				+ "prfSt6grFTwCgnf9Kt7zq+uvRgPgRYNbrvc0tGdrg20q762C7wZuS2U5p7hqelHvNXzPHwYY79QG/A3mnGd7d999X/76+7icuSO8"
				+ "ulnjVRDhi0ncQm56GtFAb+jZfBiWgbfHvmLSG7blWnbhdGhyPqi8djPcqMDhozXbKfImndDlE/72HDVNXwpDVLLadY10vN95XvBU"
				+ "vE6WiwuomXibZFpLigdckaJBuK30+n7AtVhumiQR4I6PnLjT6sPXx9pOxrui0DKraDZvjngtPJk5IG09W20o6SJx50Gdja5EThvW"
				+ "YZEmZZO02nAwheI/5F1BH5B/E/fr36qumYlfy1Bxu8oyj3LjlNe6VIq4PGigHg96Qv+7m2XVX/35f76jFangzbMNR3ZAdtIo/VFP"
				+ "Ftj84XM4ZwH64HJxiYPCKWe5cfTMhH3t44LNQGbqGgXqp3bLq9c+5Wdka5jKPUs24cG2wtpXvgOeKPTTIOt/x0SPlFZvSLLp2m4j"
				+ "QYz3M9yy2BvIB1Z33Z2UE6bO4hNBuxvV1mSqOVdOuE9nho9+Z0eBinl7QNdxMO0YFymHr7x552wrCqdSsvZocMWUxnPpoKPitTgB"
				+ "lr+LaquWXFr5M8jsi/Y5EZBGrmm2ROQN9pRX1MnkJ0MwJ+ls5jsZZq6bMonq1yjNH/xKK/cdQ6JNCio4YEy5Boyx2fIsA8X4vYr0"
				+ "Qvfb/aVdxF59w905KgCjyvfP8d/8i8/8u9d06c+jhq5Pzf93ukGGFt90ZAcJuW0cEeIrp8AXQa2fNiOYf2AUu5Zur/yyhgc3kyOy"
				+ "4RXWP5DVSrnV65ZFbwV3B/QkvWoaMl6cqgJwGtGK/1d21ZVf/9l/TNa8vzmBVuZbf0mK3o/yx15a9BmaWd9G+kMKA/HY3kg20TmH"
				+ "5TLGzPV84571yujweJoYfAimXxWbd/01cp1X2/bS8LlnmVTlFf6HIn/BG+gDMv2A1WtnFtZu+qJUDVo0NvAJDcK0sg1zZ6AvNHu+"
				+ "sZ1Kg7jTpNGlmS1l2S170P51A+X1dBRGDTwxPyUQBlyPyW3pHrrD6/2N7TuE3zlkz/YrYaN/hgV95PkHRNqA26h5d/HK9d97Y9qR"
				+ "x72KewoMrd7p4HOWyRs9UVD6hd1WtJ2Ak7flLbMP0ve/U759I+PUUOG0iDi0WCiukNtUOxf+351SXXNyntDXWN4U+Z4pcNf/3oVf"
				+ "kbrQHFKnqKB6pPVZ+/7nn/HL5M+o+Uwg5OZazr64mgDjdaXBxMgZRNZOwWn1wltYaqjLgfQUu1AWqphUHlDqAnA0vBr/p6dn6n+9"
				+ "rLMW7SUFiw7zCuVsDXO/FATsJ3cpWr3ji9Wfnc5XiMqEq3uF9yOuaUTLpL+xFRf2Ska6SAchztDXHxpkyR3Op4aPlaVXvkBGly8S"
				+ "6jgh0V68AJV49/U9o1XVq67gj/WYIUGqkmq5H3aU977yVv7jBadiJ94fnV5Zc3KxyIdwPnhcyzlvDCQ5Ud+uSZvjd0sen25w8R1H"
				+ "FuY1HNH4LSALgNbWkxSeEdSevk7h3jjptFg432avJNCLfDvoj+LK9f++1q1s+/kqHTCe4d6oyfhM1rnk3d8qA34Cy0vF9Hy8g+Rv"
				+ "1XY2qco5L6+ubs4mkSvb7MDhOz0jXQGGZ9lU3maLWdaZFmAzDexDOX5S8ar8hAMPh8iJz5Dpn6p/Ooymik9GPjC7YlfTcnhyz9zA"
				+ "l3Is5TNeWrbi9+qXP+NwXafqr/aMA5u39wy0Cewv2lnfdN0yLgOg7h56lDWupZ7ls9RwaeyfAxKgY4qtstT/pdoofdj5QXfVqSwG"
				+ "njt58uqsvvzlWsuaeQzWo50uAErZ9jqKwcbKaeF43CHMMlp0mwk70aRZWwL5Z5lr/Zp4PKUJ2dRRFhNn/6jQvxSVf2llbUrHwrD+"
				+ "o22178DyX19++vi6BRs9ZUDhZQbIWunSFOm/kLPM2sZpH0gl058T5c3avIHScasqnafigLv8ny1uHLDlWtpCRhp+4A0gF4Gm75V9"
				+ "KlHKNZh0yehpw3aVQ8dzi+39NeJ6hQ6rb6mTs86vWPHyTgyaeMAXc4Cx5NAZ0ynPH/xJFUe8mky+XtaFn7W3/r8ldUbv5X4C2IBs"
				+ "J6zNtBf+TgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4"
				+ "XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh"
				+ "8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4HA6Hw+FwOBwOh8PhcDgcDofD4XA4H"
				+ "A6Hw+FwtBel/j8/L9gbQvsoqwAAAABJRU5ErkJggg==";
	}

}
