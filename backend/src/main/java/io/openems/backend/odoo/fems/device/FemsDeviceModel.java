package io.openems.backend.odoo.fems.device;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.abercap.odoo.FilterCollection;
import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

import io.openems.backend.odoo.OdooModel;

public class FemsDeviceModel extends OdooModel<FemsDevice> {

	public FemsDeviceModel(Session session) throws XmlRpcException, OdooApiException {
		super(session);
	}
	
	protected String getObjectName() {
		return "fems.device";
	}

	@Override
	public List<FemsDevice> searchAndReadObject(FilterCollection filters) throws XmlRpcException, OdooApiException {
		RowCollection rows = super._searchAndReadObject(filters);
		List<FemsDevice> result = new ArrayList<>();
		rows.forEach(row -> {
			result.add(new FemsDevice(this, row));
		});
		return result;
	}

	@Override
	protected String[] getFields() {
		return FemsDevice.getFields();
	}
}
