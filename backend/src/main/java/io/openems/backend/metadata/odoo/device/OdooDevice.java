package io.openems.backend.metadata.odoo.device;

import com.abercap.odoo.Row;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.metadata.odoo.OdooModel;
import io.openems.backend.metadata.odoo.OdooObject;

public class OdooDevice extends OdooObject implements MetadataDevice {
	public OdooDevice(OdooModel<?> model, Row row) {
		super(model, row);
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getId()
	 */
	@Override
	public Integer getId() {
		return (Integer) get(Field.ID);
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getNameNumber()
	 */
	@Override
	public String getNameNumber() {
		return getOr(Field.NAME_NUMBER, "").toString();
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getName()
	 */
	@Override
	public String getName() {
		return getOr(Field.NAME, "UNKNOWN").toString();
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getComment()
	 */
	@Override
	public String getComment() {
		return getOr(Field.COMMENT, "").toString();
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getState()
	 */
	@Override
	public String getState() {
		return getOr(Field.STATE, "").toString();
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getProductType()
	 */
	@Override
	public String getProductType() {
		return getOr(Field.PRODUCT_TYPE, "").toString();
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#getOpenemsConfig()
	 */
	@Override
	public JsonObject getOpenemsConfig() {
		Object config = get(Field.OPENEMS_CONFIG);
		if (config != null) {
			return (new JsonParser()).parse(get(Field.OPENEMS_CONFIG).toString()).getAsJsonObject();
		} else {
			return new JsonObject();
		}
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setOpenemsConfig(com.google.gson.JsonObject)
	 */
	@Override
	public void setOpenemsConfig(JsonObject j) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		put(Field.OPENEMS_CONFIG, gson.toJson(j));
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setState(java.lang.String)
	 */
	@Override
	public void setState(String active) {
		put(Field.STATE, active);
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setSoc(int)
	 */
	@Override
	public void setSoc(int value) {
		put(Field.SOC, value);
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setLastMessage()
	 */
	@Override
	public void setLastMessage() {
		put(Field.LASTMESSAGE, this.odooCompatibleNow());
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setLastUpdate()
	 */
	@Override
	public void setLastUpdate() {
		put(Field.LASTUPDATE, this.odooCompatibleNow());
	}

	/* (non-Javadoc)
	 * @see io.openems.backend.metadata.odoo.device.Device#setIpV4(java.lang.String)
	 */
	@Override
	public void setIpV4(String value) {
		put(Field.IPV4, value);
	}
}
