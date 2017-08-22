package io.openems.backend.odoo.device;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

import io.openems.backend.odoo.OdooModel;

public class DeviceModel extends OdooModel<Device> {

	public DeviceModel(Session session) throws XmlRpcException, OdooApiException {
		super(session);
	}

	@Override
	protected String getModelId() {
		return "fems.device";
	}

	@Override
	protected String[] getFields() {
		return new String[] { Field.NAME, Field.NAME_NUMBER, Field.COMMENT, Field.SOC, Field.LASTMESSAGE,
				Field.LASTUPDATE, Field.IPV4, Field.OPENEMS_CONFIG, Field.STATE, Field.PRODUCT_TYPE };
	}

	@Override
	protected List<Device> convertRowCollectionToList(RowCollection rows) {
		List<Device> result = new ArrayList<>();
		rows.forEach(row -> {
			result.add(new Device(this, row));
		});
		return result;
	}
}
