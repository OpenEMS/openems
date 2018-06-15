package io.openems.edge.ess.streetscooter;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.streetscooter.AbstractEssStreetscooter.ChannelId;

public class PowerHandler implements BiConsumer<Integer, Integer> {

	private OpenemsComponent component;
	private Logger log;

	public PowerHandler(OpenemsComponent component, Logger log) {
		this.component = component;
		this.log = log;
	}

	@Override
	public void accept(Integer activePower, Integer reactivePower) {
		if ( !isRunning() ) { 
			setRunning();					
		}
		if ( isRunning() && !isEnabled() ) { 
			setEnabled();
		}
		if ( isRunning() && isEnabled() && isInverterInNormalMode()) { 
			writeActivePower(activePower);
		}
	}
	
	private void writeActivePower(Integer activePower) {
		try {			
			IntegerWriteChannel setActivePowerChannel = component.channel(ChannelId.INVERTER_SET_ACTIVE_POWER);
			setActivePowerChannel.setNextWriteValue(activePower);
		} catch (OpenemsException e) {
			log.error("Unable to set ActivePower: " + e.getMessage());
		}
	}

	private boolean isInverterInNormalMode() {
		IntegerReadChannel inverterModeChannel = component.channel(ChannelId.INVERTER_MODE);
		return inverterModeChannel.value().get().equals(ChannelId.INVERTER_MODE_NORMAL);
	}

	private void setEnabled() {
		try {
			BooleanWriteChannel channel = component.channel(ChannelId.ICU_ENABLED);
			channel.setNextWriteValue(true);			
		} catch (Exception e) {
			log.error("Unable to set icu enabled: " + e.getMessage());
		}
	}

	private void setRunning() {
		try {			
			BooleanWriteChannel channel = component.channel(ChannelId.ICU_RUN);
			channel.setNextWriteValue(true);
		} catch (Exception e) {
			log.error("Unable to set icu run: " + e.getMessage());
		}
	}

	private boolean isEnabled() {
		BooleanReadChannel icuEnabled = component.channel(ChannelId.ICU_ENABLED);
		boolean value = icuEnabled.value().orElse(false);
		return value;
	}

	private boolean isRunning() {
		BooleanReadChannel icuRunChannel = component.channel(ChannelId.ICU_RUN);
		boolean value = icuRunChannel.value().orElse(false);
		return value;
	}
}
