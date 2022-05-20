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
import io.openems.edge.app.heat.HeatingElement.Property;
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
 * Describes a App for a RTU Heating Element.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatingElement",
    "alias":"Heizstab",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEATING_ELEMENT_ID": "ctrlIoHeatingElement0",
    	"OUTPUT_CHANNEL_PHASE_L1": "io0/Relay1",
    	"OUTPUT_CHANNEL_PHASE_L2": "io0/Relay2",
    	"OUTPUT_CHANNEL_PHASE_L3": "io0/Relay3"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-heizstab/">https://fenecon.de/fems-2-2/fems-app-heizstab/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatingElement")
public class HeatingElement extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		ALIAS("Heating Element App"), //
		CTRL_IO_HEATING_ELEMENT_ID("ctrlIoHeatingElement0"), //
		OUTPUT_CHANNEL_PHASE_L1("io0/Relay1"), //
		OUTPUT_CHANNEL_PHASE_L2("io0/Relay2"), //
		OUTPUT_CHANNEL_PHASE_L3("io0/Relay3"), //
		;

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
	public HeatingElement(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var heatingElementId = this.getId(t, p, Property.CTRL_IO_HEATING_ELEMENT_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelPhaseL1 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L1);
			final var outputChannelPhaseL2 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L2);
			final var outputChannelPhaseL3 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L3);

			List<Component> comp = new ArrayList<>();
			var jsonConfigBuilder = JsonUtils.buildJsonObject();

			comp.add(new EdgeConfig.Component(heatingElementId, alias, "Controller.IO.HeatingElement",
					jsonConfigBuilder.addProperty("outputChannelPhaseL1", outputChannelPhaseL1) //
							.addProperty("outputChannelPhaseL2", outputChannelPhaseL2) //
							.addProperty("outputChannelPhaseL3", outputChannelPhaseL3) //
							.build()));//

			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 1, 2, 3 },
				new int[] { 4, 5, 6 });
		var options = this.componentUtil.getAllRelays() //
				.stream().map(r -> r.relays).flatMap(List::stream) //
				.collect(Collectors.toList());
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L1) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[0])) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannelPhaseL1.label"))
								.setDescription(bundle.getString(this.getAppId() + ".outputChannelPhaseL1.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L2) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[1])) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannelPhaseL2.label"))
								.setDescription(bundle.getString(this.getAppId() + ".outputChannelPhaseL2.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L3) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[2])) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannelPhaseL3.label"))
								.setDescription(bundle.getString(this.getAppId() + ".outputChannelPhaseL3.description"))
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-heizstab/") //
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
										.put("count", 3) //
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
				+ "DvYrbSTXeGvWLqeZH0N49Y2rc5G1vZZfNuQPljO6lf30RJUemCbhWabJwcYByooRwYos17Xmeip82Yvot3Ku5rED6EIHRhzyAHBV"
				+ "VX7kEHhRVWR/leImtEtXtg6+F9wy8dyeAHWW5ZS0Cx0Fmi3r1mnXrr3IbVj/SY2AlvI8Atq3+6v+b+5cW+ky0pvt+mAPWNrJ31Ke"
				+ "2BqwzZZxlEJ+pialnodsP6mVvPUA1XKAGWTLGG9nqcF9kBvE9cfsNp7sy4dqNo6LiHxclIN5yZDx41W+596ghoyYWxQrh7olofVC"
				+ "za7N55+Qb3ywGOqa9fuSBvwLFVeVvzjv99W3PiSaY7VIu4YVkMPZJ0u66CO9bY2Jpnzpsd0UFqRWswTQSD96uVyqIaPNBj7KRx8h"
				+ "ipMO+3ddKlzHRWnhVqlOgYPUlNOOkaNPuzA4CrI1DjNwMudXPfeLvXaQ0+qtY8/Q3eFePuhh2XK9y/1Vyx5Mio3M3xocJiAXjYBG"
				+ "2lvs43z0VSUEz/NSNI8saCVHousPuLsOcD0+qx6kGpc3pzFx9BudAOJ54Qa0rW1qQnHHK4mHneE8trbSUO3bkFN+Be/4HaPBJTCG"
				+ "taTFPTI9j01gcSgltuG2tLNkOsDmSr2bt2mXr7vYbVp9auBLqCo9pHJN5TfdaW/4vqNkdZRCi9D09MbPa1NLefJ51jp2ViKycYky"
				+ "1zHpgdxdbF4cxePo+afp+YXUt7zlufoaVPV1JOPVR3Dh1CJh1kOPKXqsfWV19XL9z6sdm7cRCX2XcRmdaXavv4W/95bu0KdI6Ks2"
				+ "GhEqhtJjUu15qmffRwI0Ek5LTKQ6rkWRe+cT3Sq9kEXUbdXUHlkqFZqyNhRauqpx6thk2gfS4t+VOpA0ffVhpXPq1f/8Ljq2l3yf"
				+ "ulTVHupf++3f6W2b4hUTYMeRzI+GJuNzR7Y9E0HJpkH+nOeCBZb/3odl6U+TgZsD7hOItuowvjpqnDMuy4g6UtUPCzUKtq7Bqopb"
				+ "z5ajZk+LfprXUhJY53ee78EQ4HNTtcHZYOxpt+3Z6967cHH1donn1NFPN/qNb+NdrUF/vIlz0blPIMD1hKYQqcVqdU85RkVJwOUk"
				+ "/Qyrzp0+zeT3NNGVTyPe/K8NjX+qMPUxBNmqrYOPKeSwzPJjcnuTVvVy/c9pDa/hI8h9hzGvSR+TfldV/l3fGkz7HJKTeKpP2jsK"
				+ "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"
				+ "yI//8PxZkOPxySZfWXVtww8wVZHX8iGwJt92STltX2RhvRBFEOtCn7uHT/60D6k546wIq476QA1tN1TH7/nRXyRQR/+eeYEde7kE"
				+ "eovVzyr9tKVVR9IdevZB6snN+5UFxwwSn3mDy+re9biF+VDPnjoWDWL2m+iq6cNtDl98aFXgyugX10wQ922epP6ypM9b44ncsTIQ"
				+ "eqWs6api373ovr6mdPUO2jDfIP8VpW4KLDVBfqokrLtr6+Pfoas5Gvkt9LV1lVq99av+nf9i/wa1Lgeq4XsQ+8P5Zagmq9CNzJYM"
				+ "F40KTN6vY24utR4Z188yJt7+Sfp0okuXwp/Q26DdRg6fow64t1z1EHnnqLaBw8u6axX7jsEaHRteLsVQnuVen1nV8lmVVJPt3K4/"
				+ "cJmxb64Nsgp9AeSzXa6Srp/7XZ1xsRhpA8t8P8zJwxXd722Nbgiw1UavvKFLqToNrNNPbcVnzGOfOJeLgLtUeY6/j82zofW71CPb"
				+ "dqpXt2xV51FV4cMLAaT/05yjpa4TT1uzGC6lQ2/4ivwSf8Npdtb3ILCfvyg9sAGt5lhD2RHY2MZlMg4zU2V0PMeQNnQifupGX82V"
				+ "007+yTV0fsHkOG0Uy9Vg0Y8Tuv7DvwKEQEP7NXkWdfHYWqvo+vT+m4K8rJhRZEW5JwYLKgsA+h4oW15FgL/hYkzC97cxe+hCH+Ci"
				+ "ldRGgp9B11JHUyb1PR3zVFDxke/+Uct5KB6ZakNwT/8ujZ4DtMz0t5nQAG0UfQ+p0FNrwyJEwjzohpEmw82pLtf36pOpw2qLarBR"
				+ "jCTNoS7X98W3CbiWRhqsDm+sbtLHT92SBBkgU/xDA79oxzoezThhnXnmvDL9+6kTXDWlBGBDFD/iaMmqouOnKA+fdwU9ZO5h6svv"
				+ "vmAIH/3gaMDD/hv6ckHqA8cMlZ9ifIfnnuoWkL5z8+brk4dFxzuwE/YZ0gf2VapgfHjK6WPfN/b1CT8shBtphGHUPpF4eDTl9F6H"
				+ "0WyPPyQS5aD4F50O8Ykow3Lsl6OWOpbgrxsWIxpAXnhZQ7iAsLkJ46iN2fxcYWj3vFrav4flA6CB482gcknHKmOooDn3/wrC1szo"
				+ "T9q9GC1+NjJYTpuslp0zCQ1IKgPp3PQ8E518+nTKB3Uk44bg1/MAYXgCgtXYPfSrSAevs8YNSioOW38MLV+V5d6dstutas7vMJiv"
				+ "vPMevVWuoX8+XmHqyuOn6wumDpSje6wP4+bQbeDE+hq6S7aFHF87lyzRR1LGx42xV6K6h0HjFYvb9+rLrh9ZfA87SfPv6H+fsb4n"
				+ "mDGhdxf0YaFje+8259S55Pd/Wu3qgvJJpE0KyttSMZXS0+mW/gj33uBGn3QlKgiYDYdu4foautmb9Zl+1GZVwR5eOBDNI89QDbZp"
				+ "CGrfVOQlw3LFAT6gnJZz3VseiPenIXjKGC/SbvRA9T0rEitxhyyvzrqLy9Qk048ShXawxNSOoZsGgjr4+pArxxK2EzW7dobpa4gS"
				+ "Xs8GP/FixtL0pqd+Er0kE7aiHZ2+Wrz3m71+MYd6gy6ygJ4oH/Pa+HzrN10BYYrLObnq99QH7hjlfrpC2/QptOuLqVN8udvOVydT"
				+ "psc374BlnF19RxtfN20MY6mTQq3sVv37FNnTwr74rE8uH67+t6qdWq3jz8QFNWvXtmsRna2BZsdc/tLm9R/v7QxuNLrph0Mf2A4d"
				+ "MRARftuj59Q6i0F8JZSAo80QtoIuXPEUHXw3DPU9Lefqwb3fkyKFrf4T6ptwDMUBxd7sy7lr/7ROi4p63U6qGcbvV1S26YmLxuWH"
				+ "mKcGJZlHidz2Yp31sc76HZggSq0PUPFCym1oRl+RmvGO2eraXNOUx3DwisY6ZyRnZj0JXVRiPbRB4RabAT/Slc8SLc+s05959n1J"
				+ "c+0ttBGtIxO6uUi4RUFtMZJjr/u8QN53P6dSZsIrrpOpNus4IqIwGsLA8WGhduzF7ftUT9YtUFdct9q9Va6Gnpkww71kenjgjr8B"
				+ "48sY8M6jK6ybr/gCEoz1G1vnaFGDWwP9KG/XmAfPnAqqK00dlydDm0P3/rnC9XAJpJg04HnX5QCbTCVsH0yvZ4SIcNhk8erI/78P"
				+ "HXgmSeq9uCD6EHrUZRupMuxR+lq+3woYpDdsawPQS8DU7uWIi8bFsACpllE26KnC4ChYwsUkG9XncMepyZLSROcbXgwe9DZJ6kZF"
				+ "Mh4YCv2isqxjMw+kd6SzUaCB9gDKOEZFribrqgOGjZQvWXKyODq5SHahAA2rEHillBnN+2Qv1+7XU0a0vutp9wn/jo4kfQf/vUqN"
				+ "fe2p3rSQtroSm8LxSgjsSP6Gh28OwZwS6jPBZsV3j/D1VuAbbJVAl/ts98RhwS3+xOPni6/6ocKhf+lq63bKU6mRzrGNCoMGHrOg"
				+ "Syb2rQsedmwsLicgJ6nQdoa23lzFh3pnfp3v6KA/C8yOTTQ0YmC96nwYFb/zT8dODUNTOqBrU7mUs9InbTBldYQ/ClR6BjI2BAw7"
				+ "j10C4by6u171Evbd6t5dIt3/7rtPXpsSIPoCqu0fXg7FaYibVbtaidtfFwG+D+uol6kq8CnKW3p6g4SXmrFM7MdlOO2kK31I3jUm"
				+ "EGqizaiNXQLyWC8oX3oH8/w1lH9Ht6wDNhrSpF2kG3toG/r7FBTTj1OHfne89XIAyaFFSHn0yAfo43rRtq4cPUF2JXuUtfLXNrqc"
				+ "sttZnnZnU3zrMaCon3RO/fSsaqt/UoKwL+ncs8TYjyAnXLysWX/jFZI5cO88ZQDg3ekPvcQvi2lLyfTbd0Npx4Y3DZic2BwxTTvv"
				+ "j8Frwb811umq3+483n16EZ8VVT45vvIjgFq3e6u4O13DPHtB4xSlx49SZ3zP08Go8aD/fGD23teMsVV0pFjBqsf0O3o15/qfb8Ss"
				+ "/vp3MPVr17erG5ZWfK96wFXnjBF7UdjwEuklx87Wc2dOkK9RpsPXizFDR5uT1e8sjl4CRbcfNpBavqoQepl2ljxWsWQAW2BzTefX"
				+ "Ku+t6rke6/qztaXXwve39q1CbfRPWu7geTPqL27vuX/9iZeALnwlcotA08qD+hzlYuahj4B4Z360XY1dL+PkYjf/Ov5kN/g0SPV/"
				+ "qcdHzzLaATwasFe2jSe2BRuNiYOGT4weCiNCeLGD7dXuAXEC6J4kI5XGe5fty141mWmqCYO7lAzRw0OHnCDk/YbqqYN71TDaWPDc"
				+ "zA8+H960y71e7oqk2cR3qs6a9II9fCG7Wr97r4viU4d2qEOGzFI3UF+sWGN6mxT31q5Tr2ZNtrhtHHioz7LacPaFzgt0oY1TT2zZ"
				+ "VfwV8LjaO54rraSNto7X+eXXe1Lb69JIn1L/Mz++idXqVcf7PMzZI/T/ex8//7v3qG2vp7kUNbHyS1FuiPc/OjzTAqGWAqj91eFE"
				+ "97/FvLwJSrNiNThz2ideLQaO4Nu/QxfT1xRp5XSr51XD2xYYwYOUAt+/6dI0xdcYa2iDevG6CNCNaWC47pvN36G7Am17qlVtImV7"
				+ "C3/pXz/Mn/FkueicqWUOG9m8vIMS4dDTC5kmkUtenMWHV540/tvo9u/26kcbFbYnCYcfbg66q/epvabeQgdVRzWXne9T3EYfnrTK"
				+ "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"
				+ "AAAAElFTkSuQmCC";
	}

}
