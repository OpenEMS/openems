package io.openems.edge.evcs.ocpp.core;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
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

	// Minimum pause between two consecutive writes
	private final static int WRITE_INTERVAL_SECONDS = 30;

	public WriteHandler(AbstractOcppEvcsComponent parent) {
		this.parent = parent;
	}

	/**
	 * Sends commands to the loading station depending on which profiles it
	 * implements.
	 * 
	 * <p>
	 * It is not sending an command, if the communication failed or the write
	 * channel is not set.
	 */
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
				if (!(this.parent instanceof ManagedEvcs)) {
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

	private Integer lastTarget = null;
	private LocalDateTime nextPowerWrite = LocalDateTime.MIN;

	/**
	 * Sets the current or power from SET_CHARGE_POWER channel.
	 * <p>
	 * Depending on the charging type it will send different commands with different
	 * units. Invalid values are discarded. If the energy limit is reached it will
	 * send zero.
	 */
	private void setPower() {
		WriteChannel<Integer> energyLimitChannel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);

		if (energyLimit == 0 || energyLimit > this.parent.getEnergySession().getNextValue().orElse(0)) {
			WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();

			if (valueOpt.isPresent()) {

				String key = "PowerLimit";
				int maxPower = this.parent.getMaximumHardwarePower().getNextValue().orElse(0);
				Integer power = valueOpt.get();
				Integer target = power;
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

				/*
				 * Only if the target has changed or a time has passed.
				 */
				if (!target.equals(lastTarget) || this.nextPowerWrite.isBefore(LocalDateTime.now())) {

					request.setKey(key);
					request.setValue(String.valueOf(target));

					UUID id = UUID.fromString(this.parent.getChargingSessionId().getNextValue().orElse(""));

					try {
						this.parent.getConfiguredOcppServer().send(id, request)
								.whenComplete((confirmation, throwable) -> {
									this.parent.logInfo(log, confirmation.toString());
								});

						this.parent.logInfo(this.log, "Setting EVCS " + this.parent.alias() + " charge power to ["
								+ target + " " + (chargeType == ChargingType.AC ? "A" : "W") + "]");

						this.nextPowerWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
						this.lastTarget = target;

					} catch (OccurenceConstraintException e) {
						this.parent.logWarn(log, "The request is not a valid OCPP request.");
					} catch (UnsupportedFeatureException e) {
						this.parent.logWarn(log, "This feature is not implemented by the charging station.");
					} catch (NotConnectedException e) {
						this.parent.logWarn(log, "The server is not connected.");
					}
				}
			}
		} else {
			this.parent.logInfo(this.log, "Maximum energy limit reached");
			this.parent.status().setNextValue(Status.ENERGY_LIMIT_REACHED);
		}
	}

	private Integer lastEnergySession = null;
	private LocalDateTime nextEnergySessionWrite = LocalDateTime.MIN;

	/**
	 * Sets the nextValue of the SET_ENERGY_LIMIT channel.
	 */
	private void setEnergyLimit() {
		WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			Integer energyLimit = valueOpt.get();

			/*
			 * Only if the target has changed or a time has passed.
			 */
			if (!energyLimit.equals(lastEnergySession) || this.nextEnergySessionWrite.isBefore(LocalDateTime.now())) {

				this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyLimit);
				this.parent.logInfo(this.log, "Setting OCPP EVCS " + this.parent.alias()
						+ " Energy Limit in this Session to [" + energyLimit + " Wh]");
				lastEnergySession = energyLimit;
				nextEnergySessionWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
			}
		}
	}
}
