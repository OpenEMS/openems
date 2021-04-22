package io.openems.edge.predictor.solcast;

import java.lang.annotation.Annotation;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public MyConfig(Class<? extends Annotation> annotation, String id) {
		super(annotation, id);
		// TODO Auto-generated constructor stub
	}



	@Override
	public String id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String key() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resource_id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean limitedAPI() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String starttime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String endtime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] channelAddresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean debug() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String debug_file() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String alias() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean enabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		// TODO Auto-generated method stub
		return null;
	}

}
