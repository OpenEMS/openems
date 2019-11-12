package io.openems.edge.gaspedal;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp4728;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Gaspedal")
public class GaspedalImpl extends AbstractOpenemsComponent implements OpenemsComponent, Gaspedal {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	public I2cBridge refI2cBridge;

    private Mcp allocatedMcp;
    private I2CBus bus;
    private String address;
	private String id;
	private String i2cBridge;

    public GaspedalImpl() {
        super(OpenemsComponent.ChannelId.values());
    }


    private void allocateBus(int config) {
        try {

            switch (config) {

                case 0:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_0);
                    break;
                case 1:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_1);
                    break;
                case 2:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_2);
                    break;
                case 3:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_3);
                    break;
                case 4:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_4);
                    break;
                case 5:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_5);
                    break;
                case 6:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_6);
                    break;
                case 7:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_7);
                    break;
                case 8:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_8);
                    break;
                case 9:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_9);
                    break;
                case 10:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_10);
                    break;
                case 11:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_11);
                    break;
                case 12:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_12);
                    break;
                case 13:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_13);
                    break;
                case 14:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_14);
                    break;
                case 15:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_15);
                    break;
                case 16:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_16);
                    break;
                case 17:
                    this.bus = I2CFactory.getInstance(I2CBus.BUS_17);
                    break;

            }

        } catch (IOException | I2CFactory.UnsupportedBusNumberException e) {
            e.printStackTrace();
        }

    }

    private String name;

    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.id = config.id();
        this.i2cBridge = config.bridge();

        allocateBus(config.bus());

        switch (config.version()) {
            case "1":
				try {
					this.allocatedMcp = new Mcp4728(address, super.id(), this.bus);
					this.refI2cBridge.addMcp(this.allocatedMcp);
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}

		}
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

	@Override
	public String getId() {
		return super.id();
	}

	@Override
	public Mcp getMcp() {
		return this.allocatedMcp;
	}

    @Override
    public int calculateValueForDigit(int percentageRange, int ampereRange) {
            //TODO -Calc Values --


        return -1;
    }
}
