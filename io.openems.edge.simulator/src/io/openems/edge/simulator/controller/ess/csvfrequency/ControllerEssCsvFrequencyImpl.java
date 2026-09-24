package io.openems.edge.simulator.controller.ess.csvfrequency;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Controller.Ess.CsvFrequency", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssCsvFrequencyImpl extends AbstractOpenemsComponent
		implements ControllerEssCsvFrequency, Controller, OpenemsComponent {

	private static final double DT = 1.0; // time step in seconds (OpenEMS cycle)

	private final Logger log = LoggerFactory.getLogger(ControllerEssCsvFrequencyImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SimulatorDatasource datasource;

	private Config config = null;
	private FcrStrategy fcr;
	private Ffr1Strategy ffr1;
	private RechargeStrategy recharge;
	private long elapsedSeconds = 0;
	private int currentPower = 0;
	private int rampCycleCounter = 0;

	public ControllerEssCsvFrequencyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssCsvFrequency.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this.elapsedSeconds = 0;
		this.currentPower = 0;
		// Pre-set counter so ramp fires immediately on the first cycle
		this.rampCycleCounter = config.rampCycleInterval();

		// Initialize strategies from config
		this.fcr = new FcrStrategy(//
				config.nominalFrequency(), //
				config.fcrMaxFrequencyDeviation(), //
				config.fcrFrequencyDeadband(), //
				config.fcrSocTarget());

		this.ffr1 = new Ffr1Strategy(//
				config.nominalFrequency(), //
				config.ffr1ThresholdFrequency(), //
				config.ffr1K(), //
				config.ffr1SupportDuration(), //
				config.ffr1RecoveryPeriod());

		this.recharge = new RechargeStrategy(//
				config.rechargeIdmTransactionTime(), //
				config.rechargeSocMin(), //
				config.rechargeSocMax());

		// Update ESS reference filter
		var essUpdated = OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());

		// Update Datasource reference filter
		var datasourceUpdated = OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource",
				config.datasource_id());

		return essUpdated || datasourceUpdated;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled()) {
			return;
		}

		try {
			// 1. Read frequency from CSV
			var freqChannelAddress = new ChannelAddress("", this.config.frequencyColumnName());
			var freqValue = this.datasource.getValue(OpenemsType.DOUBLE, freqChannelAddress);

			if (freqValue == null) {
				this.log.warn("Frequency value from CSV is null - column '{}' not found or no data",
						this.config.frequencyColumnName());
				return;
			}

			var frequency = ((Number) freqValue).doubleValue();

			// 2. Get SOC from ESS
			var socValue = this.ess.getSoc();
			double soc;
			if (socValue.isDefined()) {
				soc = socValue.get().doubleValue();
			} else {
				this.log.warn("ESS SOC is not available, using default 50%");
				soc = 50.0;
			}

			// 3. Compute stacked power (FCR=50%, FFR=25%, IDM=25% of total power)
			var totalRatedPower = this.config.power();
			var fcrPowerRating = totalRatedPower * 0.50;
			var ffrPowerRating = totalRatedPower * 0.25;
			var idmPowerRating = totalRatedPower * 0.25;

			var fcrResponsePu = this.fcr.next(frequency, soc);
			var ffr1ResponsePu = this.ffr1.next(frequency, DT);
			var rechargeResponsePu = this.recharge.next(this.elapsedSeconds, soc);

			var fcrPowerW = (int) Math.round(fcrResponsePu * fcrPowerRating);
			var ffr1PowerW = (int) Math.round(ffr1ResponsePu * ffrPowerRating);
			var rechargePowerW = (int) Math.round(rechargeResponsePu * idmPowerRating);

			var targetPower = fcrPowerW + ffr1PowerW + rechargePowerW;

			// 4. Apply ramp rate limiting if enabled
			if (this.config.rampEnabled()) {
				this.rampCycleCounter++;
				if (this.rampCycleCounter >= this.config.rampCycleInterval()) {
					this.rampCycleCounter = 0;

					var rampRate = this.config.rampRate();
					var powerDiff = targetPower - this.currentPower;

					if (Math.abs(powerDiff) > rampRate) {
						if (powerDiff > 0) {
							this.currentPower += rampRate;
						} else {
							this.currentPower -= rampRate;
						}
					} else {
						this.currentPower = targetPower;
					}
				}
			} else {
				this.currentPower = targetPower;
			}

			// 5. Apply power to ESS
			PowerConstraint.apply(this.ess, this.id(), //
					io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL, //
					Pwr.ACTIVE, Relationship.EQUALS, this.currentPower);

			// 6. Update monitoring channels
			this.channel(ControllerEssCsvFrequency.ChannelId.FREQUENCY).setNextValue(frequency);
			this.channel(ControllerEssCsvFrequency.ChannelId.FCR_POWER).setNextValue(fcrPowerW);
			this.channel(ControllerEssCsvFrequency.ChannelId.FFR1_POWER).setNextValue(ffr1PowerW);
			this.channel(ControllerEssCsvFrequency.ChannelId.RECHARGE_POWER).setNextValue(rechargePowerW);
			this.channel(ControllerEssCsvFrequency.ChannelId.CALCULATED_POWER).setNextValue(targetPower);
			this.channel(ControllerEssCsvFrequency.ChannelId.TOTAL_POWER).setNextValue(this.currentPower);

			// 6. Advance time counter
			this.elapsedSeconds++;

		} catch (Exception e) {
			this.log.error("Error in CSV Frequency controller: {}", e.getMessage(), e);
		}
	}
}
