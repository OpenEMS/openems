package io.openems.edge.controller.symmetric.peaklimiting;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.PeakLimiting", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PeakLimiting extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PeakLimiting.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public PeakLimiting() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	private ZonedDateTime t0 = null;

	private ZonedDateTime t1 = null;

	private ZonedDateTime t2 = null;

	private ZonedDateTime t3 = null;

	private ZonedDateTime fromDate = null;

	private ZonedDateTime now = null;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.setTimeDeltas();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		SymmetricMeter gridmeter = this.componentManager.getComponent(this.config.grid_meter_id());
		this.now = ZonedDateTime.now();

		if (this.fromDate != null) {
			ZonedDateTime yesterday = this.now.minusDays(1);
			if (yesterday.getDayOfYear() != this.fromDate.getDayOfYear()) {
				this.setTimeDeltas();
			}
		}

		if (this.config.simple() || timeBasedMode(gridmeter, ess) == false) {
			this.simpleMode(gridmeter, ess);
		}

	}

	private boolean simpleMode(SymmetricMeter gridmeter, ManagedSymmetricEss ess) throws OpenemsNamedException {

		int soc = ess.getSoc().value().orElse(0);
		int maxP = this.config.maxPower();
		IntegerReadChannel productionChannel = this.componentManager
				.getChannel(ChannelAddress.fromString("_sum/ProductionActivePower"));
		int production = productionChannel.getNextValue().orElse(0);

		IntegerReadChannel consumptionChannel = this.componentManager
				.getChannel(ChannelAddress.fromString("_sum/ConsumptionActivePower"));
		int consumption = consumptionChannel.value().orElse(0);

		int surplus = production - consumption;

		if ((surplus <= maxP) && soc >= this.config.maxSOC()) { // don't charge

			this.calcAndSet(ess, gridmeter, surplus * -1);
			this.logInfo(log, "Simple Mode: Dont't Charge");
			return true;
		}

		if ((surplus > maxP) && soc > this.config.maxSOC()) { // feed in maxP

			this.calcAndSet(ess, gridmeter, this.config.maxPower() * -1);

			this.logInfo(log, "Simple Mode: Feed in Limit");
			return true;
		}

		this.logInfo(log, "Simple Mode: Balance");
		this.calcAndSet(ess, gridmeter, 0);

		return false;

	}

	private boolean timeBasedMode(SymmetricMeter gridmeter, ManagedSymmetricEss ess) throws OpenemsNamedException {

		ZonedDateTime t1today = this.t1.plusDays(1);
		ZonedDateTime t3today = this.t3.plusDays(1);
		int soc = ess.getSoc().value().getOrError();

		Sum sum;
		try {
			sum = this.componentManager.getComponent("_sum");
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		Integer consumptionNow = sum.getConsumptionActivePower().value().getOrError();
		Integer productionNow = sum.getProductionActivePower().value().getOrError();

		if (this.now.isBefore(t1today)) {
			if (soc >= this.config.maxSOC()) {

				int setPoint = 0;

				if (productionNow <= this.config.maxPower()) {
					setPoint = productionNow * -1;
				} else {
					setPoint = this.config.maxPower() * -1;
				}

				this.logInfo(log, "Don't Charge , Setpoint: " + setPoint);
				this.calcAndSet(ess, gridmeter, setPoint);
				return true;

			} else {
				this.logInfo(log, "SOC less than " + this.config.maxSOC() + "%, Balance");
				this.calcAndSet(ess, gridmeter, 0);
				return true;
			}
		}

		if ((this.now.isEqual(t1today) || this.now.isAfter(t1today)) && this.now.isBefore(t3today)) {

			if (soc >= this.config.maxSOC()) {

				if (productionNow <= this.config.maxPower()) {
					this.logInfo(log, "Surplus < maxP, Don't Charge");
					int setPoint = productionNow * -1;
					this.calcAndSet(ess, gridmeter, setPoint);
					return true;
				}

				int remainCapacity = (ess.getCapacity().value().orElse(0) * (1 - (soc / 100)))
						* (this.config.SOCTarget() / 100);

				int deltaT = t3today.getHour() - this.now.getHour();
				if (deltaT == 0) {
					deltaT = 1;
				}

				int charge = remainCapacity / deltaT;

				if ((productionNow - consumptionNow) < charge) {
					this.logInfo(log, "Surplus < charge, Balance");
					this.calcAndSet(ess, gridmeter, 0);

					return true;
				} else {

					int setPoint = productionNow - consumptionNow - charge;

					if (setPoint > this.config.maxPower()) {
						setPoint = this.config.maxPower();
						this.logInfo(log, "Feed In maxP");
						this.calcAndSet(ess, gridmeter, setPoint * -1);
						return true;
					}
					this.logInfo(log, "trying to charge with " + charge + "W| Setpoint: " + setPoint * -1);
					this.calcAndSet(ess, gridmeter, setPoint * -1);

					return true;
				}

			}
		}

		if (this.now.isEqual(t3today) || this.now.isAfter(t3today)) {
			this.logInfo(log, "no peak limiting");
			this.calcAndSet(ess, gridmeter, 0);

			return true;
		}

		this.logInfo(log, "no peak limiting needed, balance");
		this.calcAndSet(ess, gridmeter, 0);

		return true;
	}

	private void calcAndSet(ManagedSymmetricEss ess, SymmetricMeter gridmeter, int setPoint)
			throws OpenemsNamedException {
		int calcP = this.calculateRequiredPower(ess, gridmeter, setPoint);
		calcP = ess.getPower().fitValueIntoMinMaxPower(this.config.id(), ess, Phase.ALL, Pwr.ACTIVE, calcP);
		this.logInfo(log, "CalcP: " + calcP);
		ess.getSetActivePowerEquals().setNextWriteValue(calcP);
		ess.getSetReactivePowerEquals().setNextWriteValue(0);
	}

	private boolean setTimeDeltas() {

		this.t0 = null;
		this.t1 = null;
		this.t2 = null;
		this.t3 = null;

		int maxP = this.config.maxPower();

		ChannelAddress consumptionChannel;
		try {
			consumptionChannel = ChannelAddress.fromString("_sum/ConsumptionActivePower");
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		ZonedDateTime dateTime = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0);

		Set<ChannelAddress> channels = new HashSet<>();
		ChannelAddress productionChannel;

		try {
			productionChannel = ChannelAddress.fromString("_sum/ProductionActivePower");
		} catch (OpenemsNamedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		channels.add(productionChannel);
		channels.add(consumptionChannel);

		this.fromDate = dateTime.minusDays(1);
		this.logInfo(this.log, "From Date: " + fromDate.toString());
		this.logInfo(this.log, "To Date: " + dateTime.toString());

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> history;
		try {
			history = this.getTimedata().queryHistoricData(null, fromDate, dateTime, channels);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		int maxPV = 0;

		int pvvalue = 0;
		int consvalue = 0;
		int pvy1 = -100000;
		int pvy2 = 0;
		int consy1 = 0;
		int consy2 = 0;
		for (ZonedDateTime t : history.keySet()) {

			SortedMap<ChannelAddress, JsonElement> temp = history.get(t);
			pvvalue = temp.get(productionChannel).getAsInt();

			consvalue = temp.get(consumptionChannel).getAsInt();

			if (pvy1 != -100000) {

				pvy1 = (int) (pvvalue + (0.2 * (pvy1 - pvvalue)));
				pvy2 = (int) (pvvalue + (0.2 * (pvy1 - pvvalue)));

				consy1 = (int) (consvalue + (0.2 * (consy1 - consvalue)));
				consy2 = (int) (consvalue + (0.2 * (consy1 - consvalue)));

				pvvalue = pvy2;
				consvalue = consy2;

			}

			if (history.firstKey().equals(t)) {
				pvy1 = pvvalue;
				consy1 = consvalue;

			}

			// System.out.println("T: " + t.toString() + ", Production: " + pvvalue + ",
			// Consumption: " + consvalue);

			if (this.t0 == null && pvvalue > 0) {
				this.t0 = t;
			}

			if (pvvalue > maxPV) {
				maxPV = pvvalue;
				this.t2 = t;
			}

			if ((pvvalue - consvalue) >= maxP && this.t1 == null) {
				this.t1 = t;
			}
			if (this.t1 != null && pvvalue >= maxP) {
				if (((pvvalue - consvalue) <= maxP) && t.isAfter(this.t2)) {
					this.t3 = t;
				}
			}

		}
		if (this.t1 != null && this.t2 != null && this.t3 != null) {
			this.logInfo(log, "t1: " + this.t1.toString() + "\nt2: " + this.t2.toString() + "\nt3: "
					+ this.t3.toString() + "\nMax PV: " + maxPV);
		}

		return true;
	}

	private int calculateRequiredPower(ManagedSymmetricEss ess, SymmetricMeter meter, int setPoint) {
		return meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ ess.getActivePower().value().orElse(0) /* current charge/discharge Ess */
				- setPoint; /* the configured target setpoint */
	}

	public Timedata getTimedata() throws OpenemsException {
		if (this.timedata != null) {
			return this.timedata;
		}
		throw new OpenemsException("There is no Timedata-Service available!");
	}
}
