package io.openems.edge.app.timeofusetariff;

import java.util.EnumMap;
import java.util.List;

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
import io.openems.edge.app.timeofusetariff.AwattarHourly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

/**
 * Describes a App for AwattarHourly.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.Awattar",
    "alias":"Awattar HOURLY",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID": "ctrlEssTimeOfUseTariffDischarge0",
    	"TIME_OF_USE_TARIF_ID": "timeOfUseTariff0"
    },
    "appDescriptor": {
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Awattar")
public class AwattarHourly extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID, //
		TIME_OF_USE_TARIF_ID, //
		;
	}

	@Activate
	public AwattarHourly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var ctrlEssTimeOfUseTariffDischargeId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID,
					"ctrlEssTimeOfUseTariffDischarge0");

			var timeOfUseTariffId = this.getId(t, p, Property.TIME_OF_USE_TARIF_ID, "timeOfUseTariff0");

			// TODO ess id may be changed
			List<Component> comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffDischargeId, this.getName(l),
							"Controller.Ess.Time-Of-Use-Tariff.Discharge", JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffId, this.getName(l), "TimeOfUseTariff.Awattar",
							JsonUtils.buildJsonObject() //
									.build())//
			);
			return new AppConfiguration(comp, Lists.newArrayList("ctrlEssTimeOfUseTariffDischarge0", "ctrlBalancing0"));
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIME_OF_USE_TARIFF };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "Gnj6Ny5c8SRU4n9n5069+HvFq67ODU9sxQftbgPolvLuuL+rfYNa3ha+YcGGFVwoeW8yBSVDXzufdrCdewZflR18ESCqIJUKab0/"
				+ "jv7d/ySn/59nZBwds/hw4edLVu2zOs2KkhgbDhpKucHRXneuSY/V1BRw+rccusoVtqh04CdTs4NuGw6BqUMgQu+6hMTG1tx8cY9g"
				+ "8Z8Oe/BfzbGt+EG4pwQ3gBxe9+L2Kt39M/xGbLCRBewZFZs2y+uUy3fup+XxDThM1pJ13VrNYcHqrcrlSu1ZuPGjVkjR44syvdTF"
				+ "QawcmxHSTwllJHt/LXDQIDOgLaoB1Q2Ojm3YFvah1KeOXMm++uvv1j16tWT2zSpt/GBq7rPql0lNm3HwZONTiaeL83jBFu7+7Djk"
				+ "9krWQgPXhc1qsHCQkOVAcRqUKFYbRPI5uDJRDbyg1/YIx/9zg54jqocjpDUbi3rLZ7y+PVjR17Z9W1neureOXPmOOE9+gcPei+8q"
				+ "8BtZChm2Hmj0bkVpINi3zQQqWQAywU1Fr/A9/Duu+8+VqVKlYiMTGfHMV/9ee+kmcsHnElKwVc/OBpUqyA+Q3Zll+YiiNCAk5uA5"
				+ "Q+5P1X/KekZbPyPi9kbPyxgSSnpXCNWn7NJzUo7R13T7b17L+/8w8KFC08nJCS4rrrqKrquiwPUF3Rybilu68ISeV0pRZmCmJvKC"
				+ "aiTAVAOxllU9rRPWVYtJyh5xIgRcCsElCN3Hjp5yZiv5j3185JN3dIyMuHWeNCz3m0bsgn3DWat61WDYqHDYxebvhg+9z6L7Tt2F"
				+ "jQ8OZzwGa17Bnb65ZXh/d87e+bM1k8//dT51FNP4RwN2dhynQjntCm6uQnPd4s+slXQEaCdTgZ0y0E7BPWFCjyfeM0114gvVpctV"
				+ "67ygnW7bh7z5bzhSzbvbcarxaepw0JD2N0DOrGXb+vLKsWWAZXloywrR086Vu88JJ77+2djPC+JNq7I8LCU67q1WvDq8P5vVyobu"
				+ "Xzjxo2pkyZNck2dOlXUo2ExQjdmnd8Ei+xntqC4beRgKMi55cYZYDzYDuULuf69y//3339Zu3btQiMiIupOmrXizgk/L7511+FT+"
				+ "NOhI65MKfb8LZexh67oKt41n1d0wevomXPs+alz2dQ/V4k71vmiXdwqq2ebBlvH3tr3ve6t6v2ydOnSU/D+9HvvvdfdqHhDfUAn5"
				+ "xbow3bkdaUUZQp6bv6cCuvQaazIlGDtKcHaC+AUEU4VuRiekpZxySvT5j/48azlfU+fS+GHVhA3HKxprUriaz4DOzUVHecX/FSUv"
				+ "TtjCXv127/FM4AenHx5B5+8vseXd/TrOJXHt71Hjhxx3nzzzRf6e3/FBUvbvbiRn35X1NDNDTYk1NGd2ipWnQD7V/VNlx/MsgsFe"
				+ "D7xqaeeCuFHXGXjj57uM/rTOSNmrdh6SXpmFny6R4x3wEVN2Nv3DmbNalf2OVoK5rQPAPvflm9lj0+eyXYfPuXRivupkkYM6jz72"
				+ "Zt6vxsewtasXLky491339V9RsugxqqvFiuK3A6Tj+R2btAONzaVVQTrFNhfUVjvqjl65XXr1jnq1KnjiI2NrTh39Y5bXp3298jFm"
				+ "/bAYz4hYBIeGuq4f0gXNnZYHxZX1v9n9gE5mG3Yc1Rcp5q/dpdHw1z8dDPp6ktaLnv1jv6TGlSrMP+3335LmjVrlmvy5MkeE1uT3"
				+ "36B29RWFIUdp6BQzY06RW4cBNugM/hrT20CyUUSz7OJLCMjIyI0LKw1P0W8+c0fFl2z99gZuL4VCsOvWM79GbJ7BnUS928F4mTCe"
				+ "Tbmq3ls8uwVLDMLXk8sVmFmj1b1dz17U6+3+rRr+GtiYuLpTz/91LVz505GghWsK3/ruyhCx1zY4y/SvpVbipsDBIM8N3QYf46jq"
				+ "6N6dATsC5BlQNcXEqi+yABvO3311VcdzZs3j0pKzWj7whd/PvLVX6sHnj2fCp/ZF+/falG3Cj9NHML6tW8ExRzAu9M/+H0Ze+Xrv"
				+ "9iZpOzPaPEjqYOPX9f9+3sv7/xZwtmzu/mpn3PGjBmuDRs2eExyjW77lBRsOd9iscPkEnlueQ0Q1Olz4wy0Pcqq8eR1nFahYwHoc"
				+ "pVjgI++PvnkkyHVqlUrv+3AiX6PT5457M9VO7pmOp3wTI+wv6JLc/bWPZezRjUqek8D5/y3jZ/+zWS8DZgA8Ln3M7f1af/TuDsGf"
				+ "FImKmLD9OnTM/766y+7nP4V1jb0B25fW3GhV2pBUpBzs+KQ/hwG2hYnh/KZ6wMPPAD3bzmqV68eO+XPVT3G//jPyM37jnXhVeLGU"
				+ "7j14eGrLmFDu7dmL/LTvz/+2y7a8Sm7wkNDkwZ1arpg3PABk5rWrLCQk7ZkyRLXN998w3bt8l7PMuQdE7CKGbq50WBjJfDIYBt0C"
				+ "JVspc/cLDu30DHmCy1atIBTRPb55587okuXrvn2T4sfeHfGkpsPnkyozqulb47xQy3myGrfsMaq527u/f4VnZvOOnLkSOLEiRNdb"
				+ "731lsemQMn3+RcDbDnfwtphLgS6udFAQeXcEKxTWBlTYSEvM9gxeO1HjBgB93GVOn0u+aIXv5w3cuqfqy47n5oeB3UcZ72qccdGD"
				+ "+05895BF//v2LGj8du2bXP16tVLXndYlseg0+cXdN5Upuj0gZD7BgpqHjK4PEMxARyjKCVAp8PcikzxZxdIDgZsR5P4Sg+mL7/80"
				+ "nHs2LFQl8tVcX384VuHjvt6bYXrxh555MPfvk5MTu3B9eU2bNgQAq+6oe1KWPJZZwWcDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDw"
				+ "WAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMB"
				+ "oPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwG"
				+ "AwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGgyFoGPt/mgLnklMte0wAAAAASUVORK5CYII=";
	}

}
