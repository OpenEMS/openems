package market;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(name = "market", immediate = true)
public class ProviderImpl /* implements SomeApi */ {

	@Activate
	void activate() {
		System.out.println("activate market");
	}

	@Deactivate
	void deactivate() {
		System.out.println("deactivate market");
	}

}