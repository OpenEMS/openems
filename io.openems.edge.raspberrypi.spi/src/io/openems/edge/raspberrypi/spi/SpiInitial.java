package io.openems.edge.raspberrypi.spi;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;


@Designate( ocd= SpiInitial.Config.class, factory=true)
@Component(name="io.openems.edge.raspberrypi.spi")
public class SpiInitial /* implements SomeApi */ {

	@ObjectClassDefinition
	@interface Config {
		String name() default "World";
	}

	private String name;

	@Activate

	//TODO SPI Wiring Pi Setup; Do SPI Worker --> for every Channel
	//TODO handle Event
	void activate(Config config) {
		this.name = config.name();
	}

	@Deactivate
	void deactivate() {
	}

}
