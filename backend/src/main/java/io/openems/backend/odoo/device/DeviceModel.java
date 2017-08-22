package io.openems.backend.odoo.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

	/**
	 * Gets the device for this apikey.
	 *
	 * Note: if there is more than one matching device it returns the first match.
	 *
	 * @param apikey
	 * @return device or null
	 * @throws OdooApiException
	 * @throws XmlRpcException
	 */
	public Optional<Device> getDeviceForApikey(String apikey) throws OdooApiException, XmlRpcException {
		List<Device> devices = this.readObjectsWhere("apikey", "=", apikey);
		if (devices.size() > 0) {
			// TODO Add devices to cache
			return Optional.of(devices.get(0));
		} else {
			return Optional.empty();
		}
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
