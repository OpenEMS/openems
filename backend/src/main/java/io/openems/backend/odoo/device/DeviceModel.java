package io.openems.backend.odoo.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.xmlrpc.XmlRpcException;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

import io.openems.backend.odoo.OdooModel;
import io.openems.common.exceptions.OpenemsException;

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
	 * @throws OpenemsException
	 */
	public Optional<Device> getDeviceForApikey(String apikey) throws OpenemsException {
		List<Device> devices;
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
	protected List<Device> convertRowCollectionToList(RowCollection rows) {
		List<Device> result = new ArrayList<>();
		rows.forEach(row -> {
			result.add(new Device(this, row));
		});
		return result;
	}
}
