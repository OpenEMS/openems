package io.openems.core.utilities.power.symmetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.scheduler.BeforeControllerExecutedListener;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.Config;
import io.openems.core.SchedulerInitializedEventListener;
import io.openems.core.ThingRepository;

public class SymmetricPowerClusterImpl extends SymmetricPower implements PowerChangeListener, BeforeControllerExecutedListener {

	private final Logger log = LoggerFactory.getLogger(SymmetricPowerClusterImpl.class);

	private List<Limitation> dynamicLimitations;
	private List<SymmetricEssNature> ess;
	private Map<SymmetricEssNature, EssPowerStateListener> essListeners;
	private PowerState essState = PowerState.WRITE;

	enum PowerState {
		NORMAL, WRITE
	}

	public SymmetricPowerClusterImpl() {
		this.dynamicLimitations = Collections.synchronizedList(new ArrayList<>());
		this.essListeners = Collections.synchronizedMap(new HashMap<>());
		this.ess = new ArrayList<>();
		try {
			Config.getInstance().addSchedulerInitializedEventListener(new SchedulerInitializedEventListener() {

				@Override
				public void onSchedulerInitialized() {
					Scheduler scheduler = ThingRepository.getInstance().getSchedulers().iterator().next();
					scheduler.addBeforeControllerExecutedListener(SymmetricPowerClusterImpl.this);
				}
			});
		} catch (ConfigException e) {
			log.error("Can't load config");
		}
		reset();
	}

	public void addEss(SymmetricEssNature ess) {
		synchronized (this.ess) {
			this.ess.add(ess);
		}
		mergePower();
		EssPowerStateListener listener = new EssPowerStateListener();
		ess.getPower().addBeforePowerWriteListener(listener);
		essListeners.put(ess, listener);
		ess.getPower().addPowerChangeListener(this);
		setMaxApparentPower(getMaxApparentPower() + ess.maxNominalPower().valueOptional().orElse(0L));
	}

	public void removeEss(SymmetricEssNature ess) {
		synchronized (this.ess) {
			this.ess.remove(ess);
		}
		mergePower();
		EssPowerStateListener listener = essListeners.remove(ess);
		ess.getPower().removeBeforePowerWriteListener(listener);
		ess.getPower().removePowerChangeListener(this);
		setMaxApparentPower(getMaxApparentPower() - ess.maxNominalPower().valueOptional().orElse(0L));
	}

	@Override
	public void powerChanged(Geometry allowedPower) {
		if (essState == PowerState.NORMAL) {
			mergePower();
		}
	}

	private void mergePower() {
		Geometry base = FACTORY.createPoint(new CoordinateArraySequence(0));
		synchronized (this.ess) {
			for (SymmetricEssNature ess : this.ess) {
				try {
					if (base.isEmpty()) {
						base = ess.getPower().getGeometry();
					} else {
						base = TopologyPreservingSimplifier.simplify(getUnionAround(base, ess.getPower().getGeometry()),
								getMaxApparentPower() / 100.0);
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
		synchronized (dynamicLimitations) {
			for (Limitation limit : this.dynamicLimitations) {
				Geometry limitedPower;
				try {
					limitedPower = limit.applyLimit(base);
					if (!limitedPower.isEmpty()) {
						base = limitedPower;
					}
				} catch (PowerException e) {
					log.error("Failed to apply Limit after base Power changed!", e);
				}
			}
		}
		try {
			setGeometry(base);
		} catch (PowerException e) {}
	}

	private Geometry getUnionAround(Geometry g1, Geometry g2) {
		Geometry g2dens = Densifier.densify(g2, getMaxApparentPower() / 10.0);
		List<Geometry> geometries = new ArrayList<>();
		geometries.add(g2);
		for (Coordinate c : g2dens.getCoordinates()) {
			geometries.add(AffineTransformation.translationInstance(c.x, c.y).transform(g1));
		}
		GeometryCollection collection = new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]),
				FACTORY);
		return collection.union();
	}

	@Override
	public void applyLimitation(Limitation limit) throws PowerException {
		Geometry limitedPower = limit.applyLimit(getGeometry());
		if (!limitedPower.isEmpty()) {
			setGeometry(limitedPower);
			synchronized (dynamicLimitations) {
				this.dynamicLimitations.add(limit);
			}
		} else {
			throw new PowerException("No possible Power after applying Limit. Limit is not applied!");
		}
	}

	@Override
	protected void writePower() {
		super.writePower();
		if (dynamicLimitations.size() > 0) {
			synchronized (this.ess) {
				Point p = reduceToZero();
				try {
					setGeometry(p);
				} catch (PowerException e1) {}
				long activePower = (long) p.getCoordinate().x;
				long reactivePower = (long) p.getCoordinate().y;
				long socSum = 0;
				for (SymmetricEssNature ess : this.ess) {
					socSum += ess.soc().valueOptional().orElse(0L) - ess.minSoc().valueOptional().orElse(0);
				}
				if (activePower > 0) {
					/*
					 * Discharge
					 */
					// sort ess by useableSoc asc
					Collections.sort(ess, (a, b) -> {
						return (int) ((a.soc().valueOptional().orElse(0L) - a.minSoc().valueOptional().orElse(0))
								- (b.soc().valueOptional().orElse(0L) - b.minSoc().valueOptional().orElse(0)));
					});
					for (int i = 0; i < ess.size(); i++) {
						SymmetricEssNature ess = this.ess.get(i);
						// calculate minimal power needed to fulfill the calculatedPower
						long minP = activePower;
						for (int j = i + 1; j < this.ess.size(); j++) {
							if (this.ess.get(j).soc().valueOptional().orElse(0L)
									- this.ess.get(j).minSoc().valueOptional().orElse(0) > 0) {
								minP -= this.ess.get(j).getPower().getMaxP().orElse(0L);
							}
						}
						if (minP < 0) {
							minP = 0;
						}
						// check maximal power to avoid larger charges then calculatedPower
						long maxP = ess.getPower().getMaxP().orElse(0L);
						if (activePower < maxP) {
							maxP = activePower;
						}
						double diff = maxP - minP;
						/*
						 * weight the range of possible power by the useableSoc
						 * if the useableSoc is negative the ess will be charged
						 */
						long power = (long) (Math.ceil(minP + diff / socSum
								* (ess.soc().valueOptional().orElse(0L) - ess.minSoc().valueOptional().orElse(0))));
						PEqualLimitation limit = new PEqualLimitation(ess.getPower());
						limit.setP(power);
						try {
							ess.getPower().applyLimitation(limit);
							activePower -= power;
						} catch (PowerException e) {
							log.error("Failed to set activePower on " + ess.id());
						}
					}
				} else {
					/*
					 * Charge
					 */
					/*
					 * sort ess by 100 - useabelSoc
					 * 100 - 90 = 10
					 * 100 - 45 = 55
					 * 100 - (- 5) = 105
					 * => ess with negative useableSoc will be charged much more then one with positive useableSoc
					 */
					Collections.sort(this.ess, (a, b) -> {
						return (int) ((100
								- (a.soc().valueOptional().orElse(0L) - a.minSoc().valueOptional().orElse(0)))
								- (100 - (b.soc().valueOptional().orElse(0L) - b.minSoc().valueOptional().orElse(0))));
					});
					for (int i = 0; i < this.ess.size(); i++) {
						SymmetricEssNature ess = this.ess.get(i);
						// calculate minimal power needed to fulfill the calculatedPower
						long minP = activePower;
						for (int j = i + 1; j < this.ess.size(); j++) {
							minP -= this.ess.get(j).getPower().getMinP().orElse(0L);
						}
						if (minP > 0) {
							minP = 0;
						}
						// check maximal power to avoid larger charges then calculatedPower
						long maxP = ess.getPower().getMinP().orElse(0L);
						if (activePower > maxP) {
							maxP = activePower;
						}
						double diff = maxP - minP;
						// weight the range of possible power by the useableSoc
						long power = (long) Math.floor(minP + diff / (this.ess.size() * 100 - socSum) * (100
								- (ess.soc().valueOptional().orElse(0L) - ess.minSoc().valueOptional().orElse(0))));
						PEqualLimitation limit = new PEqualLimitation(ess.getPower());
						limit.setP(power);
						try {
							ess.getPower().applyLimitation(limit);
							activePower -= power;
						} catch (PowerException e) {
							log.error("Failed to set activePower on " + ess.id());
						}
					}
				}

				// sort ess by maxNominalPower asc
				Collections.sort(ess, (a, b) -> {
					return (int) (a.maxNominalPower().valueOptional().orElse(0L)
							- b.maxNominalPower().valueOptional().orElse(0L));
				});
				if (reactivePower > 0) {
					for (int i = 0; i < ess.size(); i++) {
						SymmetricEssNature ess = this.ess.get(i);
						// calculate minimal power needed to fulfill the calculatedPower
						long minQ = reactivePower;
						for (int j = i + 1; j < this.ess.size(); j++) {
							if (this.ess.get(j).maxNominalPower().valueOptional().orElse(0L) > 0) {
								minQ -= this.ess.get(j).getPower().getMaxQ().orElse(0L);
							}
						}
						if (minQ < 0) {
							minQ = 0;
						}
						// check maximal power to avoid larger charges then calculatedPower
						long maxQ = ess.getPower().getMaxQ().orElse(0L);
						if (reactivePower < maxQ) {
							maxQ = reactivePower;
						}
						double diff = maxQ - minQ;
						/*
						 * weight the range of possible power by the useableSoc
						 * if the useableSoc is negative the ess will be charged
						 */
						long power = (long) (Math.ceil(minQ
								+ diff / getMaxApparentPower() * ess.maxNominalPower().valueOptional().orElse(0L)));
						QEqualLimitation limit = new QEqualLimitation(ess.getPower());
						limit.setQ(power);
						try {
							ess.getPower().applyLimitation(limit);
							reactivePower -= power;
						} catch (PowerException e) {
							log.error("Failed to set reactivePower on " + ess.id());
						}
					}
				} else {
					for (int i = 0; i < this.ess.size(); i++) {
						SymmetricEssNature ess = this.ess.get(i);
						// calculate minimal power needed to fulfill the calculatedPower
						long minQ = reactivePower;
						for (int j = i + 1; j < this.ess.size(); j++) {
							minQ -= this.ess.get(j).getPower().getMinQ().orElse(0L);
						}
						if (minQ > 0) {
							minQ = 0;
						}
						// check maximal power to avoid larger charges then calculatedPower
						long maxQ = ess.getPower().getMinQ().orElse(0L);
						if (reactivePower > maxQ) {
							maxQ = reactivePower;
						}
						double diff = maxQ - minQ;
						// weight the range of possible power by the useableSoc
						long power = (long) Math.floor(
								minQ + diff / getMaxApparentPower() * ess.maxNominalPower().valueOptional().orElse(0L));
						QEqualLimitation limit = new QEqualLimitation(ess.getPower());
						limit.setQ(power);
						try {
							ess.getPower().applyLimitation(limit);
							reactivePower -= power;
						} catch (PowerException e) {
							log.error("Failed to set reactivePower on " + ess.id());
						}
					}
				}
			}
		}
	}

	@Override
	protected void reset() {
		synchronized (dynamicLimitations) {
			this.dynamicLimitations.clear();
		}
		super.reset();
	}

	public class EssPowerStateListener implements BeforePowerWriteListener {
		private boolean write = true;

		public boolean isWrite() {
			return write;
		}

		public void reset() {
			write = false;
		}

		@Override
		public void beforeWritePower() {
			synchronized (essListeners) {
				write = true;
				updateEssState();
			}
		}

	}

	private void updateEssState() {
		synchronized (essState) {
			switch (essState) {
			case NORMAL:
				for (EssPowerStateListener listener : essListeners.values()) {
					if (listener.isWrite()) {
						essState = PowerState.WRITE;
						writePower();
						return;
					}
				}
				break;
			case WRITE:
				for (EssPowerStateListener listener : essListeners.values()) {
					if (listener.isWrite()) {
						return;
					}
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void beforeControllerExecuted() {
		reset();
		mergePower();
		for (EssPowerStateListener listener : essListeners.values()) {
			listener.reset();
		}
		essState = PowerState.NORMAL;
	}

}
