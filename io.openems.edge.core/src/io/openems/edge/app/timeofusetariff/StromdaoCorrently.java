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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for StromdaoCorrently.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.Stromdao",
    "alias":"Stromdao Corrently",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID": "ctrlEssTimeOfUseTariffDischarge0",
    	"TIME_OF_USE_TARIF_ID": "timeOfUseTariff0",
    	"ZIP_CODE": "12345678"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-stromdao-corrently/">https://fenecon.de/fems-2-2/fems-app-stromdao-corrently/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Stromdao")
public class StromdaoCorrently extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID, //
		TIME_OF_USE_TARIF_ID, //
		ZIP_CODE;
	}

	@Activate
	public StromdaoCorrently(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var ctrlEssTimeOfUseTariffDischargeId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID,
					"ctrlEssTimeOfUseTariffDischarge0");

			var timeOfUseTariffId = this.getId(t, p, Property.TIME_OF_USE_TARIF_ID, "timeOfUseTariff0");

			var zipCode = EnumUtils.getAsString(p, Property.ZIP_CODE);

			// TODO ess id may be changed
			List<Component> comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffDischargeId, this.getName(l),
							"Controller.Ess.Time-Of-Use-Tariff.Discharge", JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffId, this.getName(l), "TimeOfUseTariff.Corrently",
							JsonUtils.buildJsonObject() //
									.addProperty("zipcode", zipCode) //
									.build())//
			);
			return new AppConfiguration(comp, Lists.newArrayList("ctrlEssTimeOfUseTariffDischarge0", "ctrlBalancing0"));
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.ZIP_CODE) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".zipCode.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".zipCode.description")) //
								.isRequired(true) //
								.build()) //
						.build()) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-stromdao-corrently/") //
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
				+ "JA2X0lr5gfi8tBlCnhl0NvYvITVWSqrNaxfShvXfI/u+lesbIGk1fmYebriTB0gnMZfI/a4dLnFtkPygqttro7uK+Lr0LFwFhVa+"
				+ "GCis5Trdj49uZaKXV+lFUtxLaXZce3zrHpt6Vi4HxUwUdE5YREoKrh2+K3w2uFSPE6VlTT1tfm42iw6tiCrnivQsLwibdMdV4v21"
				+ "npQ6Hra6lekfU4qsPBBRbgwv39gDXmVJ64v0PpX/pXu+REesrbl5dKFLL75YPb83Xj1egk373wO6V9nb6Ji90JaftVTUbie1Hv/I"
				+ "v/ckc8BGeJqmwwU6dAs+8A1yLTdpuuyRE+LpCnQmZ8dRG2DP806H2xlH9i8j2UeLbvytjBYc0p1CLHp0i6xNx/Hf7iVRu3xca7il"
				+ "7i6u0dWpvAYh+fTnTcuy3h90EbSfmoU0h+5onkHV/XUs21ZB58MHqQxB1L2es6Ztzu1tPFBV+CDr9gaZVHk1dZ/8uafeYXwYuDnC"
				+ "Rk2hujkj5/B++lq3j+Hql2+jsOX0cbXv0t33Zj1M1p9gR53Ll0wx1kuMBuZJ3Tbkjo3C6680uoasSdhz2/uIj74gnuFzgzCIXg18"
				+ "xLq2nkF3XI1PkOm0w485izch1oKeED53ZEF4CL6d6h712W0Yikei+oL6t0v5ljLBXkeyI1smzn4JCyDJq0OXGnNeFCkI84r0O578"
				+ "8GIg5L2Cc0Bqzj6c7R1/Y/ptutxN7bOMytSXv/hjAtG0qAhn2Pts9zsIaoJy3glitcT82lgrqm0r5ua/jcQ01OPtslkoQeDhKW8L"
				+ "AOldnU87ZNDaPAIPjgLOEj1e5n+RMXueXyaeHcUzgt6n5c4/iMtNGrCRzjqqxyaqNye4olqIT36PzfR6mo/+dcvyDIO+w21O2CaD"
				+ "z2BaB0grPW06LxsmIMkrg4gbdlmerfesXAiFVpwsPJB2/MZMlzf+jFvL+aVRdbPkPUf5naezH/xpe6jQkMAHmv6Cm3b8C3643fSf"
				+ "EYrL+hxlhvSHjD9EVfb5CCXDs2yD8wJwsTM1+Zj4ipflwXMsl1lhXpH59EUvqYZB7GwieUK6u5aSiuW4J1NcXkAM9ycdHRO4xric"
				+ "+/v4apKXbu4+t+j7l2X0oqr10S2gYT0Xa5o7oFYHfVqm+sAr0avDweeVaAph76Hj2F8GGN6aAx4kVdc/8ynR//Jp0dSj/7HmRcMp"
				+ "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";
	}

}
