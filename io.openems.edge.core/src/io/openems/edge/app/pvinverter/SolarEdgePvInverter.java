package io.openems.edge.app.pvinverter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.SolarEdgePvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;

/**
 * Describes a App for SolarEdge PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.SolarEdge",
    "alias":"SolarEdge PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-solaredge-pv-wechselrichter/">https://fenecon.de/fems-2-2/fems-app-solaredge-pv-wechselrichter/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.SolarEdge")
public class SolarEdgePvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT;

	}

	@Activate
	public SolarEdgePvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "SolarEdge.PV-Inverter";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(bundle.getString("ipAddress")) //
								.setDescription(bundle.getString("App.PvInverter.ip.description")) //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel(bundle.getString("port")) //
								.setDescription(bundle.getString("App.PvInverter.port.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-solaredge-pv-wechselrichter/") //
				.build();
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "SPvnbGnBzBxupv9TyeacezfcqgrOCg+UyXwLKt4Ea19+if0l182qnMgn1q3mU6zHukDW7rV5JsmritMmrzSpg9cfq48Qr8zzh9Bb"
				+ "UMuokJxPjujr+oEXit5Fr+AZ/H4HFo1SPlyRVLD9Wd03aQD6aMm+Tyc8ekhNGgo3vGOT0TJp9fBHbxanE8rlsj3AQXJy8RlF7LEq"
				+ "zStZkCXrVZ683PUuS00bp8P8sz9q1z0yaroT7N7ET3w6/+mV55AnaqlFmk0Hf2nobOT1LlF17YkijSvkzsZXcZRPshH+fUQX5P5I"
				+ "XV3f55WXikf60yLWYZa6bWg2dOrJ7Uv67zOEznJq1g7NkweFDbwH7yvUtfOb9DvrtkZGeNIWy7JIFf0l85TKVI/3cii9/SY6JiNj"
				+ "s5jw/0t4k7YwyZO9uu0c+vV9Idr8YmrWpO2s1aK7TzVk6x52MLXo5xp0kyf79yF06il5WusvY+jRHGKXazfQN1dF9PKpfI5sVqC8"
				+ "uWOWjd0M6HrVkmnRnhpdK2XbMd9oECjJr2X+yA++DktNAc8x6EW07O3/5yewfczy/J36XFIuDTppNGbgUrKD+B2hTFJG64+nHn+M"
				+ "GobjA/K4uvPw1XRbwluU1ixBB9VrTVSTxxzR2MbsLHouiV1Vt3ALt1GmO7cBcOopQ0dE4KvPAu3cJD5tHwJPlaq05F0pUy2/JpJx"
				+ "9GTlkPOKdCk2e/hP2SX8+mL/pAFpxJf8F5Mz//5Z/Qkvr1aV6QNc0XeO6LUTy46ffHpizItOh1QrgdT/1bMtt4buEPwGfMbONgXe"
				+ "ODC1F/HESSspK/RtnrpAO44XcI3mr7MOzvBVgEtY+2k0BCcvs18/Drt3rmMfnf19tBed6T9ckX/6QjZcdVNLgB9QabF1QnK0+voP"
				+ "DHa3zo2NAVsYPkKFbu/SSuuxOZq2vylvCCLjiOwhak1cWUBSX7V6CbwE7tLTyJL2JCORZM4ymUc64N8bImK2M36j6i763O0cik+9"
				+ "V8JrrK46iZ6UIC8ka1R+he6brZGt9nSYHYIrYPQferHW2jIqA+xfhkLd+YenqJisZOevePX9MztcWk1k46jx8bpnx5C7cHtLhey6"
				+ "Ifl7wxud7nrh3fTxpcjU0OQ9pI2zBV57oi6bvoCFBpzIZ55wQhqG3QRZ4VOPURlu4L1BbxMxA2CUg7d2Uwb6CvdY2K9oRinDDcU0"
				+ "0W05pGf0MM3BYY+oi/zrht57pC2ukkjwk/rWTDjwR2nh+E7Fs3klQJ37uK72Cxh8AjGd3nJcCkvGV4LTb1ISh+U52W3u+IBl19SX"
				+ "K2DNOFsceL0pLhA+yURl2a6NMJHtrBPxaMWCKJt5RnVlVTsuoJWLsPjW1lx5V+pHcfcYatwXtB1k8bTDSl61nMQF0f79dZHTSQ6/"
				+ "oPcyYPOrh9yXc/yRdqx5dt0y7VJD7lKuYFNd+cfEhceiB2YYQY2cxZMoFb9UHxwavi/4k95sMJD8avCgE2BtGGuyHNH1HUzL0BQy"
				+ "cWow1eun/apFho89B/YyZ2fJgS2kMc42AJ65o7fRvdvgcrzqZ0+sDn9vHZqH/Zp1r7AMjqwBRTu4dOE21Z6GqsJkHbDMXfkuUNWe"
				+ "uG5wsd1AN1B0ughHZ17sJUvggIuhvbQGHATLy0W0oqlT0RuTzk4j7Y2qi2TDyE6+Oy38fJvCWe5f6kJCy/xjOrztGHNv9PdP8ZjW"
				+ "XE0pqy9Ke9rOaEvTmSj0HUzG096Hsh6DnQ83RmzdJDy/Ds69+OL4krW3xaaAnDrw7XU3fVlWrn0jdBkxSxDrXUN7MD0k/Da35VGH"
				+ "GZ8E1sewMwvTgfJ4Ts6Z/P/WLrPC6whuIfqah6svsbLv02sx6UHtN20AVu8Wum5RCqYV2yN2WyEZQv3tzpY5YukeHDohSIX1/F//"
				+ "0Jb37iebrsem/S2OtVbHzjMmT+OWtsuZe2fWdoCW3gufskD1WIeqJ4NTXUn7vynaTPouSPPHbJedbN1BOQldq0n0buMp3+qjdqH8"
				+ "sVSwEUzLjQGPMiygO74/u9oc/BON90540gbDuiwWeLVC1cZqimbvY6nnddGg4d9gp3/wuaxoTnwfoCPuP3k90G47FRT1mpAvrmjL"
				+ "05ko9B1s3fSynClJR3ETNuVX1w6BZqzcCy1tvLFQx9nGQSPiF9xkE6+gOT93lnTd5EmPOwgKS0XOr6ZH0ibrsQ10wNxaeg8Bbwy6"
				+ "M1sXsrqbJXUWtYvoU1rv0d3/htmtkDi6nTMNF1+pg7gThNeI/a4eLnFdkLygqturobuK+LL0LFoNhVa+GKis1TQHbw8uYaKXV+ll"
				+ "cuwl9LsuM55Vr22dCzanwoYqOicMAtkFewdfivcO1yGx6mykqa8tjCuOouOI8iq5wpULK9I3XTD1aK+te4Uupy28hVp35MKLHxRE"
				+ "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"
				+ "UT/H4bIXidi6LlUAAAAAElFTkSuQmCC";
	}

}
