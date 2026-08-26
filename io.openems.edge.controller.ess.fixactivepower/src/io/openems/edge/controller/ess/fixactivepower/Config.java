package io.openems.edge.controller.ess.fixactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.controller.ess.fixactivepower.enums.HybridEssMode;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.ess.power.api.Relationship;

@ObjectClassDefinition(//
		name = "Controller Ess Fix Active Power", //
		description = "Defines a fixed charge/discharge power to a symmetric energy storage system.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlFixActivePower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.MANUAL_ON;

	@AttributeDefinition(name = "Hybrid-ESS Mode", description = "For Hybrid-ESS (ESS with attached DC-side PV system): apply target power to AC or DC side of inverter?")
	HybridEssMode hybridEssMode() default HybridEssMode.TARGET_DC;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Charge/Discharge power [W]", description = "Negative values for Charge; positive for Discharge")
	int power();

	@AttributeDefinition(name = "Power Relationship", description = "Target power must be equal, less-than or greater-than the configured power value")
	Relationship relationship() default Relationship.EQUALS;

	@AttributeDefinition(name = "Phase", description = "Apply target power to L1, L2, L3 or sum of all phases")
	SingleOrAllPhase phase() default SingleOrAllPhase.ALL;

	@AttributeDefinition(name = "ChargeOnce - Power [W]", description = "Charge power used to charge the battery in the ChargeOnce event")
	int chargeOncePower() default 2000;

	@AttributeDefinition(name = "ChargeOnce - Target SoC enabled", description = "If enabled, the battery will be charged until the target SoC is reached. If disabled, the battery will be charged with the chargeOncePower until it is full or a fallback time is reached.")
	boolean chargeOnceTargetSocEnable() default false;

	@AttributeDefinition(name = "ChargeOnce - Target SoC", description = "Target State of Charge in ChargeOnce event")
	int chargeOnceTargetSoc() default 90;

	@AttributeDefinition(name = "DischargeOnce - Power [W]", description = "Discharge power used to discharge the battery in the DischargeOnce event")
	int dischargeOncePower() default 2000;

	@AttributeDefinition(name = "DischargeOnce - Target SoC enabled", description = "If enabled, the battery will be discharged until the target SoC is reached. If disabled, the battery will be discharged with the dischargeOncePower until it is empty or a fallback time is reached.")
	boolean dischargeOnceTargetSocEnable() default false;

	@AttributeDefinition(name = "DischargeOnce - Target SoC", description = "Target State of Charge in DischargeOnce event")
	int dischargeOnceTargetSoc() default 30;

	@AttributeDefinition(name = "Ignore system permissions set within the SystemLimits of META once - For Service UseCases only", description = "Ignore system permissions once, especially isChargingFromGridAllowed/isDischargeFromGridAllowed. Will be reset after reaching the target. For Service UseCases only.")
	boolean ignoreSystemLimitsPermissionsOnce() default false;

	/*
	 * Systems with existing FixActivePower controllers in complex szenarios could
	 * have wrong system limits due to missing IBN e.g. Multi-MWh Projects with a
	 * default GridLimit of 32A. New Ctrls installed by AppCenter will consider the
	 * system limits.
	 */
	@AttributeDefinition(name = "Consider System Limits", description = "Flag if System limits (e.g. GridLimit) will be considered. Forced by AppCenter for defined Systems and new installations.")
	boolean considerSystemLimits() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Fix Active Power [{id}]";
}
