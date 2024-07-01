package io.openems.edge.controller.ess.timeframe;

import io.openems.common.exceptions.InvalidValueException;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Controller.Ess.Timeframe", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssTimeframeImpl extends AbstractOpenemsComponent
        implements ControllerEssTimeframe, Controller, OpenemsComponent, TimedataProvider {

    private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
            ControllerEssTimeframe.ChannelId.CUMULATED_ACTIVE_TIME);

    @Reference
    private ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private ManagedSymmetricEss ess;

    private Config config;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile Timedata timedata = null;


    private final Logger log = LoggerFactory.getLogger(io.openems.edge.controller.ess.timeframe.ControllerEssTimeframeImpl.class);

    public ControllerEssTimeframeImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                Controller.ChannelId.values(), //
                ControllerEssTimeframe.ChannelId.values() //
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
        return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
        var isActive = false;
        try {
            isActive = switch (this.config.mode()) {
                case MANUAL -> {
                    // Apply Active-Power Set-Point
                    var acPower = getAcPower(this.ess, this.config.fallback_ess_capacity(), this.config.targetSoC(), this.config.startTime(), this.config.endTime());

                    if (acPower == null) {
                        yield false; // is not active
                    } else {
                        PowerConstraint.apply(this.ess, this.id(), //
                                this.config.phase(), Pwr.ACTIVE, this.config.relationship(), acPower);
                        yield true; // is active
                    }
                }
                case OFF -> {
                    // Do nothing
                    yield false; // is not active
                }
            };

        } finally {
            this.calculateCumulatedActiveTime.update(isActive);
        }
    }

    /**
     * Gets the required AC power set-point for AC-ESS.
     *
     * @param ess       the {@link ManagedSymmetricEss}
     * @param fallback_ess_capacity capacity of the ess, used as fallback if automatic determination does not work
     * @param targetSoC the target SoC
     * @param startTime the start time of the timeframe
     * @param endTime   the end time of the timeframe
     * @return the AC power set-point
     */
    protected static Integer getAcPower(ManagedSymmetricEss ess, Integer fallback_ess_capacity, int targetSoC, String startTime, String endTime) throws InvalidValueException {
        Date start = getDateFromIsoString(startTime);
        Date end = getDateFromIsoString(endTime);
        Date current = new Date();
        if (current.after(start) && current.before(end)) {
            int currentSoC = ess.getSoc().getOrError();
            Integer maxEssCapacity = ess.getCapacity().orElse(fallback_ess_capacity);

            if (maxEssCapacity == null || maxEssCapacity <= 0) {
                throw new InvalidValueException("could not determine ESS capacity and no valid fallback capacity was configured.");
            }

            int targetCapacity = maxEssCapacity * targetSoC / 100;
            int currentCapacity = maxEssCapacity * currentSoC / 100;

            float timeframeInHours = (end.getTime() - current.getTime()) / 1000f / 60f / 60f;

           return Math.round((currentCapacity - targetCapacity) / timeframeInHours);
        }

        return null;
    }

    private static Date getDateFromIsoString(String iso8601String) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(iso8601String, timeFormatter);

        return Date.from(Instant.from(offsetDateTime));
    }

    @Override
    public Timedata getTimedata() {
        return this.timedata;
    }
}