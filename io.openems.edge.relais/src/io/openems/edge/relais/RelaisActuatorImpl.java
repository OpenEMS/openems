package io.openems.edge.relais;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.relais.api.ActuatorRelaisChannel;
import io.openems.edge.relais.api.RelaisActuator;
import io.openems.edge.relais.board.RelaisBoard;
import io.openems.edge.relais.board.api.Mcp;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "ConsolinnoRelais",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class RelaisActuatorImpl extends AbstractOpenemsComponent implements ActuatorRelaisChannel, OpenemsComponent, RelaisActuator {

    private Mcp allocatedMcp;
    //private int position;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    public RelaisActuatorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ActuatorRelaisChannel.ChannelId.values());
    }

    private boolean relaisValue = false;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        //if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "I2Cregister", config.spiI2c_id())) {
        //return;
        //}
        allocateRelaisValue(config.relaisType());
        //        this.position = config.position();
        if (cpm.getComponent(config.relaisBoard_id()) instanceof RelaisBoard) {
            RelaisBoard relaisBoard = cpm.getComponent(config.relaisBoard_id());
            if (relaisBoard.getId().equals(config.relaisBoard_id())) {
                Mcp mcp = relaisBoard.getMcp();
                allocatedMcp = mcp;
                //Value if it's activated always true
                mcp.setPosition(config.position(), !this.relaisValue);
                //Value if it's deactivated Opener will be closed and Closer will be opened
                mcp.addToDefault(config.position(), !this.relaisValue);
                mcp.shift();
                mcp.addTask(config.id(), new RelaisActuatorTask(mcp, config.position(),
                        !this.relaisValue, this.getRelaisChannel(),
                        config.relaisBoard_id()));
            }
        }
    }


    private void allocateRelaisValue(String relaisType) {
        switch (relaisType) {

            case "Closer":
            case "Reverse":
                this.relaisValue = true;
                break;
            default:
                this.relaisValue = false;
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        allocatedMcp.removeTask(this.id());
    }

    @Override
    public String debugLog() {
        if (this.getRelaisChannelValue().getNextWriteValue().isPresent() && !this.getRelaisChannelValue().getNextWriteValue().equals(Optional.empty())) {
            Optional<Boolean> status = this.getRelaisChannelValue().getNextWriteValue();
            return "Status of " + super.id() + " alias: " + super.alias() + " will be " + this.getRelaisChannel().getNextWriteValue();
        } else {
            return "Status of " + super.id() + " alias " + super.alias() + " is " + this.getRelaisChannel().value().get();
        }
    }

    //For Controller
    @Override
    public WriteChannel<Boolean> getRelaisChannelValue() {
        return this.getRelaisChannel();
    }

    @Override
    public String getRelaisId() {
        return super.id();
    }

    @Override
    public boolean isCloser() {
        return this.relaisValue;
    }

}
