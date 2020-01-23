package io.openems.edge.battery.soltaro.controller.state;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.BatteryHandlingController;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.CircularTreeMap;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public abstract class BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(BaseState.class);
	private ManagedSymmetricEss ess;
	private Battery bms;
	
	public BaseState(ManagedSymmetricEss ess, Battery bms) {
		this.ess = ess;
		this.bms = bms;
	}

	protected void denyCharge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(BatteryHandlingController.class.getName(), ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}
	
	protected void denyDischarge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(BatteryHandlingController.class.getName(), ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}
	
	protected void chargeEssWithPercentOfMaxPower(int chargePowerPercent) {
		int maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int calculatedPower = maxCharge / 100 * chargePowerPercent;
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			log.error(e.getMessage());
		}		
	}
	
	protected boolean isChargeOrDischargeIndicationPresent(long timeInSeconds) {
		if (noValuesPresent(timeInSeconds)) {
			return false; 
		}
		
		if (isChargeOrDischargePresent(timeInSeconds)) {
			return true;
		}
		
		return false;
	}
	
	private boolean isChargeOrDischargePresent(long timeInSeconds) {
		CircularTreeMap<LocalDateTime, ?> values = this.getChargeIndicationValues();
		
		for (LocalDateTime dateTime : values.keySet()) {
			if (dateTime.plusSeconds(timeInSeconds).isAfter(LocalDateTime.now())) {
				// entry is in the time span
				try {
					Object x = ((Value<?>) values.get(dateTime)).get();
					if (x instanceof Integer) {
						if ((Integer) x > 0) {
							return true;
						}
					}
					if (x instanceof OptionsEnum) {
						if (((OptionsEnum) x).getValue()  > 0) {
							return true;
						}
					}
					
				} catch (Exception e) {
					log.error(e.getMessage());
				}				
			}
		}
		
		return false;
	}

	private boolean noValuesPresent(long timeInSeconds) {
		return this.getChargeIndicationValues() == null;		
	}
	
	protected boolean isNextStateUndefined() {
		if (ess == null) {
			return true;
		}
		
 		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
		if (!minCellVoltageOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> maxCellVoltageOpt = ess.getMaxCellVoltage().value().asOptional();
		if (!maxCellVoltageOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> maxCellTemperatureOpt = ess.getMaxCellTemperature().value().asOptional();
		if (!maxCellTemperatureOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> minCellTemperatureOpt = ess.getMinCellTemperature().value().asOptional();
		if (!minCellTemperatureOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();
		if (!socOpt.isPresent()) {
			return true;
		}
		
		return false;
	}
	
	protected int getEssSoC() {
		return this.ess.getSoc().value().get();
	}
	
	protected int getEssMinCellTemperature() {
		return this.ess.getMinCellTemperature().value().get();
	}

	protected int getEssMaxCellTemperature() {
		return this.ess.getMaxCellTemperature().value().get();
	}
	
	protected int getEssMinCellVoltage() {
		return this.ess.getMinCellVoltage().value().get();
	}

	protected int getEssMaxCellVoltage() {
		return this.ess.getMaxCellVoltage().value().get();
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}
	
	public CircularTreeMap<LocalDateTime, ?> getChargeIndicationValues() {
		CircularTreeMap<LocalDateTime, ?> pastValues = null;
		
		ChannelId id = null;
		if (bms instanceof io.openems.edge.battery.soltaro.single.versiona.SingleRack) {
			id = io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.CLUSTER_1_CHARGE_INDICATION;			
		} else if (bms instanceof io.openems.edge.battery.soltaro.single.versionb.SingleRack) {
			id = io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.CLUSTER_1_CHARGE_INDICATION;
		} else if (bms instanceof io.openems.edge.battery.soltaro.cluster.versionb.Cluster) {
			id = io.openems.edge.battery.soltaro.cluster.versionb.ClusterChannelId.CHARGE_INDICATION;
		} else {
			//sarch for "charge" and "indication" manually
			for (Channel<?> c : bms.channels()) {
				String name = c.address().getChannelId().toLowerCase();
				if (name.contains("charge") && name.contains("indication")) {
					id = c.channelId();
					break;
				}
			}
		}
		
		if (id != null) {
			Channel<?> channel = bms.channel(id);
			if (channel != null) {
				pastValues = channel.getPastValues();	
			}
		}
		
		return pastValues;
	}
}
