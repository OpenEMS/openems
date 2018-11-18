package io.openems.edge.controller.api.core;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;

public interface ApiController extends Controller {

	Timedata getTimedata();

	List<OpenemsComponent> getComponents();

	ConfigurationAdmin getConfigurationAdmin();

}
