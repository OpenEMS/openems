package io.openems.impl.controller.symmetric.cosphicharacteristic;

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

public class CosPhiCharacteristicController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<List<Long[]>> cosPhiPoints = new ConfigChannel<List<Long[]>>("cosPhiPoints", this,
			Long[].class);

	public List<Point> cosPhiCharacteristic;

	public CosPhiCharacteristicController() {
		cosPhiPoints.changeListener(new ChannelChangeListener() {

			@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					List<Point> points = new ArrayList<>();
					for (Long[] arr : cosPhiPoints.value()) {
						points.add(new Point(arr[0], arr[1]));
					}
					cosPhiCharacteristic = points;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override public void run() {
		try {
			if (ess.value().setActivePower.peekWrite().isPresent()) {
				double pRatio = (double) ess.value().setActivePower.peekWrite().get()
						/ (double) ess.value().nominalPower.value() * 100;
				double cosPhi = ControllerUtils.getValueOfLine(cosPhiCharacteristic, pRatio) / 100;
				ess.value().power.setReactivePower(
						ControllerUtils.calculateReactivePower(ess.value().setActivePower.peekWrite().get(), cosPhi));
				ess.value().power.writePower();
				log.info("Set reactive power [{}] to get cosPhi [{}]",
						new Object[] { ess.value().power.getReactivePower(), cosPhi });
			} else {
				log.error(ess.id() + " no ActivePower is Set.");
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
