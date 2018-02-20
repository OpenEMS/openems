package io.openems.core.utilities.power;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import io.openems.api.bridge.Bridge;
import io.openems.api.bridge.BridgeEvent;
import io.openems.api.bridge.BridgeEventListener;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.WriteChannelException;

public class SymmetricPowerImpl extends SymmetricPower implements LimitationChangedListener, BridgeEventListener {
	/*
	 * Object
	 */

	private WriteChannel<Long> setActivePower;
	private WriteChannel<Long> setReactivePower;
	private Geometry baseGeometry;

	private List<Limitation> staticLimitations;
	private List<Limitation> dynamicLimitations;
	private double lastActivePower = 0;
	private double lastReactivePower = 0;

	public SymmetricPowerImpl(long maxApparentPower, WriteChannel<Long> setActivePower,
			WriteChannel<Long> setReactivePower, Bridge bridge) {
		setMaxApparentPower(maxApparentPower);
		this.staticLimitations = new ArrayList<>();
		this.dynamicLimitations = new ArrayList<>();
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		if(bridge != null) {
			bridge.addListener(this);
		}else {
			log.error("the Bridge is null! the Power Values won't be writte!");
		}
		createBaseGeometry();
		reset();
	}

	public void addStaticLimitation(Limitation limit) {
		this.staticLimitations.add(limit);
		limit.addListener(this);
		createBaseGeometry();
	}

	public void removeStaticLimitation(Limitation limit) {
		limit.removeListener(this);
		this.staticLimitations.remove(limit);
		createBaseGeometry();
	}

	@Override
	protected void reset() {
		this.dynamicLimitations.clear();
		this.setGeometry(baseGeometry);
		super.reset();
	}

	@Override
	public void applyLimitation(Limitation limit) throws PowerException {
		Geometry limitedPower = limit.applyLimit(getGeometry());
		if (!limitedPower.isEmpty()) {
			setGeometry(limitedPower);
			this.dynamicLimitations.add(limit);
		} else {
			throw new PowerException("No possible Power after applying Limit. Limit is not applied!");
		}
	}

	private void writePower() {
		Point p = reduceToZero();
		Coordinate c = p.getCoordinate();
		setGeometry(p);
		double activePowerDelta = c.x - lastActivePower;
		double reactivePowerDelta = c.y - lastReactivePower;
		lastActivePower += activePowerDelta/2;
		lastReactivePower += reactivePowerDelta/2;
		try {
			this.setActivePower.pushWrite((long) lastActivePower);
			this.setReactivePower.pushWrite((long) lastReactivePower);
			setActivePower.shadowCopyAndReset();
			setReactivePower.shadowCopyAndReset();
		} catch (WriteChannelException e) {
			log.error("failed to write Power.", e);
		}
	}

	private void createBaseGeometry() {
		SHAPEFACTORY.setCentre(ZERO);
		SHAPEFACTORY.setSize(getMaxApparentPower() * 2);
		SHAPEFACTORY.setNumPoints(32);
		this.baseGeometry = SHAPEFACTORY.createCircle();
		for (Limitation limit : this.staticLimitations) {
			try {
				Geometry limitedPower = limit.applyLimit(this.baseGeometry);
				if (!limitedPower.isEmpty()) {
					this.baseGeometry = limitedPower;
				} else {
					log.error("Power is empty after applying Limit. " + limit.toString());
				}
			} catch (PowerException e) {
				log.error("Failed to limit Power!", e);
			}
		}
	}

	@Override
	public void onLimitationChange(Limitation sender) {
		if (staticLimitations.contains(sender)) {
			createBaseGeometry();
		}
	}

	@Override
	public void onBridgeChange(BridgeEvent event) {
		switch (event.getPosition()) {
		case BEFOREREADOTHER1:
			this.reset();
			break;
		case BEFOREREADOTHER2:
			break;
		case BEFOREREADREQUIRED:
			break;
		case BEFOREWRITE:
			this.writePower();

			break;
		default:
			break;

		}
	}

}
