package io.openems.edge.evcs.ocpp.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.OcppEvcs;
import io.openems.edge.evcs.api.Status;

public abstract class AbstractOcppEvcsComponent extends AbstractOpenemsComponent
        implements Evcs, OcppEvcs, EventHandler {

    private final Set<OcppProfileType> profileTypes;

    private List<OcppEvcs.ChannelId> ignoreResetChannels = Arrays.asList(OcppEvcs.ChannelId.CHARGING_SESSION_ID,
            OcppEvcs.ChannelId.CONNECTOR_ID, OcppEvcs.ChannelId.OCPP_ID);

    protected AbstractOcppEvcsComponent(OcppProfileType[] profileTypes,
            io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
            io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);

        this.profileTypes = new HashSet<OcppProfileType>(Arrays.asList(profileTypes));
    }

    @Override
    protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
        super.activate(context, id, alias, enabled);

        this.channel(OcppEvcs.ChannelId.OCPP_ID).setNextValue(getConfiguredOcppId());
        this.channel(OcppEvcs.ChannelId.CONNECTOR_ID).setNextValue(getConfiguredConnectorId());
        this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(getConfiguredMaximumHardwarePower());
        this.channel(Evcs.ChannelId.MINIMUM_POWER).setNextValue(getConfiguredMinimumHardwarePower());
        this.getEnergySession().setNextValue(0);
    }

    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
        case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

            if (this.getChargingSessionId().value().orElse("").isEmpty()) {
                this.getChargingstationCommunicationFailed().setNextValue(true);
            } else {
                this.getChargingstationCommunicationFailed().setNextValue(false);
            }
            if (this.status().getNextValue().asEnum().equals(Status.CHARGING_FINISHED)) {
                this.resetChannelValues();
            }
            
            // TODO: Ask a WriteHandler to set Values for the profiles
            for (OcppProfileType ocppProfileType : profileTypes) {
                switch (ocppProfileType) {
                case CORE:
                    break;
                case FIRMWARE_MANAGEMENT:
                    break;
                case LOCAL_AUTH_LIST_MANAGEMENT:
                    break;
                case REMOTE_TRIGGER:
                    break;
                case RESERVATION:
                    break;
                case SMART_CHARGING:
                    break;
                }
            }
            break;
        }
    }
    
    @Override
    protected void deactivate() {
        super.deactivate();
    }

    public abstract Set<OcppInformations> getSupportedMeasurements();

    public abstract String getConfiguredOcppId();

    public abstract Integer getConfiguredConnectorId();
    
    public abstract Integer getConfiguredMaximumHardwarePower();
    
    public abstract Integer getConfiguredMinimumHardwarePower();
    
    private void resetChannelValues() {
        for (OcppEvcs.ChannelId c : OcppEvcs.ChannelId.values()) {
            if(!ignoreResetChannels.contains(c)) {
                
            Channel<?> channel = this.channel(c);
            channel.setNextValue(null);
            }
            this.getChargePower().setNextValue(0);
        }
    }
}
