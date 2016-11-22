package io.openems.impl.controller.symmetric.voltagecharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Point;

public class VoltageCharacteristicController extends Controller {

	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> uNenn = new ConfigChannel<>("UNenn", this, Integer.class);

	public final ConfigChannel<List<Long[]>> pByUCharacteristicPoints = new ConfigChannel<>("pByUCharacteristicPoints",
			this, Long[].class);
	public final ConfigChannel<List<Long[]>> qByUCharacteristicPoints = new ConfigChannel<>("qByUCharacteristicPoints",
			this, Long[].class);

	private List<Point> pCharacteristic;

	private List<Point> qCharacteristic;

	public VoltageCharacteristicController() {
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
			long uRatio = (long) ((double) meter.value().voltage.value() / (double) uNenn.value() * 100.0);
			long p = ControllerUtils.getValueOfLine(pCharacteristic, uRatio);
			long q = ControllerUtils.getValueOfLine(qCharacteristic, uRatio);
			long maxChargeApparentPower = ess.value().setActivePower.writeMin()
					.orElse(ess.value().allowedCharge.value() * -1);
			long maxDischargeApparentPower = ess.value().setActivePower.writeMax()
					.orElse(ess.value().allowedDischarge.value());
			long reducedP = ControllerUtils.reduceActivePower(p, q, maxChargeApparentPower, maxDischargeApparentPower);
			long reducedQ = ControllerUtils.reduceReactivePower(p, q, maxChargeApparentPower,
					maxDischargeApparentPower);
			ess.value().setActivePower.pushWrite(reducedP);
			ess.value().setReactivePower.pushWrite(reducedQ);
			log.info(ess.id() + " Set ActivePower [" + reducedP + "], ReactivePower [" + reducedQ + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read Value.", e);
		} catch (WriteChannelException e) {
			log.error("Failed to write Value.", e);
		}
	}

}
