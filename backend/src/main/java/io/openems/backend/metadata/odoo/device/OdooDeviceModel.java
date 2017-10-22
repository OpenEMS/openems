package io.openems.backend.metadata.odoo.device;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.odoojava.api.OdooApiException;
import com.odoojava.api.RowCollection;
import com.odoojava.api.Session;

import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.backend.metadata.odoo.OdooModel;
import io.openems.common.exceptions.OpenemsException;

public class OdooDeviceModel extends OdooModel<OdooDevice> implements MetadataDeviceModel {

	public OdooDeviceModel(Session session) throws XmlRpcException, OdooApiException {
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
	public MetadataDevices getDevicesForApikey(String apikey) throws OpenemsException {
		MetadataDevices devices = new MetadataDevices();
		try {
			for (OdooDevice device : this.readObjectsWhere("apikey", "=", apikey)) {
				devices.add(device);
			}
		} catch (XmlRpcException | OdooApiException e) {
			throw new OpenemsException("Unable to find device for apikey: " + e.getMessage());
		}
		return devices;
	}

	@Override
	protected List<OdooDevice> convertRowCollectionToList(RowCollection rows) {
		List<OdooDevice> result = new ArrayList<>();
		rows.forEach(row -> {
			result.add(new OdooDevice(this, row));
		});
		return result;
	}
}
