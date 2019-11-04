package io.openems.edge.relaisBoard;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.relaisBoard.api.Mcp;
import io.openems.edge.relaisBoard.api.Mcp23008;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;;
import java.util.Map;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Relais Board",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		immediate = true)
public class RelaisBoardImpl extends AbstractOpenemsComponent implements OpenemsComponent {

	private String id;
	private String alias;
	private String VersionNumber;
	private I2CBus bus;
	private short address;
	private String i2cBridge;
	private Mcp mcp;

	public RelaisBoardImpl() {
		super(OpenemsComponent.ChannelId.values());
	}

	@Activate
	void activate(Config config) {
		this.id = config.id();
		this.alias = config.alias();
		this.VersionNumber = config.version();
		this.address = config.address();
		this.i2cBridge = config.bridge();
		allocateBus(config.bus());
		try {
			switch (config.version()) {
				case "1":
					this.mcp = new Mcp23008(address, bus);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
		if (mcp instanceof Mcp23008) {
			for (Map.Entry<Integer, Boolean> entry : ((Mcp23008) mcp).getValuesPerDefault().entrySet()) {
				((Mcp23008) mcp).setPosition(entry.getKey(), entry.getValue());

			}
		}
	}

	private void allocateBus(int bus) {
		try {

			switch (bus) {

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
			}

		} catch (IOException | I2CFactory.UnsupportedBusNumberException e) {
			e.printStackTrace();
		}

	}

	public String getId() {
		return id;
	}

	public Mcp getMcp() {
		return this.mcp;
	}
}
