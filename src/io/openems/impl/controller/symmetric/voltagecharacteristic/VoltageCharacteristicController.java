package io.openems.impl.controller.symmetric.voltagecharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Point;
import io.openems.core.utilities.Power;

public class VoltageCharacteristicController extends Controller {

	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> uNenn = new ConfigChannel<>("UNenn", this, Integer.class);

	public final ConfigChannel<List<Long[]>> pByUCharacteristicPoints = new ConfigChannel<>("pByUCharacteristicPoints",
			this, Long[].class);
	public final ConfigChannel<List<Long[]>> qByUCharacteristicPoints = new ConfigChannel<>("qByUCharacteristicPoints",
			this, Long[].class);
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<Boolean>("activePowerActivated", this,
			Boolean.class).defaultValue(true);
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<Boolean>("reactivePowerActivated",
			this, Boolean.class).defaultValue(true);

	private List<Point> pCharacteristic;

	private List<Point> qCharacteristic;

	public VoltageCharacteristicController() {
		super();
		init();
	}

	public VoltageCharacteristicController(String thingId) {
		super(thingId);
		init();
	}

	private void init() {
		pByUCharacteristicPoints.changeListener(new ChannelChangeListener() {

			@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					List<Point> points = new ArrayList<>();
					for (Long[] arr : pByUCharacteristicPoints.value()) {
						points.add(new Point(arr[0], arr[1]));
					}
					pCharacteristic = points;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		qByUCharacteristicPoints.changeListener(new ChannelChangeListener() {

			@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					List<Point> points = new ArrayList<>();
					for (Long[] arr : qByUCharacteristicPoints.value()) {
						points.add(new Point(arr[0], arr[1]));
					}
					qCharacteristic = points;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override public void run() {
		try {
			Power power = ess.value().power;
			double uRatio = (double) meter.value().voltage.value() / (double) uNenn.value() * 100.0;
			long nominalActivePower = ess.value().maxNominalPower.value();
			long nominalReactivePower = ess.value().maxNominalPower.value();
			power.setActivePower(
					(long) (nominalActivePower / 100.0 * ControllerUtils.getValueOfLine(pCharacteristic, uRatio)));
			power.setReactivePower(
					(long) (nominalReactivePower / 100.0 * ControllerUtils.getValueOfLine(qCharacteristic, uRatio)));
			power.writePower();
			log.info(ess.id() + " Set ActivePower [" + power.getActivePower() + "], ReactivePower ["
					+ power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read Value.", e);
		}
	}

}
