package io.openems.edge.battery.soltaro.controller.helper;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.ChargeIndication;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public class DummyBattery extends AbstractOpenemsComponent implements SoltaroBattery, StartStoppable {

	public static int DEFAULT_SOC = 50;
	public static int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static int DEFAULT_MAX_CELL_TEMPERATURE = 33;

	private static final ChargeIndication DEFAULT_CHARGE_INDICATION = ChargeIndication.STANDBY;

	protected DummyBattery(//
	) { //
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SoltaroBattery.ChannelId.values() //
		);

//		getChargeIndication().onSetNextValue( v -> { 
//			
//			if (v != null && v.get() != null) {
//				ChargeIndication indication = v.get();
//				if (indication == ChargeIndication.CHARGING || indication == ChargeIndication.DISCHARGING) {
//					LocalDateTime time = LocalDateTime.now();				
//					long seconds = time.toEpochSecond(ZONE_OFFSET);
//					getNotActiveSince().setNextValue(seconds);
////					ICH WEISS ES NICHT
//				}
//			}
//		});

		setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		setSoc(DEFAULT_SOC);
		setChargeIndication(DEFAULT_CHARGE_INDICATION);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this._setMinCellVoltage(minimalCellVoltage);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this._setMinCellVoltage(null);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this._setMaxCellVoltage(maximalCellVoltage);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	public void setMaximalCellVoltageToUndefined() {
		this._setMaxCellVoltage(null);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this._setMinCellTemperature(minimalCellTemperature);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	public void setMinimalCellTemperatureToUndefined() {
		this._setMinCellTemperature(null);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this._setMaxCellTemperature(maximalCellTemperature);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	public void setMaximalCellTemperatureToUndefined() {
		this._setMaxCellTemperature(null);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	public void setChargeIndication(ChargeIndication chargeIndication) {
		this.getChargeIndication().setNextValue(chargeIndication);
		this.getChargeIndication().nextProcessImage();
	}

	public void setChargeIndicationToUndefined() {
		this.getChargeIndication().setNextValue(null);
		this.getChargeIndication().nextProcessImage();
	}

	public void setSoc(int soc) {
		this._setSoc(soc);
		this.getSocChannel().nextProcessImage();
	}

	public void setSocToUndefined() {
		this._setSoc(null);
		this.getSocChannel().nextProcessImage();
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO start stop is not implemented
		throw new NotImplementedException("Start Stop is not implemented for Soltaro SingleRack Version B");
	}
}
