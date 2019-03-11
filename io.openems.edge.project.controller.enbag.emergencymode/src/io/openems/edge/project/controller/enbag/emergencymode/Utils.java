package io.openems.edge.project.controller.enbag.emergencymode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Simple cluster wrapper
 */
class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EmergencyClusterMode c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(Controller.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case RUN_FAILED:
						return new StateChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(EmergencyClusterMode.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_INVERTER:
						return new EnumReadChannel(c, channelId, PvState.UNDEFINED);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}

	private final Logger log = LoggerFactory.getLogger(Utils.class);

	/**
	 * @param int active
	 * @param int reactive
	 */
	public void applyPower(int active, int reactive) throws InvalidValueException {
		try {
			for (ManagedSymmetricEss ess : esss) {

				ess.addPowerConstraintAndValidate("Balancing P", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, active);
				ess.addPowerConstraintAndValidate("Balancing Q", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS,
						reactive);
			}
		} catch (PowerException e) {
			this.log.error(e.getMessage());
			throw new InvalidValueException(e.getMessage());
		}

	}

	/**
	 * @return int activePower
	 */
	public int getActivePower() {
		int activePower = 0;
		try {
			for (ManagedSymmetricEss ess : esss) {
				activePower += ess.getActivePower().value().getOrError();
			}
		} catch (InvalidValueException e) {
			this.log.error(e.getMessage());
		}
		return activePower;
	}

	/**
	 * @return int maxChargePower
	 */
	public int getAllowedCharge() {
		int maxChargePower = 0;
		for (ManagedSymmetricEss ess : esss) {
			maxChargePower += ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		}
		return maxChargePower;
	}

	/**
	 * @return int maxDischargePower
	 */
	public int getAllowedDischarge() {
		int maxDischargePower = 0;
		for (ManagedSymmetricEss ess : esss) {
			maxDischargePower += ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		}
		return maxDischargePower;
	}

	/**
	 * Check if both ESS are on grid
	 * 
	 * @return boolean
	 */
	public boolean isBothEssOnGrid() {
		for (ManagedSymmetricEss ess : esss) {
			Optional<Enum<?>> gridMode = ess.getGridMode().value().asEnumOptional();
			if (gridMode.orElse(GridMode.OFF_GRID) == GridMode.OFF_GRID) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if both ESS are off grid
	 * 
	 * @return boolean
	 */
	public boolean isBothEssOffGrid() {
		for (ManagedSymmetricEss ess : esss) {
			Optional<Enum<?>> gridMode = ess.getGridMode().value().asEnumOptional();
			if (gridMode.orElse(GridMode.OFF_GRID) == GridMode.OFF_GRID) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return int totalSoc
	 */
	public int getSoc() {
		int totalSoc = 0;
		try {
			for (ManagedSymmetricEss ess : esss) {
				totalSoc += ess.getSoc().value().getOrError();
			}
		} catch (InvalidValueException e) {
			this.log.error(e.getMessage());
		}
		return totalSoc / esss.size();
	}
}
