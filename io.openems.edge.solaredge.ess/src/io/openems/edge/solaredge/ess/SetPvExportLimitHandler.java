package io.openems.edge.solaredge.ess;


import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;

public class SetPvExportLimitHandler implements ThrowingConsumer<Optional<Integer>, OpenemsNamedException> {

	private static final float COMPARE_THRESHOLD = 0.0001F;

	private final SolarEdgeEss parent;

	public SetPvExportLimitHandler(SolarEdgeEss parent) {
		this.parent = parent;
	}

	/**
	 * Handles a PV-Inverter Export power limitation request.
	 * 
	 * @param activePowerLimitOpt an Optional power limit; empty value sets the
	 *                            allowed inverter power to 100 %, i.e. no limit
	 * @throws OpenemsNamedException on error
	 */
	@Override
	public void accept(Optional<Integer> activeExportPowerLimitOpt) throws OpenemsNamedException {
		FloatWriteChannel wMaxLimPwrChannel;
		IntegerReadChannel exportControlModeChannel;
		IntegerReadChannel advancedPwrControlEnChannel;		

		// Get Export Power Limitation Channel
		wMaxLimPwrChannel = this.parent.channel(SolarEdgeEss.ChannelId.EXPORT_CONTROL_SITE_LIMIT);

		// Get Export Control Mode Channel
		exportControlModeChannel = this.parent.channel(SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE);
		
		// Get Get AdvancedPwrControlEn Channel
		advancedPwrControlEnChannel = this.parent.channel(SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN);

		if(advancedPwrControlEnChannel.value().get() == null || exportControlModeChannel.value().get() == null) {
			// We don't know the channel values. Not ready yet (on startup)
			return;
		}
		
		int advancedPwrControlEn = advancedPwrControlEnChannel.value().get();		
		int exportControlMode = exportControlModeChannel.value().get();
		int exportControlModeDirectExportLimitation = 1&exportControlMode;
		int exportControlModeInDirectExportLimitation = 2&exportControlMode;
		int productionLimitation = 4&exportControlMode;
				
		if(advancedPwrControlEn == 0 || (exportControlModeDirectExportLimitation == 0 && exportControlModeInDirectExportLimitation == 0 && productionLimitation == 0)) {
			// Inverter configuration not as required ,...
			if (activeExportPowerLimitOpt.isPresent()) {
				// and power should be limited -> throw error
				if(advancedPwrControlEn == 0)
					throw new OpenemsException("PV Export Limit Control requested, but inverter configuration is wrong (AdvancedPwrControl not enabled).");
				else
					throw new OpenemsException("PV Export Limit Control requested, but inverter configuration is wrong (Export Limitation not enabled).");
			}
			// and no power limitation is required -> ignore error and exit
			return;
		}		
		
		if (activeExportPowerLimitOpt.isPresent()) {
			/*
			 * A ActiveExportPowerLimit is set
			 */
			float activeExportPowerLimit = activeExportPowerLimitOpt.get();

			// keep export power limit in range [>=0].
			if (activeExportPowerLimit < 0) {  
				activeExportPowerLimit = 0;
			}	
			
			if (
			// Value is not available
			!wMaxLimPwrChannel.value().isDefined()
					// Value changed
					|| Math.abs(activeExportPowerLimit - wMaxLimPwrChannel.value().get()) > COMPARE_THRESHOLD) {
	
				// Set Export Power Limitation
				wMaxLimPwrChannel.setNextWriteValue(activeExportPowerLimit);							
			}
		}
	}
}