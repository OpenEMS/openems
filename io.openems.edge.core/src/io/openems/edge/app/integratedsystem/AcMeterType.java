package io.openems.edge.app.integratedsystem;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Function;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.meter.KdkMeter;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

public enum AcMeterType implements TranslatableEnum {
	SOCOMEC("App.Meter.Socomec.Name", Parity.NONE, AcMeterType::socomecMeter), //
	KDK("App.Meter.Kdk.Name", Parity.EVEN, AcMeterType::kdkMeter), //
	;

	private final String displayName;
	private final Parity parity;
	private final Function<String, DependencyDeclaration> dependencyFunction;

	private AcMeterType(String displayName, Parity parity, Function<String, DependencyDeclaration> dependencyFunction) {
		this.displayName = Objects.requireNonNull(displayName);
		this.parity = Objects.requireNonNull(parity);
		this.dependencyFunction = Objects.requireNonNull(dependencyFunction);
	}

	public Parity getParity() {
		return this.parity;
	}

	/**
	 * Gets the display name of this {@link AcMeterType}.
	 * 
	 * @param resourceBundle the {@link ResourceBundle} to get the name from
	 * @return the name to display
	 */
	public String getDisplayName(ResourceBundle resourceBundle) {
		return TranslationUtil.getTranslation(resourceBundle, this.displayName);
	}

	/**
	 * Gets the {@link DependencyDeclaration}.
	 * 
	 * @param modbusIdExternal the id of the modbus component
	 * @return the created {@link DependencyDeclaration}
	 */
	public final DependencyDeclaration getDependency(String modbusIdExternal) {
		return this.dependencyFunction.apply(modbusIdExternal);
	}

	private static DependencyDeclaration meter(DependencyDeclaration.AppDependencyConfig config) {
		return new DependencyDeclaration("AC_METER", //
				DependencyDeclaration.CreatePolicy.ALWAYS, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				config);
	}

	private static DependencyDeclaration socomecMeter(String modbusIdExternal) {
		return meter(DependencyDeclaration.AppDependencyConfig.create() //
				.setAppId("App.Meter.Socomec") //
				.setInitialProperties(JsonUtils.buildJsonObject() //
						.addProperty(SocomecMeter.Property.TYPE.name(), "PRODUCTION") //
						.addProperty(SocomecMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
						.addProperty(SocomecMeter.Property.MODBUS_UNIT_ID.name(), 6) //
						.build())
				.setProperties(JsonUtils.buildJsonObject() //
						.addProperty(SocomecMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
						.build())
				.build());
	}

	private static DependencyDeclaration kdkMeter(String modbusIdExternal) {
		return meter(DependencyDeclaration.AppDependencyConfig.create() //
				.setAppId("App.Meter.Kdk") //
				.setInitialProperties(JsonUtils.buildJsonObject() //
						.addProperty(KdkMeter.Property.TYPE.name(), "PRODUCTION") //
						.addProperty(KdkMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
						.addProperty(KdkMeter.Property.MODBUS_UNIT_ID.name(), 6) //
						.build())
				.setProperties(JsonUtils.buildJsonObject() //
						.addProperty(KdkMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
						.build())
				.build());
	}

	@Override
	public String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.displayName);
	}

}