package io.openems.edge.app.heat;

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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.heat.HeatPump.Property;
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
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

/**
 * Describes a App for a Heat Pump.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatPump",
    "alias":""SG-Ready" Wärmepumpe",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEAT_PUMP_ID": "ctrlIoHeatPump0",
    	"OUTPUT_CHANNEL_1": "io0/Relay2",
    	"OUTPUT_CHANNEL_2": "io0/Relay3"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-sg-ready-waermepumpe-2/">https://fenecon.de/fems-2-2/fems-app-sg-ready-waermepumpe-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatPump")
public class HeatPump extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		CTRL_IO_HEAT_PUMP_ID, //
		OUTPUT_CHANNEL_1, //
		OUTPUT_CHANNEL_2;

	}

	@Activate
	public HeatPump(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlIoHeatPumpId = this.getId(t, p, Property.CTRL_IO_HEAT_PUMP_ID, "ctrlIoHeatPump0");

			var outputChannel1 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_1, "io0/Relay2");
			var outputChannel2 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_1, "io0/Relay3");

			var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoHeatPumpId, this.getName(l), "Controller.Io.HeatPump.SgReady",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannel1", outputChannel1) //
									.addProperty("outputChannel2", outputChannel2) //
									.build()));
			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 2, 3 },
				new int[] { 2, 3 });
		var options = this.componentUtil.getAllRelays() //
				.stream().map(r -> r.relays).flatMap(List::stream) //
				.collect(Collectors.toList());
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_1) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[0])) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannel1.label"))
								.setDescription(bundle.getString(this.getAppId() + ".outputChannel1.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_2) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[1])) //
								.setLabel(bundle.getString(this.getAppId() + ".outputChannel2.label"))
								.setDescription(bundle.getString(this.getAppId() + ".outputChannel2.description"))
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-sg-ready-waermepumpe-2/") //
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
										.put("count", 2) //
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
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8Y"
				+ "QUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAEl2SURBVHhe7Z0JgBxFvf9/3bN3djeb+74J4QgJVwKBcOQS8akP77+K1wOfCqJy+9Qne"
				+ "D6VAIKi8BQQNaiAJz4vIAlXIIAgdyA35D4259473f/vt7prtmd2ZrOTzGZnZn+f5LfdXV1dXd1T/e1fVVdXi6IoiqIoiqIoiqIoi"
				+ "qIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoi"
				+ "qIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoi"
				+ "qIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoiqIoPYwTTouRYj62vMWZ+nbXGXbUMHFL3ofFWSL+b6S9dbH3yC17Jd4aRFIOB344LSpUs"
				+ "JSc4c69tFpKys/Bqf+w+P478AvEELwHl84vcP3cLY27/+k9fltbEFvpYVSwCgwVrMOIO/+qceK6/4PTPgeLQxgE0cKi+RkoUmth9"
				+ "0hr43e8pTc1MlDpUVSwCgwVrB7GOe6dqP4dfQyE6kO4PP4T18hAnvWSsjKpHT1cqgYNkD1vbpKGbfXixePhVvIa4ixEwN+8xTduF"
				+ "K89DFZyjApWgaGC1VNUDXDcWRcMlVjJv+E0X4SQ6bCYE3OleuggGXTEeBl0JByu0lJp3dcg219dLfWr35DmvftxGZnrqAG2FAs/l"
				+ "Ob9j3qP/FA9rtyjglVgqGD1EO6Cq+fi9F6K2dNR5aujCMXKSmXkSVNlwIQxUlrTD06Xk6gReu1xadq5G8K1SnasWBNoFn8d338Tf"
				+ "/8svne99+D31mBeyR0qWAWGClYOcc+6pFJKK4+DEqHq53wCQeb8llVXSd3YkTL8+KOlvLaaQV2yb+NW2fL8Ctm3ZYfEWxNPDfdBv"
				+ "L4J4fqd7N++3nvyTq0nHjoqWAWGClYuqB0u7syPjhPH/SSW3geXaRIuBTdWGpOaUcNk2NQjpXrEUHFL+EAwgvWiUkF4e3Oz7F63U"
				+ "ba9vFIa4Xn5dLl8vxXxn0KMRYiwyFv8fdQflUNABavAUME6RIxXVVZ1HkTqMzidMyAq5azjVdbVyPDpR0vt2BFS1q8qok2ZVCoNE"
				+ "KmmXXukfuV62fbKKmlvgbdl6oqyG/t4gNVEf8eaZ/zn7vVMfCVbVLAKDBWsg8AZNEGc49/TX2Ilx2PxWyj2syAgRMpr+snAI8bJi"
				+ "BOOEaekJNggiVCweObTXS5pwhnEhvkNTz0vezdukbamFitcjVh7L4TrOtQdV3tLb2rBPMOV7qGCVWAU87H1CE7dKPhR55+K6t+Hs"
				+ "fge2HCGx0pLZfCUCTJo8jipGjxQ+DSwS0LdyoZ4W7vsfXOz7Hx9rexevwmalbjeXoTdLV78196D31sfBCndQAWrwFDBygJ37uV1U"
				+ "KaPYfZi2DicvlIHulQ9bAiqf1OkZtRwI1zmrEYvhcRZ7lApak3QXzQEyz6W7aZmlZ0Jp5xww/amZtMFYusLr0kLu0EEcdgN4mXY1"
				+ "2T/9ge8J27XRvkDY05psWHKTpFSzMeWE5wRxzrO0ecOQvXvHVCYyxF0NINdeFAV/WtN1a9u4mhxYykN6t0kIU7dJBq/rbFZtr+8U"
				+ "na8tlZaGxqtx9WKmYdg34eyPe49/EOs0GpiBlSwCox0x8awdD9ktuG5orfy47hnXVIhZf3OhFCx+scXlcu5oqK22rRTDZo8XioG1"
				+ "DIodzDnPAKSaT4KwtmutfP1dagmbgwa5gO2we6AWP3a3776Zf9f9yW60WdJtuc5U3i2HI795iKfeQdPRLFSzMd2SLjzLh8qsbKvY"
				+ "/ZtsBGwmOO6UjdupIw88VgIVf/O3RR6kfbmFmnYulM2LP+XNNbvCUPhbYmswHX5c9m/8wfesp/oS9XJqGAVGCpYEZyRxznOMedOg"
				+ "m/1HixdgvI8kuGx0hKpGTlMhk2bItUjh/KBIGNzVQBnU4p+NIjRO9rHO0gKT2yAP2YF50OiiVnCME74x6bjtbVL/cq15lUf03/LC"
				+ "1c48k9EWihe+2LvoYXbg8A+T+pZLQpMmShSivnYuo9bIu7Zn6uTkrK3iO98FiGnwEod15GqQXWm+jf4qIkSqyhPPmEs7gc4gz7+O"
				+ "WkiUWCSGt3T0Y3008VhUMuuvbL9lVWya+2b0rI/8RoiFEz+jhi3SNPup7zHbu3rg2/xVBUdByoyhUwxH1u3cedffRLU4wrMzsV0C"
				+ "NWEjejDpx8lA48cLxX9q9nKHkSOCAQbueltGfFhQDQ8cmrNJli0mwaN44gRjZ9YgCXCw+0SG3YsmyhhWCI+/nG/dv/sBtG4fadse"
				+ "+l1qV+7wSbEP6uw8Dvx4jd7D1232WzcN+G5KDrC4lCUFPOxdYk7+zNlUlEzBUr0MajA53Axl1ANSqsqpP/o4TJs+tHGu8p/IoqVC"
				+ "QjVLggWhathe70RspDtWPdV8eN/8Xdt2OT/81cH2zBfqKhgFRjZHBvjpvuBcxWeLQe3X8cVd+5lQyVW+h9Y/n+wY2ExdlOoHjbYv"
				+ "PdXA8HiyAqHDeaWuSNJ81hI43kdLK2oGrKKyK4QTbv3haHSBHsE+1okTXvu9R77cUsQfECC89mZXIVnIpf7Lcr+HjywYqWYj60T7"
				+ "uxPlUnVgHk47M9jcTasiuEcTWH4tClSN350t0ZTKGTYCN+4c5fseHW17Fy5Lupt7YD9CRFu8FcufdVftzwbESlUivIYVbAKmfJqc"
				+ "WddUC2llcfgaK9ByAIcdgnbjMywL+NGyagZx0msvCyIzyJsz0pkPtrOFHV8uhMnef7AcZLoRvpRktPsel8cf4vvJ+7futN0i8Aam"
				+ "LMb07sQ6RZp2LneW/bTYu4KwQMuOtIUi6KhmI+N4xCLe/alx+Hq/BCM1b9xDGb/qUFhx89+wwcfdC/1YoAdTfes22jeT9yzcStCj"
				+ "Ghx8qQ4qCZ68fu8B6/jimJEBavAKNpjc8/+XJWUVb0PRfJKLE6CmWFf2JDOp3/9x46UEuNV4RTYs2CLLx+k+Y69dIM/iXUw66IwG"
				+ "oPC+Fxnlm04zGBnGBglGs55psuZSHinbhHRbUJ87DARx4SHW9mgMK5ZjCRl4mKZ1cS2hkbZ8doaM/4WX/kJ2YP1zyGBr/ibX33Cf"
				+ "+lPkb0WBcV2PIboT1xsFNexVdSKe9qFdRIrY7XvchzdySiSLkdO4LAvbKcaCK+KDerhtWpIV11KJSk8unGUpERhndKkkDA4WNFJj"
				+ "NKlm5RORxUvOdymn5wE54lZxoJ9uTpjJMy37N8v2154XXauXi9tTRAurvf9Jqz/A6Y/lub9T3mP3lIs/bfs0RcV9qctRgrh2MJLq"
				+ "Wvc2Z8uk8q6mbhy+d4fzbSel1VVysAjxprqX9WQgSah3jjobu03EilX+YymkzHNlBW+58meNzaZ9xP5RZ94W6K3A4eu+Yn48Xv9d"
				+ "U+v9lcuKfSnbDzyoiMX5SZfKYpjc+d8oUZKK7+C2XfD2E5l+lTVjhgio2ZMk0pUA1O7KSR5JRHPJRoevZAzhnfMJtGd8Mxpdnhey"
				+ "flMHz9KUpwIydE7lqLhSXGQEAcK3Ldpq2xY/ry07OPoNWZFE2K9iOn/yqaX7vRe+jM3K1QKOe8ZSfPzFw25ODamYX/46HzPwurf7"
				+ "E+NETf2duyWw75MZDAb0KuHDZKhU4+U/uNG9kyDevTKjsxHg5MXckQ30kwSOFgustDe0iI7Xgm6QTTV8yPVCRZjhzdC2ZZ5S2/aF"
				+ "YZxl5Eoh42D2W9v5LPHycVvnq8c7LFlKhzZhmeLSced8/l+UlI5B1cmB9I7E1bJi7SirlYGThorQ46eJKX9TBcrJUfQC23ascs0y"
				+ "nO0U9O+FcBhbP4snn+L1K9+wXv23mhv+Zz+7sFsEtmGp5KLvOUdPPhipeCOzV1w9RRk+wqI1VtR3EaxzLE6N+TYyeYF5UoO+wKvi"
				+ "g3MhtQiGSnK0SgJrwQLZhZ/OB/OJpJhvER4mnkzZQDng0kAFsxbfMA0fmPeLHKeYeG6aDrEzHMmTTgxy5i3Deo23GLTM/Mwuzop/"
				+ "cg8SWyDqVmFebOIBXaDaNiyQ7a8sEL2bgi7QTiOhw1ewcLdEm+7zVt8vfW28p3wiIuL8OcsSgri2NzTLiyVqoETxHHfj8UvIttVz"
				+ "Dm7JdSMGGq6KfQbPiRx4ZHEhciLDVOzKnJ12lmWWCMkQXA0SrAumE0KJ9H0DZF1hOtNWBgvsWiXwwCGkeT0sRBmOjn8wJhkw7SJn"
				+ "U1MU9OLxCWZ4tnljqkvO19bY0aEaNy5x3wINoy5DvYl8eJL/K2vbfdfuj+fG+Z5uEVH5GcrOvL+2Nw5l/WXkrKPYPbDuFJOxJVSy"
				+ "oH0+g0ekGinKqkoT73uIhdecIWlO9AYwifUlMvQylITj9u0e75sa2qXDQ38BKDIqOoyGVFVauJyvYf1e9vismZvi7R4nkwMt9+Hs"
				+ "Jd3NXXsF1YRc+XI/uVSCY/vzYYW2dzYZlb0K3XlqLpKGYl0XaTbhkt6b1u7rN7bbOIwy4TCYBIimJ9cWyGDKkpkE/L2BszuzEzSz"
				+ "JulhOvFZcA4mDerozCc03DbpPXRbTkJ4zAhflp/1+o3jHAlhrHxfX4v8e+I80vZu/XP3pN35utL1fbIioqk367IyNtjc2d8JCZ1I"
				+ "/l1mstRrOaibNUyu6WVEAhU/wZMGhsOTxx0kDRP+ng45kIySZj56MVprzMbbe6o/vKxI4dIdYkr7YjIuB7+PLBxj9y9aifEpkIun"
				+ "TZCaktLJO6z1sONRdbua5YbXtgi25rb5GsnjZbpg/pJc9yTjy1ZhSnzwX35csLgarlk6jAZUF4if1hbb9Kk8H1g0iA5b8JAacc2j"
				+ "M9kud8/rKuXP63fhXmb52CdfYJ52xkTkVZMXt/dJF9++s0g3OTJxAo2YiYRbvLKoHAaBhvMPGfShBOTLBYS5xME2yA/qfskEO792"
				+ "3bI9pc5/tYGeFvm/URusRmTexDhZm/Z7eukYaeJnkeER1xchL9KUZJfx4aLwT3zsxVSVjUZQvVfCDkPVsFsllWVS+3o4TL6lOmJB"
				+ "vXUiy2b+RqI1KXTR8oIeEeffXwdBAkrU7hk6nA5bmCVfO2fG2QjPZo03H7WJNna2Crj4f3c9spWeXjzXhPOj3x9bMoQmTmkWpraP"
				+ "dkIz+mWl7fIEHhIX5sxRp7b3iA3v7QZ+QkydKA8T0M+vnTCKPkjRO29ELyLHlkrWyGYQSRYIn7QphfMZ0qzI0622ybHT4mDlfu37"
				+ "pCNT70gDTt2SbyVHqWJtBXTH6Oa+DPZu2mj9/SifPG4eDRFxwE+MNcnscU3lWzDk3DnXzVJyqsvh1j9Got896+C1b+BE0fLuDNny"
				+ "LgzZiQ9/UtcRCDbedd1pAQBWyAk6cSKlGHfu1ri0gzByQSrfayerUF17tRh1YkDrYNXNaWuSp6GMNH7KotBjLGyBH+4zbp9LchPR"
				+ "4YOlOczR9SiWtkqizftlZ3N7TJ7RE2wAvBTY6y2Dq0olXKkfTSqm2ePrJVpg6rMMinFfsf0K5P+ZTGpKokZAZyDOFMGVOI8mCjdy"
				+ "k/0l+wUB3+qhw+RifNPlzEzp5nuJWGkYZheI7GSn0rdmPPduZcNZGAOiOYsSqbwPkExvxmbFz+sO/vTpc6UefyM1s24570LuRrJe"
				+ "19F/xoZffLUYDC9wQNwYfKnoLgE2ea1YO/y9A5MePDfLPMCTIRjO7vMaQXEaNbwalP9ehgiYGKZdDBj8LG+RqpLXXl8yz5pNI3KT"
				+ "CecIB4nHzxisLy6q0lW7mmW04fXBnEhUKMhDu8cP8BU8ybC+2JVcPm2/WZ6ytAaqYCH9xjicn/cL2eC6hbyh38d+fFNlfJ8VF0f2"
				+ "7xPntq+37S7TYXgLEG+4zgACut7UMVku9iC0XVy3viBxrM7e2R/iG1cVu1tgUi58pHJQ2RoZYl84IhBcs6YAUZgTxtWYwRw3f5gG"
				+ "Cybn6TzFuYnwIbbeCnx8Z9j4FcOGiC1o4aZNrrm3XvZKM+NJsDOEDd2sjPx9Bf9La9sk7ZE9wglR6hg9QQcSG/O5/s5k89aIKWVP"
				+ "8DyVQgcDY+qlO/9jTj+aONV8eMPvAAoMrwwOqY2HfO/IzwINct2asIjy4SCMQsX67HwMCgosyFOtKOxvKGhBaIDwcL6U2EUh/kQg"
				+ "reMCWwrvDK2X3Fn508eLC/UN8pDG/YY8eKFT+/pLHhAo1CN/fObu+QYeFpsaH9y6z7jscXg7ZyDdN4zcaDxhurKSqQNF3wjPDnjy"
				+ "5n8mmya/J4KgZs3ur/chCpkQ5tnGvjfPWEQhLIR1cJ24ynSA2O72OKNexBvi/xq9Q7sv1TOhGj9EyLXBmGbjzTeAqH67ep6+cHLm"
				+ "+XeNTuNsNHLenZHg6m6mt2a/UfOm5la6+b5xzHyYUj/MSOkbvwo1AY9aW1ocrx4vAoRj4Sd74w5aSyEa6szdMo2f8Nzhf6aT96gV"
				+ "cIc4876jxJ3/pUnQqiuRdG+C0HzYSWl5WUyZMpEmTDnVBl+4rESKzefAQQd93dDuGhu6gmCBmoDZ+wCppHZjnkz40gLhGk7Lvodo"
				+ "e1qaUcV0UQx0Pt4Gp7R45v3wvYZD2qHbTsCrGpRDHZDRPj07sj+rGIFIvP6nibZhe1bWCWEF0Tvik8h//LGLvn2cxvlb2/ulhJUF"
				+ "d83aZB8c+ZYOQMeGpILCPNQimWKaCsyNaKyzFTlKlHNq0Q97hiE2+jkFXh6v4ZQ7W0NmoiY18EVJfAScc8N03sEXtlfIaJN7b45T"
				+ "ra5sTpZw0+WYTly6CnnN0IYbtYn4mDGzieF4+YwoD9uPifL2NNONALG0V2xMeu0n0Ld/OdSO/QilIcxztiTo4ejHCQqWDnEnX1xu"
				+ "dQMvRJ32J9j8RJMB/HWzG4KE+efJqNOPR5e1dDwrm02AcFM4gIKw80dPXFh4A4fznXMAMbpmE3EN9tiugpVuTtf2ya3w+6A/W5tv"
				+ "dRDtEwc/GM3AwrLH9bvgtWbKt4mhPGCpOttRYg8DU+GTxYpAEfCc3odabdgXRMEqxTCRM+KO21FGD2au17fLtc/v1m+8vSbsr2pz"
				+ "XhA9LYMzBwYVFEKYaqSoUjz2hlj5JqTR8uXThwltYh3NMJryzoqABQ1Hpg9b01Y5jFQVIP0HLPvxMGDPa3tED/XCCfD7LYkOk/Sn"
				+ "v9EHC50zEbnGYedeQdOHifjz5opY08/CfcqPksxWx+FmW+KW3KXM2X+eXzlSjk0VLAOFTcm7vwrhrkLvvhhqapZLr7zLVzwx6C8l"
				+ "lUPHSQTzp4pU/59ntSMHmFG/rS+Ev9GjfCi6RRGi4Zjgd2P7LyBYTTOBouYBsu8hs3UrkuYncPfMJ2OdXAJ4SnwkmN/LK6mYI1k2"
				+ "9W4gbIfHteLqCoynI3u5RANakKQTJAyG/v3wBtiNfIpeHF8gsi2JsYxhjhjq8tNf60rlq2Tjy9ZKZ9YssrYV596w1QnR1bxfFmQb"
				+ "rgdjfvjXKinIJgxeQiNbXlsnsMhRPbLlQFhtERINH0TZpft1Mx3nLno+Sd8aDL4mCPkmPefa6r9FbVwtHy/GnY2Vv/WPePi+1BOz"
				+ "nbnfKG4x6ruQVSwDgH3zIvL3XlXni1u6S24um/B3XQa77ocO50fJp04b5YMnjJJYqWl5uI3Ft6e+TfJ8CdY32H8Y+bNgp0PUrDzB"
				+ "oYZC8IjfxJhwVLHlJ4TPRmKSElYpWN1zzgjMIoQL0PG4zKrlm9AfPiE7tXdTeYJJBNjX6syiBu3ZzzmDpNgipky/BlfUy7NSMemF"
				+ "cQTmTm0n3k6uH5/q+xj+1V7YKv3tch+TNkGx3ikH6p1NaUQUczTjoUH1oA4DYle6GI8MnpUdh8nDOknO1razEOFjv1y6wDOJYzrw"
				+ "lU2jH/MfDQcC2YazhsYZiwI57A/o2ZOQ/V/phn91QxRHax8F6Z3wQX7prvg6qOcI84KE1C6iza6HyTu/CtHS0nFV1AAr8Id9DQEm"
				+ "VE/WUDZn4rTkkpccMhFcE82M2aZ2JuzAWEMtmHhczTz32wLl8qsZyjDMGPS4SpMiF1mxHII0GnDeWMP2nFsfDMN41IAzh1bJzOGV"
				+ "Mv8Uf3l3DF1xtiOtGJXkxGhd44bII9s3mca2ilML9c3maeBfAJo2pKQzqSaCjluYCX2s8/s/6NThsiHJg8x3QoWIN13jB8oxw/uZ"
				+ "9rIlm3dF7Sh0bDtpdNGyj+3N8iybQiHF8S80XuMI9IRqH4ei7w8sGEPhK1aToT4zBxWbRrS54zsb6qYzMvSTXuNWJ4+vMZ0cuXTz"
				+ "5MR55zRdeahwpKNe+VJxLP7NeeJs1y2IMzsOww71PNv0oHgl9X0k9qRQ6VyYH9p3bdf2hqaGKs/4pyECLOcAaNjzrgZr/nrnuzrH"
				+ "33tNjyBxUrOj82dcX5MakeMRjWQ3RSuRQEdyL1wPCpW/4Ydx9dpRtlye9hI3R/bddgNgBfOS7sSX0ZOgo/k+epN0C0hSIMeEBvm2"
				+ "cDN9GZAKDi/PdIQn8oIVNvoQb2ws8FsT89nMsSGfbWYLtuaXqxvwPrGoI0phB7cacNrZQW8tc2Nna/XcaguTqwtlycgjhcdO1yGo"
				+ "ep454rtcgLEj/2tVu5pkicgRHz6WIflK6ePlK1NbaZ7BIWOniOrrazK8sFBT5J6/tPBgQO3vfia7HhtnTTtxk2E9VRuidODmS+J1"
				+ "/akv/aJen/NslxltmcPupc40HkuZHJ6bO5Zn62UsuoPQKg44ufpsArugCMoDJk6WQaMHyWlVVUFe0a7c9H1BhS2iyFYFMYrl3NQ0"
				+ "M5YweJ7kuweka+wzau5fo/Ur1on21esMYMIhnAEiD8hwiJ/7bKH/FWP5EJsilKwtEp4AJxjznXc498zHdW/b0OsLkLQsSgKJbHyU"
				+ "hl6zGQZOeM485JyrCz8lFZir9lJAAtzok0kQlI4XaY0cbLGZg1TU6ox3ynVyL5YLWKlyG5mSV22RMOT4mRY0Sk4DGAW2HueVcKa0"
				+ "ph5DzIRx8wFsHc9O7ayTYvVxIDUWF1zOM4/0+HXt/lRWw5pHW9plRZUFcXz2XZwHGKc7Qwc29+ZMGsFPC07DKoS4eDPfv5zSMdmx"
				+ "lGv6D8eBYkfJj0fydUwxdKKMqkePtS0U5XXpXtMHV4o3Lu9ZjhNIRrcMW83yJ4Mu+lE+ngHsd/u7rBLOoQwE2yfYhsZq3jLtloxS"
				+ "qYcdc/jw5e0n0fV1GTO5i9DPqPBHfN2g+zJsJtOJMXzPdm3YYtsfOZFaYTn5XV8+HUd7Cbx2u/xt7yyxX/5L91JOpWD2SbvObhfp"
				+ "zA46GNz5105StzY+5DEh5HKSWGw1I0ZIYOOHJ/sUVkOpqwfzDbgIDfLH7pzANE42cbvLgd7Ig92uwy07mtANXG9GaaZwmXwJY593"
				+ "A+vbhFcsb97i29Mr9aZYS6Ljhye9rwj62NzT3y/K4MmnAmv6uv4uacjyLyFy68oD5t6pPmMVmlVJVIOHmEbUCxs+TXTMNzWHjglq"
				+ "eGJ7aLhwWyGcFTMEGjmo9tyJpxw2cYPgzulmdg/wXw0no1D0ubTzMBIOG/jcRqNkJQOp8FiIjy6PTHB4bKNYggX7H64ndnURrLLw"
				+ "WzyfjGfSD8lPDWd6H7Th0fOP5cYzgVGDidctvHD4CC+DY9MDZi38Xx4hy1798mWf70q9avfCAYNdDjuj2zHBkvEi3/Je/B79Ly6i"
				+ "026qLCnrhjJ+tjc+VfPEde9A7PjgvaGShk0eZwZTI+iVfywjBdzkch3IIr4CRq31cuW51fIng2bJd7KJ6jmN3lC2lve5i2+IXTBD"
				+ "khRClZf6zia6Wq04eyKbeY5fMi4M042bVWdxKooiwJRsepd6Lk70g9lb/ycmTLq5Kkoe/3CdT6v1dQf6EDluejoa4KVSWo6wkN/v"
				+ "d/QweZl1g7/PULRFgflMMHu+ewgtwMlj9W8F3CjfAL2gO/7v8P057GSkh9VDxt8q+u6YTUwbaE7cHkuMor50sv62FAlnAeBuh2zY"
				+ "/ku2MiZ08O37/E/TI2TaHtEQBgQzhpMRBsveDRuVvEPwsLVQRw7H4FhJN1+idl3IlK4Hn8SY5JzOSV+InoYz6QdJmRmuRLYPHdsn"
				+ "LwtMfGDTSMbhgHhsl0VboL5cKnziuR5EOQhmDIs3Xkw+zfYiMGsIdwuiJfx/LONqB3z7ZjnIzr2kOWUA1zxLfB233FMOA6NneXbk"
				+ "QxHy2lHAuxERdHZj7gNCENVzecjSjaOs9v/fqTNdXuxXQMygWV/r++17/daW5rrl/2jZe2t32BuMuLOu3KsuCV3IK25yPFy8VrP9"
				+ "R66fne4+kB0mXahwt+tWMn62Nz5X5wnbiBY/FrNaAiWQ8FS8hVelGzkaYJxtDwa5ykmdr4j3Pejcex8h/k+u/iH84zjN0PsWjAPY"
				+ "fJbfN9r9tvbG72WppaWbZvbNv/xZ/H6Jx5A9J7BCFaMgsVx/2W5tEOwFqtgFSsHIVidPSwOY2zv0sTcqcOiEIRzLuhPZOAEYQy26"
				+ "zkl9k4fzCevj6YfbB96BSa4I/1oOEOCrTgfBLAJLkwiyduyRPMQxYRzCrN5sASzyfsNQsJ5Rk9kKFgbTYJ5M6ttZBJsjM38NqTbh"
				+ "Bh7kXN4J/RSnD0Ib0B4wlvB9vuRAIcxZRg9GQ6liqnfijB6QYGXhPQQhik9Jb/dwTqk1e57XpvvxeN+e1u739ba1ranvn37kj/6O"
				+ "5b8yY83Mbn8I/CwYnfgPM3F8cDDaoeHtVAFq0jJ+tiMYLnu7Sj0nQSLBNdYSGQhGm5LSaedh5GC9VEBCuKacPyx+8qEjU+i81ygV"
				+ "ti0ouHpN+gAYsDhOFkNMhd+xFhFYnsLP7wDYYmsC6pFnKc3Q4+lEcnzKxUNSAcCQwHCNFjmG9icZ7XIiA6qRg1ea+v+pjdWtbx67"
				+ "ScpJNhUieLOh2A5FCwHguUvl3ibeljhtBjJ5tgY1094WI7TdZUww4Xfy7QhW6zOtCBrbFsJqkWs5mCKdS3IcyO8rmA+iGPicx7bo"
				+ "MrjN9PbMfF9j1+SaMSxIg3MC+YZ34uzamTiwDNp9pobW3Y/93h8/e3f9f14x1AvWWLOfzB7WMm3/SaFG8GihyUqWBaeoGIl62NzF"
				+ "1Cw4GGxSgjBGnVK4GEdBli4OKYAhSPScGs8Es4bzwXruW6PjcNqFLYM4gftM/R62hAWekKsGpkqUhxTVo3axPOCKhKqRl5Lc3vrr"
				+ "u3te196Ol7/xIN+w6qXsJmSLwSCpW1YUVSwIiQEy2ej+5TW0acc3wTBakNKHVWhDmMdhlO4FRQFh8v0Vigwpp3FVIsciokRl1CAT"
				+ "LvLHsRvgLeCqpG3z29rbWjetrHppcvfr/UiJUFyozs8LCNYN6hgFSkHL1jwsGpGDP3H5LeecZ9bWkqhQVUJYuR7jb5vq05+k++xu"
				+ "hRv9trbmuKNDa0NK19sW3nd5fqFFCUn6FPCzqhgRejwsPyxWFwoTbu+7D12m3o9Sq+Q3OjOp4Rtff4poXYyihL9iQ/0uE5Repqk8"
				+ "ghj35A+jgpWKrZQaOFQehuKlL1vsjzqTVQFKxkUCFMoIlNF6U3sfZPlUW+iKlhJmK7hJHWqKL0Ai5/1qlSsDH1ZsDK7T+kLSab4m"
				+ "dNJT67iZ5tOJrJNP9/2m21+chU/23QykTn96BrjYZkH0Lnab0HSlwUrwy0LwRSqzm0GGeJn7YblKn626WQi2/Tzbb/Z5idX8bNNJ"
				+ "xNdp2/LYkd5zBS/T6BVwiimKKBQsGCYO1qfLhtKb8PiZ8uiESta30YFK5XOdzRF6SVQ/uw909w8tTyqYKXD3NHCeUXpNXjTDGfNz"
				+ "dOqV99FBSuKdbtZLsy4Tra0KEovYcpiaG4xf/e4e6hgRWGhsDpltCrptcDeUi9VzYC+d/65Z3a1MVOYp2+JqWAlgcKRaMPiclJZN"
				+ "SG9QG/tN9/om+efnr5pvyJJ5bFPooKVimm/CgtGoqAoSi9iy6MKlgpWRlg2EgVFUXoJ+/aF3jwNKlip2Cqh1sSUvCC8aZobaDDbl"
				+ "1HBSiJSIqhX6mEpvYm9b5qbqAnp86hgRTFPZCBSRqhg6oYrvUlYDBNlUoujClYntEqo5BOJ8gjU41fBSoLlwt7NWDaCt+MVpXeIl"
				+ "kezrDfRviZY4S/fiTThCIq3ZYqfLVns15BteCb62n4z0Vv5OcT0TSescDbxpztkm8+Coa8JVqZfPH24k7PTk91+sw/PRF/bbyZ6K"
				+ "z85SD/UnuwkKNt8FgxaJUzFtl9xGist2h9eKQTCTli2HUvbsFSw0sOCoYVDyQOMZKEsUqz09qmClRbrZSlKPkBHSz0sgwpWJ8JCY"
				+ "e5oKlpKb0OxwoTFUsujClZI5NYVuZPFWyPhPUKm9LPdb67ymav8ZEuh7zdX+UxOh0vG8Ee1yqCCFdBRHKJud88PmJapGGZbPHNVn"
				+ "HOVn2wp9P3mKp9p0gnLo1YHDSpYUWyZsMXGcXNVEBUle5JKHxe0OKpgpYPCxbKhdzWlN0kqflzQ8qiCFcXexBJPCbWAKL0My6Itk"
				+ "3oDVcHqhCkT+KOFQ+ltKFTElkl9SqiC1QmWCRaQhJel9BEqYfYpSz9Ynl0bKIt6D1XBSoIFwhQKWzgKooQMh70N9knYp2Afgb0Vd"
				+ "iSsFBaFBzQC9m+wi2Cfhf0HbD6M6XT3gEtgR8G43w/APhTau2HTYeWw3qAG9lHYNLOUHTwPb4fxmvhvGI+jd+GvYT19TvX+qYKVR"
				+ "MLlNiUFllRCwpJz2InuNzUP74E9B/sl7IuwK2Bfhf0IdieMomXhxfxD2ArYz2CXwi6BcbufwChgFbB0pO53AOxa2B9h34R9LVy+E"
				+ "bYM9k/YZFgu6c75Hwy7A/YOWDa/VzXs/TB6WBNhFPwWGOnq/Pcs0eJn5lWx+rJgpSl80SAUju61GeSqEHcnnWiGGP8CGMO+BfsYj"
				+ "N4Vp5+AUbzWwwjjch29iMdgX4DZ+PRIGL4I1gyzpJyMBNHw3TCmwzRo9PJug42CfR7W1TFlWtfVNulIFz/bNCjAFK0TYBS9KtiJs"
				+ "FQynYdDIfN5iK7hfFAec7XfgqQvC1a08EVAMAsG1yYPL5MhfsbwbMk2fYZPgb0IuwdGIXoSRg/nYdgTsP0wMhBGD+IN2Ndhd8MY7"
				+ "ykYt1kCew0W3Vd38tMKYzrcF9P5B+wHsHUwVst44WeiO+lH6U74wY64yCouf+yTIvMnw0i2+cmWrtO3ZZHToHqYKX6foC8LVgbCG"
				+ "1hh3Me6e4FS2EbDXoZRpHqy0G+HNcLKYLaKyba0o2GfgdEDY7XtezBWvSimhGXxGBi9PYrqLTBWXW+CfRA2CBblWNhlsJ/CbobRu"
				+ "+M+osyF0YscYpY64K/LNqr/hI2HbYHRS70O9iqM1Wruu3exvxJzS7HSJ9cqWJ2w1cDCuI+9AJsKY5sNL/xMJZrVHD4FWwtLd2Rsu"
				+ "6FnkYsrYiiMntVemPXwKA4UAAoBG/f5FG4mjNXQi2FsX+O+58A+DmN1jOmw8f5UGNvkKC4WHiuF6kuwCbCxMG73bVgUCiC3nW2WO"
				+ "uD+L4TdYJZEmmD3wehp7oTRA/0XLD8wXlZhFMieRgUrFXsny8Wl2/OwYZ05vQb2J9itMLYpURSiUEDo5bDNKR1s07odRm8jGyh0Y"
				+ "0KjaJwCY2P+JNjfYbbhmnliGAXm0zA+nWQ72zOw98Lo/cVhv4YxLsWJQsaHAkyPXg/jWc6BHQfj8bIdj54b29DY+B/lcRi9Jz79i"
				+ "zISRiFkVda28+UntixyqpqlgpWELRTmjsZSkvf8H2we7McwXvBnwq6HvQ5bCLPVKP7OPKBMVUhWGSk29DwIxY2N0PR8rHGZXlgUe"
				+ "m7Ph0aP5BEYn1zS07HeC2HVj1VRtnFthrHaSG+PTzhtIzfZBWuA0RvkPvvD2E5GrydarTsbthHGp5RsL2Oaq2HMR5RVMO6TeWLDu"
				+ "oXtU/S++HQ0v2XAlMVw2vMv4+c9Klip2LsZp37eqxaLMr0Pdilg36oPw/h07mkY23RsVYgXPQWN7UrpSD3O02H/A6PoWfsGjNW4K"
				+ "Kz20QOisT2JDe6sBtbBokIwDEbxpHDQs2I7FatkFLINMKZDkaTnxDR+ETFW6egNRWFafKK5xyx1kHoczAsfRrTBzmVAyHkwdu9IF"
				+ "bg8A+XPlMXQ4jyMvo0KVhTbVmDNCcfULgx4cT4LY7sQG4+Z9xkwQkFg9YzeTDoYN3qsnKfApVqqh8a2n7tCYxsVPSs+NWQVLdpIT"
				+ "teAHU0/B/svGPt+XQ0j9A759JIdWlk1PQPG/mIUTBr7dr0Ei8Jya/PYFYxDwWK1jyJFUayFsaPso7A3YfkL5TdRHrmcqsd9DxWsV"
				+ "KJ3NBaUwoRVLlat7BM4VpvYfsUGel6wqfBCjsJuERQPdia1xn5d7LrQFfWwpTAKyTsZEMJ2JLYXsVGdnVlp7Fh6PIxCRzFl9Y9ta"
				+ "Gz7YqP3/aEthrFKGGUHjE8g6clFSVdnoij9AcY+VqwKsnsHz83fYHyamcfghpkoj1zkn76NClYmAg8rXOhVWJXhay9sg3kf7F2wr"
				+ "n43ZprtM7yYtzEA0INhFegIGKuOtmrINitWy9h+lSvYL4z7fQvMCiHbr9hI3tUrM/Te2mH0zOyJp+Dy1R8KWxQ+HWWbFkXIPt1kw"
				+ "z2rxOm4F0bvkm1ffKLKNj5Wm/Mb3i+jHhatj6OClYotIPlxM2Mu2BbFqhS9Dz41Y3XLNn7zSRrbef4Xxn5LrF79CsZ+TPRa6NUQt"
				+ "vdwPQWA1TY+EWSj+G9gbCPKZdWIwsgGcfaJGscAwFeHmGfuk1U8Vgf5xJAPCNjmRjGmF0Uh4YMDHg+P5XcwChYFNwqfiDI9pvN9G"
				+ "NPhcVMk6eWlshLGaiXbz9g+R+8q1WvLTxLeVbDY11HBSiJSKszdrNdLCXNBAaCnwuoLL1xeqPZeyyoQ+yuxqsf2KlZ5KGz0Hthhk"
				+ "v2KLHyC9+8wNjSzPWkWbCuM3tvlMF7EbOvqDhTD5bC/mKVkWN1ix1B2WbBtZkyb3RJYpeR+Oc8qo+3oyeNhtZFtW6wOslMovUFWB"
				+ "/mCNUWOImXheWDVjgLHBwE8fnpR9B65bz6IiELv7Tswnjt6l73fKbS78ObJcpg/N9FepZhPQdbH5s6/ep64DrwPZywuoYXSVP9l7"
				+ "7HbevvRDBuL6VnxAmf/JV7k9BSiosWqHbsG8JgpJuweYNenws6Y9LR4XIx3oIbrXML8cd8UDeaT7WqpDfmMwy4NXE/rCnZ/YJsch"
				+ "ZYPALqC7WZ8OMD2PPacz3vc+VeOFTd2B37JufC0lkt767ne4usz9aVLJdPvX9Coh5UKf2bzU+fN783f6EEYPRde3H+FRTNHweEFS"
				+ "w+F/ZFYJeoq8xQBxmPD9eEUK8J8sSpm85kqVoRx7FPNA0GRopd4ILGiSPPpJJ9EsjpcOER/yWJ2L7qJClYq0TYDP931dNhhOw4f7"
				+ "xN2V2C7k9J92BeNTz35us+Xw/nCgGJlymOoVF3dhvoIfU2wwl++E2nCERRvzxQ/V2SRH0Ou8tOX9stqL6uC7KhKsY9W8XsjP6Sb6"
				+ "YcdlylUpi1LFauvCVamXzx9ePLwMj1BdvnJXYntS/vlCBV84sgHEew2EaU38kO6l76VL/Zfzo8uNr2OVgmjsEyYbg3hNFaSXIAU5"
				+ "XDiQ6mi5VFFSwWrE7YNK6PXrii9AMul3j5VsJKwdzJTMLR0KHkEi6N6WCpYnbAeFi33TwnZAZRjVaWOgEnYn4qvl3BsKb5Xlwr7W"
				+ "nGsdPZRsjCMr7JwW6bNTqT2dRil0DHlkOUxLJO8mfZxVLACWBwCEoUCQfG2jvBksg23sNMkPyHFPkGp453zfTmOz0Tji8apcNyr1"
				+ "KFWODDdlTD2KOfLxOwRHv1SzoHyk0qujrdQ9puJ3spPmvgojyyS6bUqV/stGFSwAjqKQ9TtdtK9/G9IX3wyh1vYIZKvxVB86C1FW"
				+ "QDjy8EUHz6Cj/42fKWEr6Dw1ZN9DAhhr3e+G2c9Lb7GEh3W5UD5SSXb4zrY85BKb+03E72VnzTxUR5ZJNNLUK72WzCoYKXDelmxW"
				+ "K5/ePbI5jt2FJWoJ0RBoufEETLZV4ije/JdOgs9M8bn9hyl08KOpBwUj6+b8N0+CtiBhoBRCoVo6StaCcoOFawkwtsYvSwjWulva"
				+ "4cIB9nj0C/R8dPZ9kSPi4LF4VnY2TH6XTwbn32K+MoKfzeOS86XfhmPQ85QuDif2j7GdDmeOkWR797RU+MwNWwLY7WUabDNjGlSJ"
				+ "Pn1ZqbHjzsQupkUS6bP9xo5dnsqFGCOssCRFfhSMr9WE22H4775gVJ7zMwjx8ZifPZEj1aPmQ++xM32vNQfgHm1x0LojXKIGcJj4"
				+ "2e6uH8OmWOHRGbafDGc4RzNIZovpm+/RUiYNoefYVy+gJ3OxeZw0qyK831EvkxOr7hnvnQdPfoeKYpKPsGfOCtz5189313wxfUwH"
				+ "3adO/vTfEk3bdxDMF78bM3nsMMchI5hvGg5JjlfcmaVkaLF9XYbjlfFQfnYhsVlXpwccYDDpnBoGL5Px3cDOdwwP7xAMbPbciQGf"
				+ "qyBHhw/8sARDjheFS9SCgAHzOPoCWwL4xeb+U6iHXDPfjiCIst98B3AR2G86G36TINjq3OMduaRaXOET46ewIuecSi4PB5+jouCw"
				+ "Hcj6RUy/hoYh8Ox6bF6yzxeBeMDBBtO46gPHE6GHieXKbzMMz1Lfj6MI1swTZ4TflWHAsx3B/nNRYZzlAeOZmrTo/Bxe37in0P1c"
				+ "CSLTTAeAzuaUqApoIzLKUfA4G/DODzfPB+vwDhkj00zZ+bOv3IcyuFDYXl80p13BUU4bdw0VpTwR1CiJJ4S8k+PvEtIL4qCQOHih"
				+ "Ud4wVFAKDa88HlB0FugMBEbl+sJx7fiBcovxbDti54VjeOhMy2OEGphuvQYOHQyf29eXBQovnxMwaTXQzHhsMEcgoXiyJ7hFBsKy"
				+ "2kwfo2G4Rx7i/miZ0F4kihC9GA4pWfEeYoc29M4jhc9OXoqfDrKAfb4/T+KHj02GsWEDyEofIRp8gECvTDOR2Fe+QKz9XwonBQli"
				+ "iPFh21/Z8E4TDNHtPg9jOPZ8zzx+DjwH8cTo0dLmD73S8E8H8ZvJTIePxlGz5Rj1dsxvegRclx7Ch/j0rNimyKFjTcFepW5xVYD9"
				+ "SlhAhWsKObyiFQF21vDmZzCHbAdihcfPQ96Eax+cV+8eHmBcbA5Co2tfvEJIj+4QDGzsK2Kxm0ocPQqOD4V7/qp1TbukxcjL1y+V"
				+ "0fBiJZ+Xsi82H8LoxfCcah4YXI8dH5yi0MV02vi+FoUOtuwTxFloz9HkOAnx7gtjWNa0ftjPlKrhtzPN2H02piPn8M4ZEq6Cz7T+"
				+ "U8NpxhzSGcKOsfCYr4p6hRePnHliKf0jChkvAvZGwVhWvw9+GEM5pvb83g4ICBHSaXQMg6FmmLJ88jzwPPM7XjOOAKFFdzcwb3SK"
				+ "FTRX6sPo4LVCVNCglm3x17N4cXKOzddfF7QFCxe4HaMca7nOtuORMGiWEVH0+T27CLBrgzWeHHyCzWpbS8cmoaeQKYhW1gdZXXSH"
				+ "i9fEOaFzeoOL8xoOIek4Uki7DPGfVGk+BEMlicavSp6SNxf9P09PuFkPqKuK4WQYmyPNUp3zz+rdfajrYT75D4oUPacMi3eDBhu8"
				+ "2/huecxRGH1kL8NxY1NAxQuTil89jh57Dzf9IR5jnsAZNV4WJFy2YdRwYpiXW5bLuJJHlZqIT8U6BnZBm96Umy34sVl4TwFi+Fs0"
				+ "GVVi+0zbDchrPax3YjeCj01Xkw0XlypYnUw2GO1ZyLTsfNpJvfHT3xRUNkuRWMbFAWEo4TSU+sKCgqFMNoh1pLp/B/oys20vqvtU"
				+ "texCwqvD/5O9AztZ/fprdnjpLE6yWOPjoiae7Q6aFDBSiK8JszdDJRUdKeUZLqYu4JeC42fcKfIUHSin0ant0NvhE/W2JbEhmheF"
				+ "LyICMMoUGxMZzsQn1jR2HDObQ8Ve9z22DKdB+vVsIrEdig2lDMPbPuhmLKR/0CDBFIIKAjpBuE72KvU5jv1t+nqt0pdx3PO/bNqS"
				+ "Y+NU8L2Ph4njR4tH5Swbe5AwpyJTHlKDmeZDESrq2MoelSw0mHaDGBWuAIyXTyZwruC3Rb4dIyCRVHi78BqiYUXCNuVWE1idwEKA"
				+ "+PbsZzY7kXPhqJnq1fMLNuM0n3GK1vsgR/o2CiizBPFle0+FCj2I+OU7UgHGgmU2NeNUj+EwXMS/QFY9Yq2h3WHg/ltLPRied75W"
				+ "7EqyXPNKi69Kh6fPVa2YdH7PVi6LldRrz8oj4dyTAWPClYmkrQq57Bqx0JOT4lP0/jkkN0GorABmX1+uN7Gt/CrNGz34dM/9prnF"
				+ "5PpbfEJYC4Eq7sXBfPAC5fjzfMLNuw6weor+3rxk1p8uhk9k6zmXgNjPybG5ZTdCejN/B+M0JPh8bIxn/2p+NSPTyn5IKC71d1M+"
				+ "c8Uzn3wiSbzzHzxKSqfJrLqTiHldtw/fwM+5eSHO3iMPFZ+VINPFnkMPQfPYvINtE+igpVK9I7Wc7CBlj3eeTGwQZ3eiG2fsrAdi"
				+ "xcvG7bZqExRs7BRnv2L+P1B3uUpVmwQpmjwUT67TViYLj2hdA3uTJ8eQ2p1xobTu4jC9iamFQ3/LowfKmUnUD5N5P7ZzYBdLNjlI"
				+ "ioy9LjoUXIbDv3Mp3t8eMAHBmzgJ6xCsr8Z47EbBZ8+Uhj4iTB+EYeejq1mUjD5wMA2rFtseOox82kkPVl6TFF4vBQe7ottUZzy6"
				+ "SvzwSewhI3wfLrJtNkXjG2IPAZ+wozHk+6F9kOHGmU9/h6+ixYCxXwGsj624Ks57u0oHKxaLZTm3V/2Hr3VVsNyDb0NPibnTYNP/"
				+ "ygy0adnbOxl2xYveFa5+DQtKqO8o9OD4RMqXnBcz3TYQ5zYKhbbuth9gutTq2jcloLI9CkEFu6b6VCYomLGRnZW4fgY3woszzO7O"
				+ "TA+98NlCgLb25gm4/JYb4LRk2E12MblsXEfFJfoUz5W/ehdsqrIvNGj5Plh/ym2dzFdVpvtObRPKS1sF2Q40+W5sfABB88HRYtix"
				+ "rR48+B3ECmIPA4eI38HChXTjYobfysKKcWJbW+Mx3PKPHJfqUJ4SJiv5jixO3BG5+KXXy5e+7neQwv79FdzVLAiuAsgWE4oWI6zU"
				+ "Brz4jNfxQAFhILFKhcFMl+wgsVOuPROu9Pmdthw54Wf+XIcCJa/XOJt+pmvcKpEsW0FtnoYkEkAsxXGXMXPNp1sKZT9ZpufXKWf7"
				+ "X6zxTF7sGWR02B8tp7eb16jgpWKbS+gJTdyZrpjZQrPRK7iZ5tOtuRyv6y+scoU7bqRiWz3m21+ovE5z7ZEVpej1fEoudpvtgTp2"
				+ "7LIxaA89vR+8xoVrCgsCiwU1kxBUXIA27OuhfHF53yCjfd8Gnk9LNrWlR9Ey2PgbjG0T6OClYq9o9FMQVGUXoSiZYzl0YT0aVSw0"
				+ "mHuauG8ovQmLIfG8IfC1cdRwUoiVCnjYaliKXlAkoelZVIFKxXjXWnBUPIAFkN+9dnMY4Gi1cdRwUoivJ2ZgqGFQ8kDWAyNaGl5J"
				+ "CpYnQg9LN7dcv9dQkXpPkasWBBJWCb7OCpYGWEB0dOj9CJGoKBaxsEKp30cvSJTMQVDS4aSL1C1ePOkBSF9GRWsKNECYURLS4jSi"
				+ "7AI2vZUvYkaVLCiGI2ydzPnGKnszxEDFOWw4xx1jiNu7GTMjU6USdUsFaxOdNzR5qKULHIXfPGD7tmft+N5K0rPEisRd94Vw52xJ"
				+ "14DB/8GcfxJCaGiaPVxujuCYyGS9a/rjJu5Q2KlDZibjMIxEDYCdp64pXOdSafvdsadssl/46kWdc+VXOMc+zbHnXbeSGfi7AvhW"
				+ "XF8rn9HaH9YI0rys7iDXiM7Vq/wt7zapwtfMUt2umNjWLofvCO8vFrcMy+eiSB+WOC9RrQCONLkL8X3Fsnerc96y3+WbrC2A6efj"
				+ "IYH9OlwePA1Ulp5DsoayxyHjY6ZtQ5HOfUXSbz9Hu+hhamfISNdpV+UfXJ4YMXKwR9b9RBxZ55fK7Hy6ShE/NovPyXP6jOHSeEQw"
				+ "fdIW/N3vCU39tC36JQ+Aat/cy+bIE6MY/FzcEOOZMpyhnLl/0Y873pp3rfGe/zWg/Hqs96gEFDBOgDuWZ+tkLLqdyC5i1EGTsG03"
				+ "JQFx3kdhWih+PG/eg9exyF8FaVbONPOc52hU44RFx6V73wSQRyRFfgcLvrvsOv9ba8/5z//u0PxklSwCozcHVv1YHFP+cQYicUuR"
				+ "LL8Us1kWHAn9OVhlI0fSFvTI97Sm/JqiF0lz6gaIO6sC4ehHL0dQnURQqahlLIdmePLL0c5WiStzXd7S7+fC89dBavAyPmxuWfC2"
				+ "yrvN1Uc90J4VxdiD25YLPjBh/8T37vee/B7HFlTUTphvhkgzqXwzk/DYp2p5jnOHmjLN1B2fi97t77pLb+LzQ65QAWrwOjRY3MXf"
				+ "PFM7OFy7GY2isaA4AVVZz8K4TdR+H4rDTvWe0/ckavCpxQo7lmXVEppFTwp5z+x+HFMbbnksMz3ixe/ATc5flIt1xSlYGm3hoCsx"
				+ "c3f9MJ6Z/QJD8HbWout+emrYUimAgUSQubOlPLqUmfsySv8dU929emn3rphZNpvvuWnp+m5/VbWiXv258ZJScWl4jpfQbmYA2MzA"
				+ "m9qf8Oe/1taGm/3ltwQ/R5lb52HgqGYT9BhOTbnmLe6zqhpR0H7/x8WP4O98tt2hN0gHkDhvN7ftvIZ//nf2o9/KkWO8arK+70Lp"
				+ "ePTWOQXovmRW/IKysNCeFV/95bcuFm8HnXAtUpYYBz2Y3PnXzlGnJJvY8/8fPxQFBkX8/wq8X3ix78nLQ2rvUduSfcFZqXAcYZMF"
				+ "mfqO/pLSdmJ8KT4hWh+8t6BQLVheQ2mv5a2xu96S28+XB+70CphgXHYBctfs2yvM+6kByQWexW7L0MO+DSxHDYd1cQzUZirnQmzN"
				+ "iBedz+GqRQCpZXinvrxWRIr/QLE6b8RAo/blL9dWP4pxOrr0rDjXu/RH+f0y9B9EfWwegDnqPmOM/qEYeKWvBf3ORRimYhCyzUNm"
				+ "Ee1wPua7N/+D22UL3zcuZfXQag+jt/2Ivy24xBUit+cv+vjuIXdIO0tS72Hf7Cvh6t/6dAqYYGRF8eGauIQCBefEF2AMjQG2eJrF"
				+ "6gm+A+J598k7c2PektvYrVRKRCcMSc4zpHzBuN3fScW2U3haNyQHEzZD+91/M7fkub993uP/LA3q/8qWAVG3hybM2Wu44ydeTZy9"
				+ "GEUI77UOijM3TYU9DtRtn4tuze95D39C22Yz3Pcsz9fiSrgWRAn/Jb+exHEKj9wVuLPr0S8u70HvgvR6nW0DavAyB8x3rlWJN663"
				+ "hkw5jFx3KcRMhXZG44c9sN0Jgr/mVJRU+mMnPqM/8YzOpB8nuLOv2qolJR/D7/ZZfjtZmFait+uFfZHCNXnpLXxj/CWtwbVf6UnU"
				+ "A+rF3DnfKFaSis+iCxegMLNp0ol4apnsXydxNsXe4sXbg/DlF7EGY3q31ELjsCN5j34nT6H3wc3GlO09mF+KaY3yv5tj+Vhe6RWC"
				+ "QuMAx0b16f7UXMVngkT3xl9PC6Et0wWN/YpLL8bNjZcxyeI/8DFcIs07FjuLftpG5Zzsd9MZHtc+bbfbPPT7fju3MsGSKyMw75cj"
				+ "EX2pyqFUZj+hSTuxo3lLu+hhXxh+VDoqePN5pwUDDz4YqUgjs2d8/l+UlJ5krkofP99YTALG99J/J3E2272Fl+/yYQqhw13wdUcn"
				+ "vgK/C5z8LsMMcXJ8dmH6jos/0oa6ld7y37Cm0m+ooJVYBTWsZWUQ7wufaepdojMgNWYIufIdlwgXxUv/hd/z4ZN/jO/0ob5HsI94"
				+ "6IyKa8+CtW/j5rfwZeSsBRthsHr9RZ6D3z3ZROS/xSlYGmje77gQYcad77mDJm8GFmnRzUO08E4in6Yn4eLaJpTVec7o6a/5q9/W"
				+ "kUrl7gcSO/yYVJWeRHO+VchVu8wYiXGo3oYv8E10tZyC6p/Ou5ZL6MeVh7invLRmNSM4GinH8fiR2G1mOcqvih7v3jeDf66J1/xV"
				+ "z1clHfRw4k7+1NlUjlgPorL51FiTkdQVeCcOOsxvQFe1f3e8l+sk710sgoKrRIWGIV/bHWjxD35fI5M+W3xndNwRINQPcRxmTGU7"
				+ "oJb9kNp3L3ee/x/87ktJf+oqBH31AuqpbT8GNwIrsW5XYBQ1jbiOMdv4lL/g8Rbv+4tvr6QX6FSwSowcn1sTK83CoHjzru8v7ilb"
				+ "0cOzsciLy57bMthi8Rru897cOGWIKhoydn5dxdcPQ1nlR984OixfJ2GcPTYX2MXi6R57xPeoz+yvdR763c/VFSwCoyiOjb3xPfHZ"
				+ "OD4keLGPobFz8BGwNvCxHhbfMz+FX/zK0/4L92vHU8z4J71uSopr3o/LuUrcN4mIYjj83soKTh/cr20t/3de+rn9bK/KLrAqWAVG"
				+ "MV5bFUDxT3twrEQLj5N5Bhcw2EcGK4JAvZHlNMfS/O+pyIeQt+G52vWx+vELXsLqn+XI+QkGM8XR05YA7tR2pt/5S2+cT/miwkVr"
				+ "AKjmI9N3Bnnx2TA6LfhMPlO27m4GGvCVW+grP5EPO9ef+uKVf6Lf+qzHpd7xmfKpKL2FFT/PoRFVgGrzQqRDThH9+C8LfKW3vyct"
				+ "BXlt0OKUrC0W0OB4m96wZfy2pVOv8GPiOu+gMOF5+APwJRfC56Fi/R0p3qI51TVPe9ve70oC29XuHMvrZWyftdAyP9LfA5PLBUIj"
				+ "uMcLcalfDG8qnu9J3+2Xpr3BhsoBYF6WEWCO/+KAeKWXoCL8XxclFMZZFY4sgRhN0i89XFv8Q3FPXBgaQXbqcZCrP/NVP98mRg6G"
				+ "nShnsC5+IG07Pub93CfGPVVq4QFRp8SLOKe/EFXBow7DkfODpDvRJVnGC5crtoG+zOWfyS7NzzvPf3Lout46s65tJ+Uls3B3Gexe"
				+ "Aau10qcAx7nChz3b8SL334YxlHPJ1SwCow+J1gWXLz9paQc1ULnUpTbBeZU+OZp2Csoxr/CRXub99DC+jB6wePOv4qv07BB/VzYS"
				+ "CPSvk+x+hFm7pTmfa/2wYcQKlgFRp8VLIsz+UzHmXDaRzHHESGmw+B1EH8divOXxY8v8bes2FaIXSHc0z5ZKlV1E8SNfQDHdzWCq"
				+ "oI1shNi9TCmC70HvvNkENQnKUrB6muN7plELNvwbMnVfrPLT/16R0qrXnBqhi7FpjvgeUxEKBvm6zB/DrySI53qIa3OoPGr/E0vU"
				+ "LRys9/s08kqHB5knVTWXoD8fxVRIFimQb0dl+hTuE6/IfHWG1H9W8kuViE9mp8u6M39qodVYBTzsWWNO/s/+c7cDAgVvC2H42/RI"
				+ "2GhZg/5e1BNvNl78mfrZP/2vC3o7skfKpEBY1nVvQyLc2G2KweHmv4hDudef9Wjr/trlxXlxZolKlgFhgpWGpwp8x1n7Ekz4Z18C"
				+ "xf5iQiqC06VvxXTW8Vv/5ns2bTBe3pR3jTMu2ddUgFP8UgI1Rdh5yHf8KgcuE/+ZmT9H+LFv+I9eF3BvZ3cw6hgFRgqWF3gzr9iq"
				+ "DgxeFrO+RCB00ygj3+OLMbMIom3/zEHo2keMu6CqyZBXD+InH0I+TwqvAxbkc/fQ7gWSVvjYm/pzfrVoc6oYBUYKlgHwJ31HyXSb"
				+ "9A4CBc/qf4J2EAYRMuphxgsx+x/ec/c/aLsepPRDyvu7M+gClvLnvxfCYTKRxXWwUXo84s0N6IK+wdU/bb7a7T6lwEVrAJDBaubO"
				+ "ONmOM6Rc6bAk7kMxZyjng5hMOb3odwvgljcIQ3b/3U4PrRgPtBRUsHPaF2K/Z+J/XPUT+zXeQOrb5N42+3F1CWjB1HBKjCyOTbGT"
				+ "fcDZwrPFbnab07y6Z7+yXKpGsQGeb53NweiwW4QTJcfB71V4vHf+y//35v+lldyfU4c99RPxKR66HRxUf0T4YgUHPuLq7Zj37+HL"
				+ "fKevecx2bk2l/vOq/MPcpUfkov85B36LqGSwH/z2bjUjXrFqey/FGK1EqdwJs4in8QNxvxsiMkpzrAp+2THmhXSAucrR7izP1Uh1"
				+ "YOvgGxdg0V2/uRLyqyaPoPJxeK1/tx/8f6Vsn1VUV6ESvcp5otaBesQcedfOUzc2CU4le+FcEzG1J7T34vv/VDaW572lnz/oIdlc"
				+ "eddMVzckgVI9gqkf1z4k7VBqp4Rx79NWpvu08/4HzRFKe4qWEqXoJpYKpUDZ8C74rf53gobAOPFsBGT36Hadqu//qnX/NeXdPsCc"
				+ "c+8uFzKa06DUPG9v/kwenHcfi0m90EM/9d//g9r++IoEzlEBavAUMHKIe68yweLW3o2Tiu9IX5UlIQfFZU7Jd76S2/xDQesJ7rzr"
				+ "xqDqt+lSOffkc4EE+jw6Z+wcf/H0tb4vHZTyAkqWAWGClYPYLpCVA+lt/VxCA37RXGYYZ7tlyA4X5J4+xP++uX1/urHEheMO/MjM"
				+ "ek/YjTm3omI1yIuvTRs5u9BGk9i+n3vge/83YQpuaIoBUsb3ZWs8Dc85zmDxj0j5bWPYhEelTMZolON6VBM3ypObJwzYGyDlFWsl"
				+ "R1r2Eu9UvoN+jDWsT/VBdgGcc1Lfi/C/gez13nP/PJf0py7RnyleFEPSzlojBiVVp0OIboEdg7u6WU463ylhx8cvQue0zKEn4/wc"
				+ "xHOTqmBVyVyO2Z+ITvXveg9+5uCGymiQNAqYYGR62Njer1RCKL7zYc8dMKd+VFW+fh16muxyNFO7djpFm7LT9E8Kl78S96D31tpQ"
				+ "rNDz3929EY+exyeiGLlQMeWqRDkKjxbCn6/7vyr+DUffuuPHU85/hahB/VXbPJLaW/5q7f4RjuIeq72mzE/WVJs+81F3vIOHnyxU"
				+ "szHlre4Z1xULhU1E+FtcQRQfvr9DvHa7/a3r9rsP/97rf4dPopSsBRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFU"
				+ "RRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFU"
				+ "RRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFU"
				+ "RRF6VlE/j+hSydA6QTo4wAAAABJRU5ErkJggg==";
	}

}
