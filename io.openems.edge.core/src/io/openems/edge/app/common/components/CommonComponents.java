package io.openems.edge.app.common.components;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.core.appmanager.AbstractOpenemsApp.getTranslationBundle;

import java.util.UUID;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.types.MeterType;
import io.openems.edge.app.meter.EastronMeter;
import io.openems.edge.app.openemshardware.TechbaseCm4sGen3;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;

public class CommonComponents {

	private CommonComponents() {
	}

	/**
	 * Creates a {@link DependencyDeclaration} for an internal {@link EastronMeter}.
	 * 
	 * @param l       the language
	 * @param meterId the meter id of the internal meter
	 * @return the {@link DependencyDeclaration}
	 */
	public static final DependencyDeclaration internMeter(//
			final Language l, //
			final String meterId //
	) {
		final var meterProperties = buildJsonObject() //
				.addProperty(EastronMeter.Property.MODBUS_ID.name(), "modbus0")
				.addProperty(EastronMeter.Property.MODBUS_UNIT_ID.name(), 3)
				.addProperty(EastronMeter.Property.TYPE.name(), MeterType.CONSUMPTION_METERED.toString()).build();

		return new DependencyDeclaration("INTERN_METER", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.ALWAYS, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Meter.Eastron") //
						.setAlias(TranslationUtil.getTranslation(getTranslationBundle(l), "internalMeterAlias")) //
						.setInitialProperties(buildJsonObject(meterProperties.deepCopy()) //
								.addProperty(EastronMeter.Property.METER_ID.name(), meterId).build())
						.setProperties(meterProperties) //
						.build());
	}

	/**
	 * Creates a {@link DependencyDeclaration} for an internal meter with the
	 * {@link TechbaseCm4sGen3} hardware installed.
	 * 
	 * @param componentManager the component manager
	 * @param meterId          the internal meter id of the masterbox
	 * @return the {@link DependencyDeclaration}
	 */
	public static final DependencyDeclaration internMeterWithGen3(//
			ComponentManager componentManager, //
			String meterId //
	) throws OpenemsError.OpenemsNamedException {
		final var masterboxAppId = DependencyUtil.getInstanceIdOfAppWhichHasComponent(componentManager, meterId);

		if (masterboxAppId == null) {
			throw new OpenemsException("Could not find masterbox app for internal meter with id: " + meterId);
		}

		return new DependencyDeclaration("INTERN_METER", //
				DependencyDeclaration.CreatePolicy.NEVER, //
				DependencyDeclaration.UpdatePolicy.NEVER, //
				DependencyDeclaration.DeletePolicy.NEVER, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_NONE, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setSpecificInstanceId(masterboxAppId) //
						.build());
	}

	/**
	 * Creates a {@link DependencyDeclaration} for an external meter with the given
	 * app id.
	 * 
	 * @param appIdOfMeter the app id of the external meter
	 * @return the {@link DependencyDeclaration}
	 */
	public static final DependencyDeclaration externMeter(//
			final UUID appIdOfMeter //
	) {
		return new DependencyDeclaration("EXTERN_METER", //
				DependencyDeclaration.CreatePolicy.NEVER, //
				DependencyDeclaration.UpdatePolicy.NEVER, //
				DependencyDeclaration.DeletePolicy.NEVER, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setSpecificInstanceId(appIdOfMeter) //
						.build());
	}
}
