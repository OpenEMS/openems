package io.openems.core.utilities.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.scheduler.AfterControllerExecutedListener;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.Config;
import io.openems.core.SchedulerInitializedEventListener;
import io.openems.core.ThingRepository;

public class SymmetricPowerClusterImpl extends SymmetricPower
implements PowerChangeListener, AfterControllerExecutedListener {

	private List<Limitation> dynamicLimitations;
	private List<SymmetricEssNature> ess;

	public SymmetricPowerClusterImpl() {
		this.dynamicLimitations = new ArrayList<>();
		this.ess = new ArrayList<>();
		try {
			Config.getInstance().addSchedulerInitializedEventListener(new SchedulerInitializedEventListener() {

				@Override
				public void onSchedulerInitialized() {
					Scheduler scheduler = ThingRepository.getInstance().getSchedulers().iterator().next();
					scheduler.addListener(SymmetricPowerClusterImpl.this);
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
		ess.getPower().addListener(this);
		setMaxApparentPower(getMaxApparentPower() + ess.maxNominalPower().valueOptional().orElse(0L));
	}

	public void removeEss(SymmetricEssNature ess) {
		synchronized (this.ess) {
			this.ess.remove(ess);
		}
		mergePower();
		ess.getPower().removeListener(this);
		setMaxApparentPower(getMaxApparentPower() - ess.maxNominalPower().valueOptional().orElse(0L));
	}

	@Override
	public void powerChanged(Geometry allowedPower) {
		mergePower();
	}

	private void mergePower() {
		Geometry base = FACTORY.createPoint(new Coordinate(0, 0));
		synchronized (this.ess) {
			for (SymmetricEssNature ess : this.ess) {
				base = getUnionAround(base, ess.getPower().getGeometry());
			}
		}
		synchronized (this.dynamicLimitations) {
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
			setGeometry(base);
		}
	}

	private Geometry getUnionAround(Geometry g1, Geometry g2) {
		Geometry g1dens = Densifier.densify(g1, 10000);
		Geometry g2dens = Densifier.densify(g2, 10000);
		List<Geometry> geometries = new ArrayList<>();
		geometries.add(g1);
		for (Coordinate c : g1dens.getCoordinates()) {
			geometries.add(AffineTransformation.translationInstance(c.x, c.y).transform(g2));
		}
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
		synchronized (this.dynamicLimitations) {
			Geometry limitedPower = limit.applyLimit(getGeometry());
			if (!limitedPower.isEmpty()) {
				setGeometry(limitedPower);
				this.dynamicLimitations.add(limit);
			} else {
				throw new PowerException("No possible Power after applying Limit. Limit is not applied!");
			}
		}
	}

	private void setPower() {
		synchronized (this.ess) {
			Point p = reduceToZero();
			setGeometry(p);
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
					return (int) ((100 - (a.soc().valueOptional().orElse(0L) - a.minSoc().valueOptional().orElse(0)))
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
					long power = (long) Math.floor(minP + diff / (this.ess.size() * 100 - socSum)
							* (100 - (ess.soc().valueOptional().orElse(0L) - ess.minSoc().valueOptional().orElse(0))));
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
					long power = (long) (Math.ceil(
							minQ + diff / getMaxApparentPower() * ess.maxNominalPower().valueOptional().orElse(0L)));
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

	@Override
	protected void reset() {
		this.dynamicLimitations.clear();
		super.reset();
	}

	@Override
	public void afterControllerExecuted() {
		setPower();
		reset();
	}

}
