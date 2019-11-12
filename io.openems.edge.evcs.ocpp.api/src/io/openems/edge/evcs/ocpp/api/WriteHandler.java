package io.openems.edge.evcs.ocpp.api;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

/**
 * Handles writes. Called in every cycle
 */
public class WriteHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(WriteHandler.class);

	private final AbstractOcppEvcsComponent parent;

	/**
	 * Minimum pause between two consecutive writes
	 */
	private final static int WRITE_INTERVAL_SECONDS = 5;

	public WriteHandler(AbstractOcppEvcsComponent parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		Channel<Boolean> communicationChannel = this.parent
				.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
		if (communicationChannel.value().orElse(true)) {
			return;
		}
		
		for (OcppProfileType ocppProfileType : this.parent.profileTypes) {
			switch (ocppProfileType) {
			case CORE:
				if(!(this.parent instanceof ManagedEvcs)) {
					break;
				}
				this.setEnergyLimit();
				this.setPower();
				break;
			case FIRMWARE_MANAGEMENT:
				break;
			case LOCAL_AUTH_LIST_MANAGEMENT:
				break;
			case REMOTE_TRIGGER:
				break;
			case RESERVATION:
				break;
			case SMART_CHARGING:
				break;
			}
		}
		
	}

	private int lastTarget = 0;
	private LocalDateTime nextCurrentWrite = LocalDateTime.MIN;

	/**
	 * Sets the current from SET_CHARGE_POWER channel
	 * 
	 * Allowed loading current are between 6000mA and 63000mA. Invalid values are
	 * discarded. The value is also depending on the DIP-switch settings and the
	 * used cable of the charging station.
	 */
	private void setPower() {

		WriteChannel<Integer> energyLimitChannel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);

		if (energyLimit == 0 || energyLimit > this.parent.getEnergySession().getNextValue().orElse(0)) {
			WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {

				int target = 0;
				String key = "PowerLimit";
				int maxPower = this.parent.getMaximumHardwarePower().getNextValue().orElse(0);
				Integer power = valueOpt.get();
				ChangeConfigurationRequest request = new ChangeConfigurationRequest();
				ChargingType chargeType = this.parent.getChargingType().getNextValue().asEnum();

				switch (chargeType) {
				case UNDEFINED:
					this.parent.logInfo(this.log, "Please set the type of charging");
					return;
				case AC:
					int phases = this.parent.getPhases().getNextValue().orElse(3);
					target = power * 1000 / phases / 230 /* voltage */ ;

					if (target > maxPower / phases / 230) {
						target = maxPower / phases / 230;
					}
					key = "loutAcMax";
					break;
				case CCS:
					target = target > maxPower ? maxPower : target;
					break;
				case CHADEMO:
					this.parent.logInfo(this.log,
							"The limit cannot be set during the charging because of the charge type chademo");
					break;

				}

				if (target != lastTarget || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {

					this.parent.logInfo(this.log, "Setting EVCS " + this.parent.alias() + " Limit to [" + target + " "
							+ (chargeType == ChargingType.AC ? "A" : "W") + "]");

					request.setKey(key);
					request.setValue(String.valueOf(target));

					UUID id = UUID.fromString(this.parent.getChargingSessionId().getNextValue().orElse(""));
					boolean sentSuccessfully = parent.getConfiguredOcppServer().send(id, request);
					if (sentSuccessfully) {
						this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
						this.lastTarget = target;
					}
				}
			}
		} else {
			this.parent.logInfo(this.log, "Maximum energy limit reached");
			this.parent.status().setNextValue(Status.ENERGY_LIMIT_REACHED);
		}
	}

	private void setEnergyLimit() {
		WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			int energyLimit = valueOpt.get();
			this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyLimit);
		}
	}
}
