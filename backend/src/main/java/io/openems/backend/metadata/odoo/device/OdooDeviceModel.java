package io.openems.backend.metadata.odoo.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.xmlrpc.XmlRpcException;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
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
	public Optional<MetadataDevice> getDeviceForApikey(String apikey) throws OpenemsException {
		List<OdooDevice> devices;
		try {
			devices = this.readObjectsWhere("apikey", "=", apikey);
		} catch (XmlRpcException | OdooApiException e) {
			throw new OpenemsException("Unable to find device for apikey: " + e.getMessage());
		}
		if (devices.size() > 0) {
			return Optional.of(devices.get(0));
		} else {
			return Optional.empty();
		}
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
