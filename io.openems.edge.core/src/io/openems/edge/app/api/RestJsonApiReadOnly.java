package io.openems.edge.app.api;

import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;

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
import io.openems.edge.app.api.RestJsonApiReadOnly.Property;
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
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for ReadOnly Rest JSON Api.
 *
 * <pre>
  {
    "appId":"App.Api.RestJson.ReadOnly",
    "alias":"REST/JSON-Api Read-Only",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlApiRest0"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-rest-json-lesend-2/">https://fenecon.de/fems-2-2/fems-app-rest-json-lesend-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.RestJson.ReadOnly")
public class RestJsonApiReadOnly extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		CONTROLLER_ID;
	}

	@Activate
	public RestJsonApiReadOnly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-rest-json-lesend-2/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlApiRest0");

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.Rest.ReadOnly",
							JsonUtils.buildJsonObject() //
									.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new ValidatorConfig.CheckableConfig(CheckAppsNotInstalled.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.RestJson.ReadWrite" }) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw"
				+ "1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs575"
				+ "3DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93FeBb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxO"
				+ "fG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2Y"
				+ "rTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+Ag"
				+ "W3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAA"
				+ "AAJcEhZcwAADsQAAA7EAZUrDhsAAFR+SURBVHhe7Z0JnB1Vlf/P6+50OvtKSEIW0tnDGtYQQhJIugFxFxHUGZVRZ0YdZUlwdFzHn"
				+ "QCCyqCiMm7jvs3fccYQF0SCIKLsyBKQPUBYsm/d739+VXX6nXf73qp6Sy9V737zOalzzz331q2qW6fr3qpXVaCOteTxeDxZoClae"
				+ "jwez6DHByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHLI/Hk"
				+ "xl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHL"
				+ "I/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8m"
				+ "cEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8mcEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8"
				+ "ng8mcEHLI/Hkxl8wPJ4PJnBByyPx5MZfMDyeDyZwQcsj8eTGXzA8ng8maFAHWsjNWdcuw7/FwI9PUWWpDLapxY9v6x4VxMNHf4W1"
				+ "s7kTf53uuV7N9Jzj4R5venLfYU6AOqx1a3zgc0HJOlYgji9WmzrAy49pGOtrDtX5PkKSx9MjU6beeUHvTc2f1d9Sbq2Ca4yJroOl"
				+ "y7E6bY8bdPYfEG53rl2OQerm1m/muVUlt/TMWd/izounA4HxizrOulsx8+ma5tG6pNjVOl6bP5x9QjaBl37V4NZn+DSc02jDAl1Z"
				+ "9EdqJYDjfJSh9Tv6kRp1+Oqy0TysHTpQpxuy9M2G3q/lfTOi2ayfJ+Tv+HUUYEtzEb+66nQdDfnf4hWXTA8yOpdjwBdpwXTRyNpV"
				+ "1kTl0/StseRZlvStM0TQyMELHQk3Zlq6UC6LiC6tpmY/kll4uqqhHqfHLrdUjemFEZwIPoY63ezvDawcfaYqQfSrJOOo6EjR8APZ"
				+ "igfoeaWe7jM2XTkq3TfQ526ftGBzZ5WF0xd+5S2pVwXqtEl7dKTqPexyw3NNPvESM0ZmzbaOke1HciGWVanoaPTVVK/9jdPHJ121"
				+ "WuWifO1YVunS+drqDOb6dAz3sgXUD/h1BksQ2BuGz2S2lecQNOPX0wjJk2gSYvmUnNTE+14egsVu7vhMobLvIZGTFhN7SfcwcfpC"
				+ "RgZWYduh9aB2Y60OpbA5uMqB2x5NrS/qcfVIb7A9Ae6LNB5Zt1A6qC8ntd5D1hmB0iD2REqLS+kLafXoZe6vE5ru8bMDztub3/X9"
				+ "mGpfc1yJTovOoGGjfs+B553cWo0TM1DWmj6MYdT+6oTafiEcVw6rK7AwWoUX21NnNdOXbt2087nXoA7mME+/0Czlx5Ms5fdRA/es"
				+ "D2yC7pdGqRl2wTxMe0al49rPUCvy6VrXHXopQY2EaCXogMzDVz5oe4DVsYoXWHpA5sG7V9JWXRg7W+mXYhPJetyoeuAbqvT9ElDW"
				+ "NfqNdNpzrL/4E27nIPNtCCDcybNb6d5p66kMTOnBgHKRnPrEBrXPoPGTptCuzho7d2xC2YuXTiSl2/nwEU0a8kttOnGrtAeYLbPt"
				+ "GMpuuxvl7jybYjd9NFp0XXaXIruwtVHqu1LJXzAyhhhwILgYIPKDnh16HUkrc/WCSu1mUuTNH6wAdMPhPqq84bT3OXv4+j0HU4dz"
				+ "Va2F2j05ANobucKmnTo/CAgacIKAsdSrUzryBF0wII5NGzMKNr+9LPUtW8fzEPZYRXX/3ruj4/RlEPuoUduDfwZs02CaYvWEGDaz"
				+ "bJA+wObj8ZVfxxSJmn92k90nQ/MtEbqL9XjA1bGKA0JhbgDbqI7jonOE713hynXga5PbMBWRhNnM5caXV+cH2w2vwLNP6VAR535O"
				+ "mpq+SmnX8lVtuKSChPp7cuPpxknHM0BSG76AakKgSoKVqBHCSlwHRg2Yn4L+o6nn6NiMWguxpJn0ZBhK/iK6y/04MbNMAaFetUSo"
				+ "G3Q9X4Fps3lD10EaL+0uMroeoFeh4gcK9EBdI2Z1khZwQ8JM0fvIWHcATeJ8y3vGKWlzQ7MPCA206+eVF/f6MlEy//5aBp70Hc5o"
				+ "lzAljEwN7e00LSjDqPZq5fR8InjeQ21NbmpuZlGHzSZJs6bRft27qJdz78Y5dDBXPnbuG8exMPEm3iYuJNt+qSWFduWSTZbWoO0u"
				+ "S7xMW0iwGaHCDqt7QBpvU5gsyUh/n5ImDlKQ0KQ9oAPJOhsAG2tRQdIi9202SjlrV4zmaYfeQUHoy9wamZgYybOOZjmnYZ5qmkca"
				+ "Jq4BLsXomK4OgpKh1VEV0uBHsa0UlprQTF2aBk6hMbPnhk8CrFzywu0bwfHp0IBk2E8/Gx6O7Uv3Uczj7qVHr4Z81tCWE1pqTFtp"
				+ "dVWjms9cXXa7PAHacqI7vJ1UfL3AStjlK6w6o3uqKYOkLbpGrHrZSXA31ZG6jMRfzOvZFt1XhvNXb6GA8X3OLWEJah/5KQJNLdjO"
				+ "U0+fCE1t2JEGFUV5Ab/8SKyRcAHpsAcoPKiJQjrCrTg/6GjRtIBC+cEQ84dmN/avx/mNnbspOYhZ1H7CX+jFx67n3b1XImlRa82C"
				+ "d0oXc6sQ/vJUgTIUnDlmbopQOsgWfcBK2P0TcCyBRdJYxmnawGyBGZ+kgg2u2kTu7S93D77xAIdc/aro3kqPPg5FObW4cPo4GXH0"
				+ "cxlx3IgGWHdcJvNxO3jsHIQG3HAeJq0aA5Rd5F2PLslumKjCZx5Dk095ASatfTPfHyfCQpkG3MnSDrYYAfwkXytl+MDVsZIDljuc"
				+ "8mNzb+SOmzrlLTuhJpK7SBuPSU61h5B42f8F+e8l7PHwYR5pamLD+GrqpNoxKSJwVVSuKJwEj0IHqxEKWVnDQrbQv8wp2A0BbmhF"
				+ "UtQ7iH5wfzWtCk0gYei+7bvoF0vbA1y2XM2y9tpzokH0KzjMb8VPB/hMfABK2MkB6yk/Dj0OeaqR3zKz9iS7srXuOwgLi+ezrWTa"
				+ "PaySznCfJFX3y5VTWifEcxTjW+fTgUOGCFh2JGVBcO9KFVuj5TIpnKiZYguK3nao1SOdVZb2oYGQQuPUOx89nnat2s3nDC/dRwVm"
				+ "t5Ks5fupAPn/5ke/UvwGL0nwgesjFG/IaEZUJAG5edZb8Rm+kl5gLT2MwXY7BBgs7uE6OR/GUrzVryHkz/g1DIWPvF5CDZxHM1df"
				+ "RJNOfIQahnayubQPZa4MJumfIUMHT2KJi2cEwxVMb/VvT+Yfx/G6zqdja/hfvwgbb7vQdqLG4qZQu8s145z+bh3tA9YGaN+AcusB"
				+ "2mxaV0Tezozupz46jJxOhB/IHkaXYb4CoToxHNfSs1DME/1es5qg3nIsDY6+MSj6eDlx3NAGAlTQHlhAxn7SQucjgqXn2kP0hbny"
				+ "I4bh/h9Iibmqaubdjz7XDDPxRkH8H9vpOmLj6b2E/7Ex35LWLCB8QErY/TNpDvQZ5RLF5A2fYBpN8vVj861h9Dk+d/gVXyQg80Er"
				+ "KmpqZmmHL6Q5naeRCN5qFW6Wxc2KUzZNocpjf3St9rlZ9qDtMXZsDe1tNCYGVODIezebdtp94vbohyax+3DYxBjadbxN9NDf+DxY"
				+ "4PiA1bG6LuApes11yFpLG1+tqWp10dWnT+R5p70aVa/wjKPbbwo0PiDp9Pc01YE80KY2A7dBZdewoy+Nj2OdGXsOWKVZQtfIY6fe"
				+ "zCNmjSRdjzzHO3fvYet3KcLhRN4497CfXsrTZj5F3r8DhRpLHzAyhhhwHKfE+V5rg4tdQhmXboO13q0XeqCTexSh65L+8Xpvf1Pf"
				+ "k8zzVvxDj5hf8jJlWwLZs+Hjx9Dc1Yto6lHHUrNbW3saBa36SUCK/8XXmCFdwJxLSbeEOjhf0xk0BdkOitYRoXFrrWy6zz+D8+pI"
				+ "lUIHliFDsI7ikPHjKIDF86jlrbW4DU23V3B/Bbev/VSahv9Ch4m3kdP3vUw7Q8C2kATbXUf4wNWxggDVlzH0Hniawow03JWaRvQH"
				+ "VH72Dqoadf5pq8Nu3/n2k5qbvkJ1/wmPrODH/kN4eA0c8lRNGvFkuDExgkuYQaPKIQBRaooNVvywpAQeYhbYAsTPSYm8OP/Aol0T"
				+ "WALVYbXFTmIPay1VDcINP5P8kt6lI6WhaYCjTzwAJq0YDYV9+8P57dCJvN63kQzjzmM2oO3QTwf2QeKsNF9TU7P6zx/hEI6Ruksd"
				+ "KN9XPrgZfWF8/iK6hLWXhoauMFNTTT5kHk09ejDgkcD6g72TB32Sk3VxBTeteUF+tuNt9CLjz0VWQJ2cyS+nIpdn6INl+HBrr4+r"
				+ "rYWulpdpz0akdOPUOT5Cgv/6w4AXTqFHEwzP27Zn7jWWW5ffQGP8076BEenazi1MDQSjZ0+leadtiJ4aV6hpaWnkHlG6J3hWqGTi"
				+ "gvYkOu3EuVt0SlLK213FCOGDG+jCXNn0YiJ48P5rT172VrknVFYxvvrzdzvn6NpR9xGf/sjKtGgQm0z00K1dp0PHVRid+lA/P2QM"
				+ "HOEAQvgAMoBlSXQBzktuvzAsfTcZlrY8VY+8X7MqdUswTzVsLGjac7JS+mgYw4PHlkAeq5K69js0pDKjjx13nf0rrvcolPlrdfnp"
				+ "lPnxbBxY+hAvKa5dQjt2LyFusPXNI9ieQW1tL6U2pfeTc9uepT2bEchiFQQVRKrC5XoWIoIZp5g2uPaBkq6D1gZozSHhYMM5GDae"
				+ "rdNgM02cAwdSXTKe1bS0BE/4tTbWIIvPOBhzxnHH0ntHKza+AQtnzxKo9sJPZL9+ouelgRHzbUtJb0nIOM1zZMn0QELZlPXnn20c"
				+ "0vPNNZUructfKW1gGad8EfuM/hVtatiYOaJgLR6rdjq0H06xAesjGEfEgrQdSCTA65tgui9O0V/0rFmFh183Ff4BPsUp6bAhOenc"
				+ "PUw79QVwe/uSs9TldCNdm2Ae8Pi66sF1AOqqosLSTtKy9LVoM4DoZU7+5AhNO7gaTRu5jTa/cKLfFG1A2ZkH8o77+18LrQGz29tu"
				+ "hHjxyT0KjRJdp1fqa6Js/uAlTnKH2sID2KI6NrusokOtN5/rDpvFM1d/mFe+ze5CYexJWjHmIMmB4EKT303DWmByUqaDUi7YbIzT"
				+ "Vz2OOAP0WVtuivfzCsfuobBy/QF0IeMGEYT57fT8PFjacczW6hrb/CaZrzjeQVfjv09zV76NI2feRc9cacUqydoggioVpfNt9t9w"
				+ "MoY9iGhHGSg9YHE3oZDXtJEi1/5JmpqwWe0Tme3ICq1jR5F7SvxGa0jacjwYTDVkfhd4spJ3Ikx1WqzTXflAzMt6OCl69H+uBodx"
				+ "gFr0qJ51NzcHASu6DNkozn31TRsTCe1L72T+xE+QybF6yG1outw6z5gZYzkIaEgupxWWAKbXZfrOzrWnkijD/wBr+4dnMIEcTCcm"
				+ "X7cEdR+ytLgyqCapiRvgDtXly0Nv8qn5ct9VG09Cqh2N5bKhVq4boD/K61V/DG/NXLqJJo0fzbt3707eBwiYjpHtXP5/Gin9hN4m"
				+ "LgRj0FoZLUmSfY05dLo8eT0vG6E57AADrIcbLMDCJI3cKy+cAY1NX+atdexBG3BtNQBfDJNO64vrqjisOyOVHto4HdjLezY/Cz9b"
				+ "eMttI2Xiu1ULH6GL8EupQ2XJr1/Cxsv/Usj9kp3TnU71D+HlTHCIaHGdtB1JzLzbR1FpyvV3Zz8nhE0b8W/8Z/7b3NqMUtQbvSUS"
				+ "TQPn9E6hIctfIXVv1ianrg1pV1W3Vk28OArQLibiEdE8DOf6DNkeDf0KXx83sDDxCf4YNxLz/0NdmyiKSDOXim6jGu39rb7K6yMc"
				+ "e26SOkBBxQHFqTRNWLHsn4sXF2g6YvP5mpxVTU9NBaDd5vPWHJU8OFR250/F2YDx7Q206S2IYG9u1ik/Swv7OmirfvCbzkMb2miq"
				+ "cP5XGQdPvu7i7S7q5ue2hWcpDSOy48b2kI79rFtN2zld+Kmc9nW5gJtZv/t+8P35yG3fdRQmjFyKA1pKgT1PbdnPz28bQ/tYB8M5"
				+ "Eq7E4Q1TuD1oL1bdu+nF/btD6wlD/bmBHaF2EKkrhDRxKrLA+1druuBbYlubseTf7mLnrjtbnn/lvA7btD53Md6Pp6o0KvWiN1cA"
				+ "lm5Wc7mm46cntd5HxLqgy2dYnDQseZY/ov9WdaWhgbMU7XQ1CMPoclHLAxeoVILy6eMpo8fi2mYAu3noCFsePxF+sSfH6f5Y9roy"
				+ "pPaqa25ifap/F0cVF7yf/cG+ueWHkxHHzCS9nD+6f97Dy9L58yMEa30vY75hBD0/QeepSvuDH8C88+LDqQ3zZ9Ez3Hg2ckneROvv"
				+ "5UD13fY59ssJqgRrw/9Idc1Zmgz/fWFXfTO3z+U7oj101Hdu20HPXrTrfTsA8FVlYAIdg0Hrg9wX8P3EwXpby7M/pikS12V2X3Ay"
				+ "hjlvyUUfeBZfcFUamr+JDfpjZzCuRo0buLcWTRtyWJqHaE/TFo9646fSSOHNNG7bniIVJzp4V8OmUynHDSGXrfhPtqLl+CZsOmrK"
				+ "2fTXc/tpDNmjqMP/fFRumFzz3un6I1zJ9IqLv88Xz09y8Hpk7c+HlwB/fKMhfTzh5+nz91V9hu+WBaNHUZfWtFO7/zdQ3TV8nZ6O"
				+ "QfMLVxvXYnrBa68wB5l8mL7U08H81vbn+n5YTXYykHr47R76+fp+i/p10HErbFe6HWUry+nc1jBCZNTcMDkoGldMPNdxOWlZ+U7h"
				+ "lHnRe/nSye+fCn8PVuCfT/qwIl0yKtOo/ZVJ1Lr8OFlKyvpvZsAi2kNh1shHKvoqZ37yoJVWT4P5TD8QrCSuiQ3WHLXb2Of7XyVd"
				+ "NPm7XTSlFFsDz3w//LJo+n6J7cGV2S4SsNbX/hCioeZzfTA1t2BTyAYy0WgPNKSJ/8jcN76zA66/fmd9PiOvbSCrw4FeAzn+ody5"
				+ "SiJYeriCcN5KBvO6QV18r+RPLzFEBT+Bw4bEvhgmBmugf24baKDMh2nuS0TdokBvBg5ZRItevXpNHvlCcGrmiNGc6S+mIaNuYM61"
				+ "748mjtCDVKrrWbTHoetvIlpT1t35shzwIp6WrAUEXBAdRrAJgfatayEsP4piwrckc+k1pF3curjLMG7iIfyldScU06kha88lUZw0"
				+ "ArgErpRJV1bQ4J3U0W6EMzD9LS0NAcUwIGiNE+DnJIOTQSEyyIN4+CDgPT7p7bSMg5QzVEOAsEhHBB+/9S2YJiIuTDkIDhu2b2Pj"
				+ "po4IuhYQZ1qDg7rRzqw91jCgHXdE+FTA9dxEFw1LfjQdADy33PYFHrnoZPpA4un0Q8759Mnj5sZLF918PigBvy7eMlMesOciXQpL"
				+ "799ylz6DC9/cuoCWjopfPVzaZ0hvXRXpgHaP3HBbDr87JfTQYsP4Ytl+VgHzeHcn3LAWs/HGw/46t0PvexwMLIW00+w6Sgjus7XL"
				+ "db23JHngCXYDqAceL0EcR3CVk8cRepYs5gOe+mvufj3OT0LNTRxEJh29GF0GHf4CfNmlZ3QFeEqpuyHjR9O/3rkQaEsPojee8RUa"
				+ "gnyw82ZNXooXbmsnWVWjyyeEPw8kSkEV1i4AtvIQ0FMvi8cF15VnHjgKHpm1z6678XdtKsrvMISrvnrM/QSHkL+5NT59MGjDqIzp"
				+ "o+l8a3u+biFPByczFdL13NQxP657okX6UgOeAiKJYr08pnj6dHte+mMX9wTzKf98MEt9PaFB/Z0YFzIncMBC4Hv1F/cTaez302bt"
				+ "9Jb2SeRNEdW+7COH1NPO34xHX7WS2n8rOh+Schq3ne38tX0lbTqggM4LUcEy3DHhxg19gDd5pOGSv0zR54Dlq0TmAdU0ubSxGW30"
				+ "3HhJO6wX6ZC081cdEVkpQlzZtIRr+O/zMce0fNzGl0xdFtDxB6XB0p6qCGYPL1rbyT7AtH+mBj/6UPPlckTO/f25A/lQLRzXze9s"
				+ "LeL7nhuB53EV1kAE/o3PBnOZ+3mKzBcYQk/eXgLveFX99OPNm3hoDOELuQg+ZPT5tMyDnIyfAOi4+rqAQ58XRwYx3OQwjB26579t"
				+ "HJquC5pyy3PbKev3/807Q6eRi/SLx97gcYObQ6CnfCLR56n/37kueBKr4sjGG4wzB3TRhx3e+oJtVIqQEJKGdLSCO2jdLwUcW7nC"
				+ "lr0stU0YkLwaUeAg/sOam75K/eDd9Oq8/EpImCsuCxt5pkgX3zMckllc0OeA5bZxUQE0fUyTpe0mxXvaOXhwBoqNP+VU29l4fFCg"
				+ "UZOHE+LXtFJczpOotZR4RWMrlzQK7HZy/KiLtrLHhBaEQi+xlc8kK/+9Wm65r5nyua0XuRAtJ5P6muV4BEFlMZJjrt7MiGP4d9yD"
				+ "iK46jqWh1nBFRGDxxbaVMDC8OyhbXvom/c/S+fd+DC9hK+G/vLsDjp3waQgD/9Qo+gIWPP4KusXZyxiWUg/f8lCGtc2JLCH9ZWAf"
				+ "zjhVKCt3HZcnY4cEg7J5EI18Ik0+LRi/oslsAabEpZPplRTIuw46qDJdMhrTqdZy4+jIcN6XpiICHY5NQ+5jTrWnh6anOjViW42w"
				+ "dYkW7nckueABXAA0xxE10FP1wFGTsTd1pfR0JF3cJGL2RKcba3D22j2yiW0iDvyqCmT6vtn0NEy94aUUi4fDSawW1gwhwV+z1dUs"
				+ "0a10WnTxgZXL7dyEAIIWMPUkNBkN0fIP2zeTlNHyEVGaZ24OziF7W/+9f3U+fO7e+QiDnTlw0LVykhtbQrXiWfHAIaE5rYgWOH5M"
				+ "1y9Bbg2tk7gZz74beIRZ78i+CpRU9RGZgFH1P/hq61fcD9ZENkEW6vQYNhlCXTaVqYhyHPAwsEVAeYyDdrXXq5jzaG09Nxfchf6G"
				+ "afmwtTEJwqepzqcO+6EBXNi56lQqa1h2g5ceXqp7YK2aR9caY3ArURlE6AjIKDde3gIhvTD2/fQI9t30/k8xLvp6e09dgSkYXyFV"
				+ "V4+HE6FUuRgNYR2cuCTNMD/uIp6iK8C72V5cV9XIHioFXNmO3iJYaF4m3vwsAnDaB8Hoid4CCmgvaF/WD/m8J7m/D0SsCy4c8rRf"
				+ "tBd5WBvxvvJlh5Nh531Uho386AwI+R0buTtfBV+OQcuGT9KVWaVpl0vta+p5zqYNcpvCYV6HFCUL9Ip503kS/2PcAd8O6d7ZogxA"
				+ "Yun1DG3UT21N/PyEw4OnpH66K2PRZZylvCw7rKlBwfDRgQHAVdM59/4t+DRgJ+dtoD+8boH6bbnwq8p45GCsa0t9PTufcHT72jiy"
				+ "2aOowsPn0on/7+7glZjYv/A4UN6HjLFVdKhE4bTN3k4etXdpecrsXU/6pxPv3z0BfrSPfq5y5CPHD2NDuA24CHS9x15EHVOH0NPc"
				+ "vDBg6UY4GF4uuGxF4KHYMGVJ86iBeOG0aMcWPFYxYiW5sDny3dtpq/f/0zgM1C8+OgT9MjGP9HO5/F+wB7wFO2HaO/Oq+m3X5ADo"
				+ "A98bXpOz+tGeluDYLO56N0hlr5lCC3seBc1tfyAg9VytgWXKXiDwpzVy2jq4kPr8NGHSppoB0Hlzud2BUsbj+3YG9xRwyQ77vbhK"
				+ "ueu53fR7Vt20qOch419cOsetu3sCT64msIEPK5swiYWaRsHu3s5iGzaFj4ziSEahpG72Bd5D3EA+a/7n6WfPlz+sRo8V4UHTn/zx"
				+ "IvB1ZfJg9t209O79gfzYZjsx897Lr3tyeBqbh8P8/7nkefpGxwEw5JFOmPGuODB1u8+sCUIVvgZ0Pd4vT9jvxB9KMtx5ySRrmQb/"
				+ "/GatGguDeF+sR2f2Q8/Q4YnhM/gP3qvoval99Ozmx6iPdthlwp1xaLrFcbp/reEmaP3FVa63uVi/HSio88+jYPUpZzq+eADOuG0Y"
				+ "48IXqSHOQyT2lZaIwO68vqBK6wJbS205g9lP40pA1dY97+4iy6PfiLUp9SwX/Gx18dvuZ02330fFcuHqj/jSLaWNlz6QJSuDf+ke"
				+ "+aRLqYPZJqDiuep5tMx5/ycg9UvOBkEK0yoTjl8AR1+ziuCtymwIXAVSrM4gszelLSSJQ2V+EbkIFgNSmrYr7j6nrnsWDrszDNo7"
				+ "LTgTdfCK6ipGU/Lf4ZWnYdnOso7T4lK9VzR6ENCHcR666suGEtzT/oUB6qvcSq6u1OgcTMOonmnrQw+k96sfqRcuhMnT5WX35sTX"
				+ "TSdm0wlvn2I3lN1IKwuvtKdPLTEsBXDWBfh0HQ3bd4FH6mrmsbWeQPLKNWNrxpNnDeLRk6cQDuf2RJ9hgxzoYUT+Y/fm2n2shdp8"
				+ "vzb6NE/y3gZBVEBEN1sqKRz+8bRRpt0T8eJb22mEePfxn3io5w6IOwaBRo+bgzNOOFoGjNjauA2+OnLk89TL4pd3bT5znvp8T/dQ"
				+ "fv36oBLtxJeY3PjNdfT9t5vuojFv8AvY/R+gV/y2TtyItGKd55CrcPxvb9z2X0EiuAyfsaSxTRr5QnUNrb0w9z+Qjc8jV4C1y72d"
				+ "z15Bg/BZ/YnH0AHLJhD3XvLPkOGTyG9maYvPoTal8pnyNKR0/M6z3NYOIchgj5rtR2E6RPejB/3bWDtcCTxXA8+937EOS+nAw9bE"
				+ "Eyq64IuvXf1QuUlgG54Gl3TN8EqrrVxecC1D5LK1Up/rstG8joxTDx4xRI69DWn05ipPb+BxAF8LXfG/wmTAahMKnTpuaSRJt015"
				+ "lks6ein98Xgd2GHvfYMmnHSsdTc1pbYC8ordAUJbU9Xor+orJfHtTZpS1z7oK/3QH+uy4Z7nea+Hz5xPC14eQfN61yu50j1uYrKp"
				+ "EKXnkvyHLBcB0/3D8d5Wgh+StM2fgxr+hqlvKi9Z5TuASbrMUQOvfyUXee5dZ1yk+tePvjAg1i7+MhgYuohljtYv7FYpPV8vH5cL"
				+ "Ba/TsXilePaZ3ym0NxU9rZAheuQ55o8T7pHSgXnYscaPEx1H9Qph86nGcuODcwavIBOfmqDYBCEM+4uxYLMFYUhorceuEVaud6T6"
				+ "lnUOO9UXnmdkEqxBKK7VuTK03ZXnSDOB4gduPRKcdaDmXB8LQdPduI1Fds5dzvnBmnuE9u4T7Be3M7dI7AVoAc+Bc7vZlthG0XL7"
				+ "n17du148O6993zwXNwBxEqwMtBb77zofl7OYbmX1l+8KLCFiC+WvcnppLu/S6hZfcEcamqJDVieTFAWXHqWxeJ2KnDg6NHZzgGGz"
				+ "2wEnzAQcfBhH04Xt3V3dXFZ9usu7tlyw/91bfr8B9mln+m8CP1RB6x0+ICVMaoJWMYV1nQOWJVXUhnuP5G9ifPVV2XaT18RpqOSF"
				+ "tUMVobfDuHHijrAIFBEOgIMBw2dX+SrGqIdoQ/nd3NgQQDi/N1PPLz99vNeU/pxZGX09cZXXr8PWGXkfUiY1Dl0B8IT7XPDgFWky"
				+ "YcuCJ5I7t3H7H2uzGp3iakp1HS+o4qK4XhFFcWrWIocCAq4esG7ZUoBpFyPAkwhvFphG68+CDDBEsMiBBwOMsWu/Tt2PfLA7rv+9"
				+ "Y0YGrk2v167QtMXdaalsu30AauMvF9hyUHTHcHWScIlrrCIAxZbcjIkxNwLgggCDIJH7wATDpOwDNM8ZIrmXEJbFGDg17V3966dD"
				+ "9+7/54P/oPtZLDtV1BvHUsQpwNbWRCnC7CZaeAq68Lln17vXMsBq1BpwMrt2xoaZUioO4IbNSSczAErvMIqgUpK9K5S97TeaP9QV"
				+ "6VhwFVGz9wLGxAoWC+G8ysFzK/QNr5aCuycv43toW8wHMKkbzcPjzjAFJq2de/euX3rXX/c++zvflF8buN6dutXem+spzr8FVYZe"
				+ "R8SgriTpfxk6hWwjmFNFcf4CulCEFxw9SJDoyiAcODA1YgKKpLPhVkvbC9Ek73dWGJSNwgwtG3P00/suv3dr6zzx/hiwYZJp06jY"
				+ "2nmAbEDl65JYxcdS5BWB1IWpNGTEF8sgU231RunAykLTB2EZXpfYem63PiAlTFKV1jpDjDouJADVnMQsEZOmvB/i1516pe5KIZFO"
				+ "7iWbcWufbiS2bZ/x9adW++4ed+Dl79PJnd1h6snuu3pt6P/cLWvL3UsgamDpLJA67WQph6XT/o2xAcsl+4DVuawDwnLD6qJCljse"
				+ "gWtX3d+qKdC6gdpdI8nGT8kLKNRfksoQUqWgnFQVXZ5TpqDr33S6AON3hcuXZO0TViaepxNRBBd29PooBY9LWZ5SbvqjdNNEbQeY"
				+ "TGViM3MI43w05y4DmGcnM7jbzuJXSd2PYlre6246natR7YX+TYdmLotT9tlmVSPYNNd7UlbpwZlBK0Ds7ykXfWKbrYDIK1FMP1sm"
				+ "O1qKBrhx8/oBDjI5oHW6UhX/SVULT49mOlqSKqj0s7cH7jaNJj1tNRa3oZZT4X19nJ3lY/rq7mhkYaEIoJD10Wc/vWiL+rsL8wTx"
				+ "EwLWjdJUyZNXS67gPwknyRc7UhTr6xfpF64+meW+1UsjfK2BukkurM4Ok7ssU7qFK78pHL9Qb3WK/sN9Wk9Deb+l3LV6IK2AzPPB"
				+ "Daxm7pg0/U6XLoL+GjxVEner7DisHScpCKBg5TTOoCuO7euzKX3J/Var7nNQr11tLdSHSBt6lim0YHWgdYHDrQqHu2R7J1R8j6HJ"
				+ "QdOOl1c52Nf3Wd7MA++TqfRG4Vqtjlun7mCRrU66q+0rGaAjimvNvhBaJR0k2YbMk+jDQnjKFBRf6Ckh77qCAN0AsRSy7ZWs2+kD"
				+ "PaF1gWXHkeaMmnr0lSzffVh4NY86GiUIWG6Q15QuyP4GU4vqunoLgZjN5Ttw7Ke25qE3hdJum6XTYdfJTrQ+iADTYVYcW3DIN6e2"
				+ "sj7FVZl6BL1eydLGgZbx+vXja+QsqMULQF02V9pdUHb+xJZh15Xgo6FNvcyuPRckvc5rMroliEhsB573cltmCeB4NKFSnzrTS3rN"
				+ "ndSvU+YuPp0nuhos9aFNNuVZntrRdaRpm2RjoU2lxni9FzSKEPCdOghoZ2kOm0nEXDpgwFb27B0tTPOLieKWd5VRrD5mjazDr0uf"
				+ "YJCN8sKLl3jsteTerUPuqRNPZfkfUioD5xLB64DbCtjnhxCNbprvQONbqdJ2m1z5dmw+eo6tG5is2tbGl3jsvcFtv4FRA+XpRxtL"
				+ "1ndeu7I+5AwbaeM0upYh6r2Ez1NR0mru9o3UNi210RvAzDT/UXcfh3syL7FMk4vGI80lOzJkkvyHrCk85pLYOnYmTzOlu2omrj9Y"
				+ "8vTy2oE2PQ0AkxdMPMHiwi2PLuU3/yx+9gll+Q9YGnMvzxaDw+wfpQh7Cj6wA9EJ0iz/rIe3YfIerA09WoF2PRqBdj0wSIabdN5L"
				+ "l1TqX9uaJQhYdKBDPO1Vxi8tCWpDhNX2UrqrMS3Wqptm6d6bPsZS5uucfm49NzRCFdYrqsUyxWLOs61P4eVZr2WNvQ7SW3Dsi8F2"
				+ "OyVCLDptQqot54kQOnaXJZIo+eOPAcsOWg68pi6cWAH3XHWDRqoxmE/9aUAm70SATa9VgH11uPyBK0zPUnD3kNM2XyR54AlB05Od"
				+ "HOZloEKFGAwdsQ0+0P7VKo3Aq7jatGx6Nk9UGz+2m765Iq8X2Hpg2cugXFgrcfZanSA9Qlp9P6mmvYhrQWYNlOALEFaPUlAJXo1A"
				+ "vpLTyFYlHVByQNaB1rPJY0y6d5f6PWl0ZOod2espn1IVyugEj1JQCV6NQL6WgeuPEMXNcDhE4D+ofVc0ghDQpDyAA6646y3QesDS"
				+ "W5Phn7EdVzTHG+bD5Y2PXfk/QpLkAOoTzbLiaePsz8vU+Dan3G6pLUOtF4trvoqrTutf1/V63HQCAELnUQEEUl3GksHsphKSCaWp"
				+ "h6XB7QOtJ6WasrUC1m33ofV6BAgS8FMm0gdALpOC7IuiKn3BX1Vr4FshqcRAhY6lUiKoy590NoXdabUJ7oguj5RTL1a9Hr6A91W2"
				+ "zaCSnWNy27DrC+uTsmL84ujmjJpqKRe5Vu2GbqONHquaLQrLEH08gPb84pkoN2dxHUYScfpSdjaXC/S1Ic2xvmZ7TPTgktPQyVlK"
				+ "61bU03ZSspUWr/LX9vT6Lki7wHLPHBmkCjPL3sfVuBqlkc6TkClehy6vWkCXCWkrU/8dHttullfmvqT6nTlA+0jOtbpKhOnQ5Laq"
				+ "8vYiKsfmG2rABSrsmjOyHvAQifRojuPnfK+ocuY6DqkfmDTUYfYRBe7kEbXxPmLAHNpYtq1v86T9gPRZTsErWtcPjYd66yHD0ir6"
				+ "zTQ2y2YPiCuTsGlVwCKVVS0yvUMfhphSGgS04G4nwZ9o6y/2vxtNl0Iuk7rcrayII2ugV3WIzoQf7NcXD0aXV7nSf2yTlCLDpJ0r"
				+ "L8Sfyy1Lpi69tG6C52XpGOZpPc1/bWefifPAUt3FME8OQ2wO9gdJeK/mhN4hGqZDkSXdbl8ZVkLOqjobXPp9SZunUlt0NsP3ebvs"
				+ "ps2nbYh+VIfxFW3DbM8lsBWxlWnzTcFWJVIKlI7ZpE8ByzpIFjaDqKlA2HSnc3Bmxos2W60s61gmsp0G106QF0tkUQRNrGsadN2E"
				+ "dQ3mmUkS3NkEz8g24Cl3h6XLpj5Sf4AdrRjLMtQGAzQLikLvZVlIssYFrHb6ta2OD8T2Q/w1evW2OoGLr0CUCx10SrXkQ0aYUjo6"
				+ "mCaML/sBX7RsrwsdJsAWYI43eav22i2V/R/Z3mOZQ/LLiUPs3yOBSc4EP+XsNzAcj/LA9HyHpa/sPwHC4DvahbYdrK8wPIiC+p9k"
				+ "uULLOC1LHenkDUsguxMLCFfY/lIpINJLCizNkiF9gksP2RBGyDPs+xgQbv+zIJ8Af4vZ7mJZRvL0yzwf4rl8ywIXrIuLF/Kclckr"
				+ "2cBsIvPHBa05+1BqmR3octqHcTpOm1iycMhiivSq/5c0whDQiC66+Di0qoY3CWUoBUu8Z9LQK26YAtQJriCwBXHh1hwkp/H8mEWB"
				+ "Kx/Yfksi4BggBN/Nsv/svw8kv9j+S3L7SygjeW7LDhZEfQuYDmf5V9ZcNLfyAKeZfmTkidYFrBsVTbI4ywA26a34wQWBIlvBakQX"
				+ "NGhDrQVwP/jLK9h+TXLe1new4Ig+EmWn7FsZwHwfQXLT1hQx5dY3s3ybyybWN7J8v9YcKUI4I9tXMgynuUTLENYYJd24koOdSEoa"
				+ "jswddk+U7dh2l1+IC7PhS5TTflMUaAO+QOXM65dl3TwzJOKrzUunEOF5vsi6xW0/mKcvDakrK4jTgfiD8yyGldZXBX9EwuCFq4+x"
				+ "AcnJa4acKIdAAOzlOX3LDjp18HASL16eSQLrlwuYbmIBZjrF13TyYLg948sV8OQwLUsCCTwlzqnsCDAXcoiV2Z3sCCgzGAJ/4i42"
				+ "4KrwkUsx0W6gD/CP2JBQDud5ZcsAAH+Mpb3sXyK5S0sX2cRDmHB+hH0ECD1utLsk3pSWkfnRffx/wi293J/xPamo2Mt6sgdjTAkB"
				+ "LaDZ+90yV3RrAtpsek82zpdxPmiRXH5+1lwotnmezCcAihvbhnSI0KVnmGRdWg/195I2jadfzLLiSy4erK1Q5D24GpOP8FrA8Huc"
				+ "JYNLAi4GpRFEASnRkvN9Sy3sCBAu/q/2cY0+6QSkvafsY4k9x5SO2aVRhgS6oPo0kPKuon12FuNTK2d2HVCiG7Wb/oMY9nLguAFk"
				+ "vwFGcIdxWKWMXFtuw2pC0sEqi+yPBqlbfWI/TGWWSzjWIBuk9bnR8tbWXSdssQVFwKX+Ol1Qv80C4aHuArTeYLNlkSaMuKjtyUBK"
				+ "ZKqSRXUm03yHLDk4GGpdX3ky3tBWcp67K1Ghc43fd3rrQ0MF05iweRzFwwKXN1gHkjLu1ikbX9jwRXH61jWs2Du51AWW7+I2zYXm"
				+ "Pg/jAUBQnCVhf3bLKNYMB/2MZZTWIazmGA+D2CCHUidssRcF64uxc9c509ZcAMCc3W29pi2NMfLVo9JGh8DFBHxNMqQMB3pPzyhH"
				+ "dMWiisjJwSWtsCmbZg7QpCB3MZyJwvuAmLiXRD/DhaclFowaSnrh9+ZLN9kWcKCiXZMyGNSHVdFmBPT6xZsNg3ysQ4EHUzm4w6eC"
				+ "/hJfZgPk3lDzCVhyLclWi5nEfAYA8BVpastu1nEzwRXX59hOZYFd0mTMI/XAJC0yxuDvA8JbaJPkHog9QJT15hpjZwQWOqTQ9sFB"
				+ "BMM5SA4KfGYwxUsmHgXxP/9LJjv0TKTReaI4If5qzexYNIec00IFAiAuL2Pu3BSV1z7BfFBGTwKgXXJfFIceh3YlnYWXDm+lQUT6"
				+ "LjLiCtA3MUDMvTFnUaUtbUNwUr8TOD/HRbcYUUQN0G+rjONnoZK/RkpIruoscn7kNAUgB6g0yXsT7eb9C5XbnPl2+wac+U6rfVzW"
				+ "c6O5HgWXHF9mWUqiw3XRpn2fSx4jAF30Jax4HEH3G1E4ABJ7QfigzuXeOYKwQrPRsXhah/uKuLZrTeynMWCAIShK5A6XUM+PK6B5"
				+ "9Jc64Y/thd3RzHsxH7UIF/XmUZPQ6X+DIq4dlHj0ShDQtsR720rGxIG2dpHdHMZ16O0r/Yx/ZGWlUuerlc3zAQnNU5mPEQp/rp+s"
				+ "322+oGp/yFUaVq0rAQEGTyegCGmRq9DSGoHkOfBpkfLB6MlHkWw1YnJdgRN8XNxDQvmwfCoQ39ga2sK4g5/Y5H3gOU6EUBML2DX0"
				+ "Fv7iC4nmKS1bmIrD0z/tH42fseCJ9MRsMRflng4EqSp39TxmATAkBOY+0/QdugInni4FRPteAJdo9chj2BgeJrUJmkLhsDgIZZHW"
				+ "PCcle3K8h+i5XXR0kTajKf7L2d5GYsMN4U021spettSUsvq8kfeA5Z0EL3UncbRgRzmsPeYPcjpzIgvfHQ50W22SkGwwomJO4L4L"
				+ "SDA4wEIBG9meQMLhlRa9AQ2frKC55Vgw93GlSyYO3oHC05omRtzbae2Q0dZBEpM2tvAFRuGmh8IUuFPhgQ8yIo7i9gWtAXySparW"
				+ "ACenwLYVxhuYtj3GxY8BIo6sR3wxd1ODCkxB2dD2owl2omf/nwUBkWa7e0rSn2h2B+ryw6NMCSUg1/ZkbffMRSjqy5thy5ptMHM0"
				+ "+j8Umct1zWmD+axcMWCJ9ABHlfAU+6YvMYdQMxHieDnMTIEQmCBDT/fwU92EPjwsxjMiSHvn1nwVD1wtUVAPp4Hw0Q/nr2SqyGzr"
				+ "bj6wlP4mIvDoxj/xSI++HkNfkL0Kxa0BfJjFswzYZIcjz0A+ON3jvhpEm4kfJUFdWI78DQ9hpAIfNIGE90mbB9+1iRPkePnRsgXS"
				+ "Usa3zgfnVfqH8GrjippRmXOWSPvP83BwTOXQOsg7CAda+ZQoQk/hQD4aQ5+zgHC/BCpy0Tb43SAtKseF5iTgeBWvgn+8OCOGZ7D0"
				+ "s9iwaaffsdEM668RACGcLjqwQQ2Ag7ahUcJcOVjWxfysT4ENNyFw/pke9CZEDBw8qOs2IFsK9aBQIr138si+QA+eJQCwzw8j4U0r"
				+ "iAxF4UffmtQDvnYPsxl4WFTbB98cQdVrxc69h32h+wDsQPoMnzGNsm+gV0j6wS16OnpXMv9sTCHi99L69f5n+ZEy7wiHcRcauydK"
				+ "DzcyDPz7f7ldpcPcOUldTAEBlsAATjBkGc+OIqTDw9QimA+Sp+QAOUwfLqZBVc0mBPDMNC1LrQfbUW+nPgAS1yl4coGQUHbRQcIQ"
				+ "KgfD26a24w0ntnCk+rXs6Atf2QxgxWQOrFNeOIdV2Xwl6f39XqxlH2EdWi76GgzRPLFrtG2WvQKqLJYTslzwDJPBmCzKSTb6YYM7"
				+ "ZRGF+J07V+rAJtuSycJMHUb4oM5Jlyhar9adUmn0YFN1z5JuhZBp027EOdv04FLN0BWELiqKJsv8hyw9J8mfUDdf/V6vNhcniPAC"
				+ "oFnkg6SdO1fLwE23SbAZhcBpt6zlxjRJR+IvytPSKvrdBKyP4HWQRodSNr0cdkFlz+ALu0x7YLWDXqyqiibL/I+JBTkgGKJjiMnk"
				+ "yx7435Fsi4LkbpN3fQDsgRaHyiq6ei6jEsH1dQdB+qTOuN0oRpd0tpeK31RZ8OS94BlBg1J2zuPvjOYfJfQXkcJne/yhV23bTCLM"
				+ "Nh0UEt5lw4kjaWZl5Zqy0WkKq6dalzf4KZRJt0FVxBxHWSb3VWHua40oH6Uy4LY0Pa+0E1sJ6Zpk/KmLrh8gOmPtPhovyT0+sw6K"
				+ "0SaEItuWyXtzByNMiQEciBtB1fZpHMEy2oPftpy2s/VK7Xd1CWdRgeSdtl0ntY11e6TatDrgi7pOF0w9SQfTRqfOGxlqqknooaiO"
				+ "SPPAUufcDji5gloOxkV1k6i60iryzJOt6VtdqB1kFbXaWCzCbayYjN1Ic5HC7DppgBZAtNu04Gkzfwku0anTT/Jc+nA5VMFzmI6w"
				+ "6XnjjwHLEQciTpyEM0oFHNwrVlSJzJtOtA6kLQtX+f1l+AnOD9gwcOWtnybAFkCrQvaF5i6K08wfYSYYxSAn/Fge/CDZ/GVemzHR"
				+ "tA2bRf0esVH+8pS1gGgS1rKSx4QmyxBCh2qzgrQ9br03NGIQ0IB6XJbcGdQTM7jrp10L7Lp8INuppOw1QXidEkn6fjAA17aJ69mE"
				+ "buIoHW9M1y6Jo2/6FiadlueSz+YBdtjfu1G60DbBa1rdFmXDzD9bGnBzBNS6FB1Vi9cxy13NMKQMP0BTPfGUbNTSdq0C9oHaB3od"
				+ "ur2VqqDNLrGZodN7KLb/IC223QpqwXYdNMWh81PyotoTL8kdB2iawE2XdKCTpt5KUF3SSwa179yRaMMCTUxnaiXe1xPSdsBY9YXI"
				+ "O3sTxFseS4R9DZou0232VAeOkTrwFym2XfycyRdVnRBp808F2Z9WgRT12ng8vVUSSMMCeM6SnleET83KzsvbGXhICea6MClA5eus"
				+ "flg2Rc6wG8KRQf48S9eF4yf1+D3fPjyM96AIF9CFuCDd8nDB69XxnupkP5vFs1iFswt4TXE8mPqb7DIG0zBChZ88BRvCD2CBS/Uw"
				+ "+8asW68mUFelwOwv/H2CKwLnwLDD6fhj49mCHHHuhLS1KN9bP71aUtwhHqq0sdL6xqXPRc0wpBQEN3dkYIhYWw/03XUQ8dSBJi60"
				+ "Jc6lhC8Sx0fEMUbOC9mwUdH8cNivC8KH3AFeOcUXqGMYIUlPjKBV7PgFTX44bGAT3Xhh9SY4P8eC/zwpRq8KA+vr0GAwjrxVoYzW"
				+ "PDlZvz4Gm9owAdj8Y6rc1jkwxrwxTvir4x0vFoG68R3DPGaGgF5gkuvFCmLZZr60/inJ3i9TA/oO4LWNS57Lsj7FZYZKFLCRUqfq"
				+ "hegow6pJ60u2HTx7W8RJD2GBW/pxIce8EI/vOscX4zGC/EQwDCpDeZGS7zzCoEKQQ6BDcEL/rK/3saCl+u9mgVXZPDDq2dQDq85x"
				+ "hd6NHinFa7IsB6sGy8exBWgfFcQbYQNb5zAXUG8bA/vjMfLCPX7kcxtE7ReKVLWVodsb9x6bT4VoKvw5H0OS5a6s+ijb/QE1bfCE"
				+ "rqc6K7y0CWt7boOjWmPq1dIq+u0ia0chlUYEuKT8kDqQNDAK1nwziyAN5kCBKTZLNgG2Q6tI/jg24DyLnYBr5YBqE9vP16njNfNC"
				+ "Fgn1q198BgGhoNJH7XoS3R79PbGkcbHTdmda0/eh4Sy1AK0XqLsB8/OTqIzTF3SrsI2u6tdtehA0qaYwCZfWn4VCz5qgbkhCN7+i"
				+ "ccgMLwDGM59hQWvJMacFD45hs9wYeiGqzQB9aFvwRf1SZ0YGiKQYZ7M1hYTvb9QfyVf4Emj20gqh6VLF7ReG+nuXDcMeb/C0h1HH"
				+ "3l7LyjUbXe4Oq+tI6Mt/S2CpOVOG97KiTaKbGbB65TxymMAGybhMYRDcMPrlzEPheEh5qDk7aa4OsLbPXVduKOBCXa8xhkv5NPtc"
				+ "IFyAtqI9sVhbpuUN+1xuHxN3Va3Db0NnhrJc8AC0pmkg5mdTog6Va++pzub6Fgm6cDUtZ9eAp0P4nRJu3Rgpl2Iz6PR8gYWzGVpw"
				+ "ReS5cs5Au7S/YwF743HR07xLnbMbx3NAlAfAtYaFl0XPg4hnw+rFFzN4WrPpNdBU8TlVYvUiaWuP41eD+pdX6bIe8AScGLiQJsnt"
				+ "RB1Av3m4ACz40k9wNQFl49G/HU5V73mOpCO03UZQewArwEGmGeSMpg/wvwUvrCDO3ziC7TuQq58xBdzYehbuDqrVx/DfNhCFnzkV"
				+ "daD1zHjQxQC7K62J+lYunwGmLKmxLXLPO65I+8By+yAOKByULVuwdovbGVF1wJsuk2ALEGcrv3T6IK24yoK80h4NADPR+HKB5EaV"
				+ "z/4/h+eb/oTC+atMMzDNwBxJQUw/MP71lEH8jeywB/f9sPzVn9mARgq4v3uuMLC1Rbez45nuu5gwZWSfPChErAO3CXEPBragHbhW"
				+ "S184UfQ22li7g9BdLOs1qUzYGnTTbTd5aNxrTfStSmglyEizboyTZ4DFg4eDqyWeNJ1M5sXljYdpNH7E1xJnciCRxHwzNPtLAAPf"
				+ "uKrLP/GgkB1N8sGFsxf4RNbAN8pxAQ6nrnCHTsEDkzMI9gdxyKf1cJVXAcLHmtAUEFd+FAEPjGG56bkUhaf+UJa3yEU8PWdq0M1A"
				+ "D54HAKPUWBYiXkzDEPxzBjqeIDFJPmYp0Pq0f1I6yba7vIRkC99waVrTB9B67mlkT7zJciBlbwSqy+cQ03N0We+ilfQ+nXnh3oPt"
				+ "g5UK7qdIE4H4g/SlLHp/UV/rLOWdbjKDsS+stN5EffH4hxuzr20/mL/ma9omVfQ6aTzJXfA4BayHGeru9RTDwHmEsTp2j/OT3Dp/"
				+ "YW5Tn0S1euEqmW7XO1L2+56bYMnJXkfEgJ0PugQ3RF7d/R0D+lJXVqALIGZb5NGJH7/DzyuNrna3U/bMBh31cCQ54AlR9kMDpLuH"
				+ "TiS+4X4m57uOuPR5QSXbpKmTFz5etIf60+7Dts64/yFSuv0DAB5HxIKCDAQ3fHE5sDaR3UZ0WsVIEvg0k3iykjj48pXi94xtvW4d"
				+ "EHKYGmrC9h013aZ65C0rsOF9rG1VTDzpByWWhfS6J4qaISAhc6GjgLRHU9sCp1t7b9SxiVA60Dnm9JXWBtfJxJ3UgJSBktXXWn0J"
				+ "GqpL2k9ko+l1oU0uqcKGiFgITCk60BBCJE40hNPzE6WttOhgp5KIlBWl++1MiaNDsx0NbjqSNOGNLogtkrKgGp8JK11F6a/YJZz5"
				+ "Zl+QqX+npQ0yhWWLKXD2DtO4KnjSYD21XXZBNjsIoJpM/MElw7MdDW46khqA/ZJnB1L2W/m/pO0SwdJOpYuH41ul2D6ig/Q9Wod2"
				+ "HTd7jh/YKYrwKy6cWmUOSxBOieWuqNaSMhubPTOsZ1Jev9qHVSiY5lkj9OFtLpNgEsHST42WxwWn7RF80+jDAnlpNInV8KfrCDb9"
				+ "EG63gJkCUy7zUfjsmu0j0vXpPER9Jkkepp606D99XoqrScOW12mTafrpbswfJBMU6wxaKQhIajgz1TgavojXasArQNTt/lpH43Lr"
				+ "tE+Ll0jdpwpWreh7aKjDHQtgk3XPloHNrvUD0xfl92GabeVcZVNQx3qcx0iJ7W0d9CT94AlB093cMHsCYXyF/j1oP3gUIkIpk30i"
				+ "ntjP6PbJ7q5HTbEbts+XY9gW48gaSxtfqbNZbeh/YEuq3WNzR8k6ViKVEhFRaqoPzvkPWDh4OmTR58k5snGadMUoI1JnUHny/ogr"
				+ "nJSt16HS6+UNHVWUz+2RbZH68BmN3VB24FN1zZNNdtQ6bZWs28EKYv211KPx6BRhoS6AwmWzqSzrf0MRjiJo+g2G7DppgBZApdeK"
				+ "WnqrKV+G3qn2XQsq9XT4PKFXbbVrNNWxvTXSBpLrQvQdVlXPTbMejwGjRCwNOgE0hESOkRhAZ38bnx2SqNPcK0D39nc+0d0LKvVN"
				+ "drm0jWmT1KZNP7abssHLt1Fuf/qC/H6HLyjzBOR54Clg4Z0BCxtekRZ8lQa0nYPdax9Ex17juwn1OkSYNOTBMgS9LWeRFIdZl1J/"
				+ "nHE1RUH/MQ3rkxc/UnlJN8so+1mnmDqOp3M6gvGUOfadVRowgsPJ1ZaPM/kOWDp6GM74r1tW5/6G5svYu3F0EBTqVC4hsZN/wN1r"
				+ "MFHRCXI1UuEOF3aaepCpXoSSXWYdSX5o83SblMXREcZmx1oHZjtAKa/bgOwtc+FKz9pvUCvFzrEbFtvlr6lmQPV26ip5a9c5ELue"
				+ "9Hn1Qr3ULFovputIWmUIaF0GC1A60Q3f2sfrV93CXXvxwc8r+Ys+ZrMMdx5rqfOi77NgQsfAgWlctXrts5von3S+A820GZpd5xuQ"
				+ "9vrpachbZ2S1rrG9BfKfdtGEQeqFTRy4h85C1/BnhRm4LNmxfNo1wtH0rXrfhnZGpq8v3EUIDho3UZ5BxI61hzJgeqznL0isgC8J"
				+ "ngdB7V1tOEyvGM8DtSrg5O5flcb661XSz3qyCr9s+2rL5xFTU34MtGZ4eqw2uAzaV+m/fs+Qr++HF8oqpycvnG0mWbj9d45ZBO+j"
				+ "1DW4dLo5Wza+BQ1t36dxky9k72OZVd8IBQfUFhJhaa/o9lLn6ZRB95FT+E7DEE9pgCbrm0grS4nUVr/NOgT0zxJ09aRhFkviFtvE"
				+ "mnKunzSrCtt/cCV51pHmHfyu0fS3BUfpqbCNzl5eOgeZG3g4d+r6dpL/pMe+gP+OFZHTs/rRpjDMjuO9AxB672577e4WvsR7duNT"
				+ "6XjAw344gzgoWHhWzRp7vX814yDWS9c9cavL76cuS3V1qXRdZr114tK6tVtdulpcG1Xmrak8Yc9Ls/OUWc2BzdyhrTdS4Xi+9i1L"
				+ "crBRzReSbf97FQOVneGJo9JI8xhoaObHUjS7o5l8pvP76b1F3+Sq5vP8nW2yJdflvKw8UbqvOgaWr0Gn8HC+uTkEl0LsKVtJNnRf"
				+ "u1j+qffPjuu9VeLrk+3rV56Pan3tmOeailNbL+R+8s1XD36CniRr6guov17DqX16/6bNv81ab31b1eGyPMcFv6XzoyDbOpy4Kvr8"
				+ "LiqCua3OGCV2MbVfpr27rycfnvlrshWT/R29AW2/dSXVLoOm39ftDNNnenXu/rC6dTUhM+qncNFojK4ocOBq7vrg7Th0s2hrY74O"
				+ "ayMsWmj2ZkkrZemj0bnaT3sCJs2PkETZ11DQ0fex30Q3+QbwzKUXVdRc+vrqf3Ex7nUPfR88BV43bldehzaL6meNLoLnZ/kWyvVt"
				+ "B8gre1A6xpXXUmk8Uv2OeXdw2nu8vdz/8C3GY/mIlwmaMZ1vDiT/6heTZtuTLpxUynhdvo5rFyjO5/oZmcXYAvtN32rmzvdd6h7P"
				+ "z6h/lEWmSSdxR4/4E7zGx4GHMlp+KMOXY9Zv+TZdCkv1KpLOq3eF8i2g7S6pF0+Jmn96suhZ2Dkcja1tOHjr/iU/ohw9UV8Hfsse"
				+ "vjmk7nfyFey603/becA0GhDQlkCORErOcC6HlCuB5f+zZ9m/ewgHYJnuTBn8QFavw6X/rqMIL5Sv0bb+koHSMfp4t/fDOS6KyeYK"
				+ "gi+Th1d4gS7bzsvP037915Gv75cvo7dt+R0SNgIz2GZyAmgT8i0uDpBeX0dazERj/ktffcQT89/nIrdn6drL9nLetr1S3tBJTqWw"
				+ "OZTb+LaApLyatFNkCd2l55EJb4hHWumcJFPcqm/4yWPXIImdrP+Leruej9tuPSJwK9yXG1xbVuo+zmsjFE+h6UPqLZrPQ2uOnTnK"
				+ "PC6H6GDDvsqtQzFEOB4FvyIGrevOzmQvY7alz7Cy/ui+S1dvp66UE1ZU8cyDp1v6mnyatFNtN2lJ5Hed+W/tNG8lWv5eH6X5TguG"
				+ "pUt3Mh77iy66ZtX0j3XbgttVeFqS9y2+TmsHCAnYa1I58DS1Evp332xSOsv/joPA/AYBO4QRUOB4lzu0z/lDvVL6lx7KAwsUtYMD"
				+ "oNN9wjzVmLY9RpqbbuLU59g4T9KOHxF/itUfCM9cecyvsq/mbY+Be/+JNfHq9GGhBK0kKf1SjDL6SBj00P/jjXtPFK4mJOvZrP44"
				+ "CcYX+Ihw0d4yLAlNPUiqX5Qvi673VUOuPKSymodpPGzlYnTk8oCnZdEXJ3p6gh/soV5Ko5aICi2k4rFS6jYdTFtuKyaJ9Rd66/O7"
				+ "uewMkZ5wJKDB5up2w56HHFldF5vffRkoiV/x5086Oy4eyg8x/JR2rPjKrruyn2hyYm0G9h09/pD4vyB2IHp09isumASNbd8jHfJu"
				+ "bxLmqNdw/8Vv8fB6l/p2kseCR0HAX4OK2P0fg4LaJvolZyM+uS16RC3vmd7gR7c+DBNW3w1tbQ8zmbMb41gGcZyOrW0nkmzlz7I9"
				+ "gfp+UelHKhET9MWkGQHWm9cVr6zlYeA51FT0w84hQeFo6mUwi28+86m9esu5/4mryQaaMLj6eewMkvSCWkCfxtix9LUUU96/bovd"
				+ "HEn/zL/Vcb81qVsw11DsJDd/pc728+p40K84gboNg6UPtjAfux7ph6KK5WXUeuI2zm1jncJHg5mCk9yC86lFx5fwsfxhtDmpH/aW"
				+ "mIwH7eaabQhoYA8sVV6gHU56FK+ko5Zvv6OtZiIv4T1l4WmAASxK6m762O04dIXQpMVsw311jWwAzNP/HW+q444zPImtnUAc31xO"
				+ "kj271i7iP/H0L0zsIbgxsnl/IfmUzz8w52/uPqAtps2YCtXHz2n53WeAxb+tx3MwUbYtnB+q4NVPkmKeDNEYGb9Gf7vw7Tzhavp9"
				+ "1djkt7eQftWbxxWnT+Bmls+wto/srQEtnBf/CT4kfK1l2wKTX1O3P5PPmZ+DitjlM9huQ58Ndg6AuoXu9aTgG/Ytj3biR7cuImmL"
				+ "76amoc8zWbMbw3nJX7WcQYNGfZKPlb30+a/PkR7e25CyXbJEpi6dOI4H01cXn8jbTdx2dOgy5b0Fe9sofkr30lNzT9Eis0yT3Ub/"
				+ "/cGHvpdzH3q+dBWEdW2Na6MzrPrfg4r06DTCFqvFnQM6Ry6wwiSr31M0ZTa9Nsv4DXNV1JXF+awPs8idw0PZ9lAS8/9MXWunc261"
				+ "OHaHrHDL802w0fXabYRwJ6mLhe6vK7HVa+tDRqzPlsdGuTrOsN907n2VBo64i+sX8EyHhlsxh+Nf6JtTx/Dx+PXMIT2AJcOzDy9T"
				+ "wXokjbtgtY1acq5ymaeRpnD0uBg6k7k8usv4tvQsWYR/7HHxPypynUPD0+uoGLXJ2jDZbU8Rd1fuPZ5pXp96VgzjwoF7NszwlVgV"
				+ "cHc4RfCucPLqrnzl6a9Nh/XNouOJUin+yFhxigNCfWBg03sQOtpsXWQWtDt7N2eTRv5Lz19m8bPuIWzj2Z9IksLn2gnciB7C7Uvf"
				+ "Z4mtt9Gj9+OFwra6nLpQiW+1aLrqUWvD6suGEtzl32C9981XD3uzEYZhZ/zH4JX0bWXfIc23bgnMlZKmvbCx9y/STqW6XU/JMwsc"
				+ "hDloKKjyFL0tEgnk7rS6oLWBd0O0bUU6MEb8DOf/6H9ezAsvJBF7hpO5sD1FRo79SbqvOgkTksZkKTbbCBOF2y69h2cLPl7fEbrH"
				+ "6kZn9Gi87m5rVGT7+blabTxay/nYHUfp2vZDl3WpQNbX/Ak0ChzWBodSCrtNOIf1/lcugv4xAkIl7++Yh8Hrs9S9z48v4XPQXVFT"
				+ "cGV13U8DPgeD3MOZt2so1YBsgQ2XfsOLoaPw+uJT6bRk//EqatYDoiai18YvJu2PbOY1q9bT9t7PlBTy3bosi69WtIGw9zSKHNYO"
				+ "KCS1no1uOpKq2vEnoS9vs6LcMWFZ4VOCdIheDXzpdS17zP0q8/ibZa6bOOxes1saipczLvgVZEF4PGQL1L3/o/QhssQtAaCvj0uf"
				+ "g4rY7gfa6i1k9jqsnU+pKXTmLpgs0tdpt0sRzxUfIpGT/kmjRh7O5uOYQvucOEzZMupqfnvafbSZ2jqoXfSI38y66wUve5scPK7R"
				+ "9G8FR/lITM+GHKY2oT14TzVum/Qphv74r37aenbfernsDyMnPBYiphoH8GlA3Rc3XlF13ZbPijQn3/Ip+C6n9CebXxS4rNR+BBGw"
				+ "DTO/gYNG3sDDxOXRDaps1IZjNj2PdGSNzXx8O8tNKQN81TvZbe2aBPu40D1crrzF6dxsLobBk/2aISAZQYLSWu9UsyTWZZmfaaP6"
				+ "JWi67Xr1121iwPXZ6jYjee3rmGRz5AtoULTRh4ifIPloMiWB3rvy861y2j0pJs466ucmhwaC7hBsYZ2bz2cA9XP6Qn/yb8s08jPY"
				+ "cnJXkkQkbJABw7BrNfmY+Jav14XMNftWleod6w9hodDeE3zssAago/Afoa6uy6jDZficfm4OoCZHpx0rJ3BLcTn3s/ipkpbcUPiq"
				+ "9S9/0O04bN4NKSx8HNYGcP+ehkgdiwrPRHhbzuJ66VrTLutDJZ2fdPGJ6l15H/S6APv5XMY75Yfy9LKcgpfcb2R2pc+yVcdd9O24"
				+ "Fy21QHM9ODilHePoLnLP8At/DY3czELtxWHp/AbvtJ8DV17yVdp0x/q/RmtbODnsDKN/msjOpbaXgu2+kGS3rfB4J5fFvlK83u0b"
				+ "9ciTn2Y1xqevEWayef2d+nQl/yW/xIfFdiyxKJTMU/1hvAzWoUPsgyPcjbxtp1JD96wioMVfgM42HH1D4+DRhwSmqCjVBM40pTTg"
				+ "cmlV4OsW7chWe9YO40DFd4v//oeWzjX9Z98RfIBPsnlBeS6/OCiY83xfIWIoa7cSGCKuNHwKdq3+3L6zef75zNag52cDgl9wOoNy"
				+ "rkOtpzIki/r0Ce4zcelDwwda5dw4MJJjzdCCFtZPkl7dlxB111Z7c9S+o6ONQdxmz/Ju+0NnJKRAQfb4jd4+X5av66Srz3o45VP/"
				+ "BxWxiifw6pXB5U6sBRd6taByKYDKSPL/iZs66aNj9GkOV+joSPxbid8Zn80y1CW1dTSeg7NXvoou90bfYZsYFn5rmE0b+V7OVh9h"
				+ "9uEuTjZdzdQsfha/sN0FT24ETcTKmGg9r+J9B3g0qvDz2Flmnp1UHQkU+LqRr5G0ro80GmxAZuufSrVQZi+8evdtP7ib1DX3gWc/"
				+ "gSb5CFKvLrmx9zhr6XOtYexrsv2H7NPLPD6X0utw/AZrY+xjAzsRI9woHo9PXrrcg5Wt0S2rKL7jkv3KBr5sQag9bTICYxyLh241"
				+ "iN+gtgHlo4LD6ZCMx4NOJNF2oSfsHyFuvZ/iH51Wc8P7fqcjjWLw3mq4vKwKcHu28HLdbR31yX02y+Yn9HS+zcruNps9pXqtsvPY"
				+ "WWM6uew0lBNZ0B7pJzofdnGJPT6w3aNnlyg4//upGh+S989xJs2P0a7tl5J138x6TNk1dOx5kBe98e5WW/mJjVHzUPbvkPdXe+jD"
				+ "ZcOgjFqXTGPgU2vDj+HlTHcz2HVA6kbS5sAWQLo0oFcuqZSf02l/iXwmuZNG/9G04/8KjW34ht7mJTHUAyfITuVhgw9i2YvfYief"
				+ "+x+2lXHr1ot/6ehNP/kCzhYfZ+bfAJbmqKm38ytP4uD5Ofovt/ipkDewEYKLr06/BxWbpATGMv0J3OIlDHrMEUCBZbS+UQXuyB2L"
				+ "SBJdwmw6enlt1difutrtH8PXmODr3lEdw3xWTL6OR1z9i/4amhhaKuBA+fjCv8V1Db6Dl4thqOY/AePU7H4Znrq3qV8pfwH2p2Fl"
				+ "6p6+gM/JOwNyiGoAK3biMuzIfVV27Z6YttGUw/pWDOHCk0IXK8IDaDIQ8PCVRzU/p1+fUXlr2hZfeFh1NSMV+OsCg0gmPj/LO3b+"
				+ "2n6zRWV3vnLOvXtF34OK2PYA5buFNV0ECkjnSGuvPZJ0gc7BRo2huiktyO4cJAp4O4hEzR/C8uHeYj4Zbr+y5ikj2f1monUVPgoa"
				+ "2/jetRntIo/4qsqfEbr4cgGsALZx1rPCgPXfj+HlTF6z2FJh5GlrfO4OpUuIx0hThdEx9LUdZ2DXYivpPAZsodowqyvUNuop9h8P"
				+ "As+QwZ5CQ1pexXNXno/PXb7JuqSD1krTnhLKy3qeBdfqf2Q/U9ikemIP1Ox+xwOVJfwMTM/GBuuO0TraailbL0YuDb4OazcENdxX"
				+ "HmwS54sJUABU5e06GY6jqT8emG2xaWXc/O399P6dVdRV/CaZnwWS+4aHsq7Zj2teMdPeQg5N7LJ64lPp1EH/IXzcfeRDQGbufzba"
				+ "efzx3Kw+l1kqyfptqdv6O/1NQx+Dqs60CGT6o/rtCibpU7t3taOtQso+FRW8fSSWxGXWJ/jId4P+ULqw+V5mMAvfp669n+CfvXZO"
				+ "t5q9JTh57AyRt8+OIoy0iFsepo6q1l3teg29g0dfBVVIA5cBTw576D439TdvYY2XPpAZOgv+n77Bxs5DViNOCSsFQkystQdQ3Qsb"
				+ "aLRwcrMqze2+k1bpW3Q/niNzf/S9meOYPU8ThufdC/eyR6d9PuvvDImWKE+VzsrbZtJXHmd5/Krdv1m3dXW44lo9DksrVcDyqcVG"
				+ "7YObHZyQXQsRYRqdSxFgE5rsaH7Trh9G/8Tn9n/XDS/hc9pbeZh4bto+7NH0bUXb6CdRhwrx7Wf4vZfPdB1u9ZT7frNuv0FQo3kd"
				+ "0jo8Xhyh4/4Ho8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gej"
				+ "ycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD"
				+ "1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxe"
				+ "DKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4g"
				+ "OXxeDKDD1gejycz+IDl8Xgygw9YHo8nM/iA5fF4MoMPWB6PJzP4gOXxeDKDD1gejycjEP1/MlTv1nhcmnEAAAAASUVORK5CYII=";
	}
}
