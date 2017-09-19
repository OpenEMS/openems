package io.openems.impl.device.simulator;

import com.google.gson.JsonObject;

import io.openems.api.channel.WriteChannel;
import io.openems.core.ThingRepository;

public class SambiaOffGridLoadGenerator implements LoadGenerator {

	private final ThingRepository repo = ThingRepository.getInstance();
	private WriteChannel<Boolean> do1;
	private WriteChannel<Boolean> do2;
	private WriteChannel<Boolean> do3;
	private WriteChannel<Boolean> do4;
	private WriteChannel<Boolean> do5;
	private WriteChannel<Boolean> do6;
	private WriteChannel<Boolean> do7;
	private WriteChannel<Boolean> do8;
	private WriteChannel<Long> setPivotOn;
	private WriteChannel<Long> setBorehole1On;
	private WriteChannel<Long> setBorehole2On;
	private WriteChannel<Long> setBorehole3On;
	private WriteChannel<Long> setClima1On;
	private WriteChannel<Long> setClima2On;
	private WriteChannel<Long> setOfficeOn;
	private WriteChannel<Long> setTraineeCenterOn;
	private String ess;

	public SambiaOffGridLoadGenerator(JsonObject config) {
		this.ess = config.get("ess").getAsString();
	}

	public SambiaOffGridLoadGenerator() {

	}

	@Override
	public long getLoad() {
		loadChannels();
		long power = 200L;
		if (ess != null) {
			switch (ess) {
			case "ess0": {
				if (do1 != null && do1.valueOptional().isPresent() && do1.valueOptional().get()) {
					// SupplyBus 1
					if (setPivotOn != null && setPivotOn.valueOptional().isPresent()
							&& setPivotOn.valueOptional().get() == 1L) {
						power += 17000;
					}
					if (setOfficeOn != null && setOfficeOn.valueOptional().isPresent()
							&& setOfficeOn.valueOptional().get() == 1L) {
						power += 4000;
					}
					if (setTraineeCenterOn != null && setTraineeCenterOn.valueOptional().isPresent()
							&& setTraineeCenterOn.valueOptional().get() == 1L) {
						power += 6000;
					}
				} else if (do5 != null && do5.valueOptional().isPresent() && do5.valueOptional().get()) {
					// SupplyBus 2
					if (setBorehole1On != null && setBorehole1On.valueOptional().isPresent()
							&& setBorehole1On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole2On != null && setBorehole2On.valueOptional().isPresent()
							&& setBorehole2On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole3On != null && setBorehole3On.valueOptional().isPresent()
							&& setBorehole3On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setClima1On != null && setClima1On.valueOptional().isPresent()
							&& setClima1On.valueOptional().get() == 1L) {
						power += 2000;
					}
					if (setClima2On != null && setClima2On.valueOptional().isPresent()
							&& setClima2On.valueOptional().get() == 1L) {
						power += 2000;
					}
				}
			}
				break;
			case "ess1": {
				if (do2 != null && do2.valueOptional().isPresent() && do2.valueOptional().get()) {
					// SupplyBus 1
					if (setPivotOn != null && setPivotOn.valueOptional().isPresent()
							&& setPivotOn.valueOptional().get() == 1L) {
						power += 17000;
					}
					if (setOfficeOn != null && setOfficeOn.valueOptional().isPresent()
							&& setOfficeOn.valueOptional().get() == 1L) {
						power += 4000;
					}
					if (setTraineeCenterOn != null && setTraineeCenterOn.valueOptional().isPresent()
							&& setTraineeCenterOn.valueOptional().get() == 1L) {
						power += 6000;
					}
				} else if (do6 != null && do6.valueOptional().isPresent() && do6.valueOptional().get()) {
					// SupplyBus 2
					if (setBorehole1On != null && setBorehole1On.valueOptional().isPresent()
							&& setBorehole1On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole2On != null && setBorehole2On.valueOptional().isPresent()
							&& setBorehole2On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole3On != null && setBorehole3On.valueOptional().isPresent()
							&& setBorehole3On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setClima1On != null && setClima1On.valueOptional().isPresent()
							&& setClima1On.valueOptional().get() == 1L) {
						power += 2000;
					}
					if (setClima2On != null && setClima2On.valueOptional().isPresent()
							&& setClima2On.valueOptional().get() == 1L) {
						power += 2000;
					}
				}
			}
				break;
			case "ess2": {
				if (do3 != null && do3.valueOptional().isPresent() && do3.valueOptional().get()) {
					// SupplyBus 1
					if (setPivotOn != null && setPivotOn.valueOptional().isPresent()
							&& setPivotOn.valueOptional().get() == 1L) {
						power += 17000;
					}
					if (setOfficeOn != null && setOfficeOn.valueOptional().isPresent()
							&& setOfficeOn.valueOptional().get() == 1L) {
						power += 4000;
					}
					if (setTraineeCenterOn != null && setTraineeCenterOn.valueOptional().isPresent()
							&& setTraineeCenterOn.valueOptional().get() == 1L) {
						power += 6000;
					}
				} else if (do7 != null && do7.valueOptional().isPresent() && do7.valueOptional().get()) {
					// SupplyBus 2
					if (setBorehole1On != null && setBorehole1On.valueOptional().isPresent()
							&& setBorehole1On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole2On != null && setBorehole2On.valueOptional().isPresent()
							&& setBorehole2On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole3On != null && setBorehole3On.valueOptional().isPresent()
							&& setBorehole3On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setClima1On != null && setClima1On.valueOptional().isPresent()
							&& setClima1On.valueOptional().get() == 1L) {
						power += 2000;
					}
					if (setClima2On != null && setClima2On.valueOptional().isPresent()
							&& setClima2On.valueOptional().get() == 1L) {
						power += 2000;
					}
				}
			}
				break;
			case "ess3": {
				if (do4 != null && do4.valueOptional().isPresent() && do4.valueOptional().get()) {
					// SupplyBus 1
					if (setPivotOn != null && setPivotOn.valueOptional().isPresent()
							&& setPivotOn.valueOptional().get() == 1L) {
						power += 17000;
					}
					if (setOfficeOn != null && setOfficeOn.valueOptional().isPresent()
							&& setOfficeOn.valueOptional().get() == 1L) {
						power += 4000;
					}
					if (setTraineeCenterOn != null && setTraineeCenterOn.valueOptional().isPresent()
							&& setTraineeCenterOn.valueOptional().get() == 1L) {
						power += 6000;
					}
				} else if (do8 != null && do8.valueOptional().isPresent() && do8.valueOptional().get()) {
					// SupplyBus 2
					if (setBorehole1On != null && setBorehole1On.valueOptional().isPresent()
							&& setBorehole1On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole2On != null && setBorehole2On.valueOptional().isPresent()
							&& setBorehole2On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setBorehole3On != null && setBorehole3On.valueOptional().isPresent()
							&& setBorehole3On.valueOptional().get() == 1L) {
						power += 7500;
					}
					if (setClima1On != null && setClima1On.valueOptional().isPresent()
							&& setClima1On.valueOptional().get() == 1L) {
						power += 2000;
					}
					if (setClima2On != null && setClima2On.valueOptional().isPresent()
							&& setClima2On.valueOptional().get() == 1L) {
						power += 2000;
					}
				}
			}
				break;
			}
		} else {
			if (setPivotOn != null && setPivotOn.valueOptional().isPresent()
					&& setPivotOn.valueOptional().get() == 1L) {
				power += 17000;
			}
			if (setOfficeOn != null && setOfficeOn.valueOptional().isPresent()
					&& setOfficeOn.valueOptional().get() == 1L) {
				power += 4000;
			}
			if (setTraineeCenterOn != null && setTraineeCenterOn.valueOptional().isPresent()
					&& setTraineeCenterOn.valueOptional().get() == 1L) {
				power += 6000;
			}
			if (setBorehole1On != null && setBorehole1On.valueOptional().isPresent()
					&& setBorehole1On.valueOptional().get() == 1L) {
				power += 7500;
			}
			if (setBorehole2On != null && setBorehole2On.valueOptional().isPresent()
					&& setBorehole2On.valueOptional().get() == 1L) {
				power += 7500;
			}
			if (setBorehole3On != null && setBorehole3On.valueOptional().isPresent()
					&& setBorehole3On.valueOptional().get() == 1L) {
				power += 7500;
			}
			if (setClima1On != null && setClima1On.valueOptional().isPresent()
					&& setClima1On.valueOptional().get() == 1L) {
				power += 2000;
			}
			if (setClima2On != null && setClima2On.valueOptional().isPresent()
					&& setClima2On.valueOptional().get() == 1L) {
				power += 2000;
			}
		}
		return power;
	}

	@SuppressWarnings("unchecked")
	private void loadChannels() {
		if (do1 == null) {
			do1 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO1").orElse(null);
		}
		if (do2 == null) {
			do2 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO2").orElse(null);
		}
		if (do3 == null) {
			do3 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO3").orElse(null);
		}
		if (do4 == null) {
			do4 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO4").orElse(null);
		}
		if (do5 == null) {
			do5 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO5").orElse(null);
		}
		if (do6 == null) {
			do6 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO6").orElse(null);
		}
		if (do7 == null) {
			do7 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO7").orElse(null);
		}
		if (do8 == null) {
			do8 = (WriteChannel<Boolean>) repo.getChannelByAddress("output0/DO8").orElse(null);
		}
		if (setPivotOn == null) {
			setPivotOn = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetPivotOn").orElse(null);
		}
		if (setBorehole1On == null) {
			setBorehole1On = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetBorehole1On").orElse(null);
		}
		if (setBorehole2On == null) {
			setBorehole2On = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetBorehole2On").orElse(null);
		}
		if (setBorehole3On == null) {
			setBorehole3On = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetBorehole3On").orElse(null);
		}
		if (setClima1On == null) {
			setClima1On = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetClima1On").orElse(null);
		}
		if (setClima2On == null) {
			setClima2On = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetClima2On").orElse(null);
		}
		if (setOfficeOn == null) {
			setOfficeOn = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetOfficeOn").orElse(null);
		}
		if (setTraineeCenterOn == null) {
			setTraineeCenterOn = (WriteChannel<Long>) repo.getChannelByAddress("sps0/SetTraineeCenterOn").orElse(null);
		}
	}

}
