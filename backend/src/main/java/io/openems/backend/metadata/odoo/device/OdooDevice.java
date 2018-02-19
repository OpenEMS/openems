package io.openems.backend.metadata.odoo.device;

import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.odoojava.api.Row;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.metadata.odoo.OdooModel;
import io.openems.backend.metadata.odoo.OdooObject;

public class OdooDevice extends OdooObject implements MetadataDevice {
	public OdooDevice(OdooModel<?> model, Row row) {
		super(model, row);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getNameNumber()
	 */
	@Override
	public Optional<Integer> getIdOpt() {
		Optional<Object> objOpt = this.getOpt(Field.NAME_NUMBER);
		if (objOpt.isPresent()) {
			return Optional.of((Integer) objOpt.get());
		} else {
			return Optional.empty();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getName()
	 */
	@Override
	public String getName() {
		return this.getOpt(Field.NAME).orElse("UNKOWN").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getComment()
	 */
	@Override
	public String getComment() {
		return this.getOpt(Field.COMMENT).orElse("UNKOWN").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getState()
	 */
	@Override
	public String getState() {
		return this.getOpt(Field.STATE).orElse("UNKOWN").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getProductType()
	 */
	@Override
	public String getProductType() {
		return this.getOpt(Field.PRODUCT_TYPE).orElse("UNKOWN").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#getOpenemsConfig()
	 */
	@Override
	public JsonObject getOpenemsConfig() {
		Optional<Object> objOpt = this.getOpt(Field.OPENEMS_CONFIG);
		if (objOpt.isPresent()) {
			return new JsonParser().parse(objOpt.get().toString()).getAsJsonObject();
		} else {
			return new JsonObject();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setOpenemsConfig(com.google.gson.JsonObject)
	 */
	@Override
	public void setOpenemsConfig(JsonObject j) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		put(Field.OPENEMS_CONFIG, gson.toJson(j));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setState(java.lang.String)
	 */
	@Override
	public void setState(String active) {
		put(Field.STATE, active);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setSoc(int)
	 */
	@Override
	public void setSoc(int value) {
		put(Field.SOC, value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setLastMessage()
	 */
	@Override
	public void setLastMessage() {
		put(Field.LASTMESSAGE, this.odooCompatibleNow());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setLastUpdate()
	 */
	@Override
	public void setLastUpdate() {
		put(Field.LASTUPDATE, this.odooCompatibleNow());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.openems.backend.metadata.odoo.device.Device#setIpV4(java.lang.String)
	 */
	@Override
	public void setIpV4(String value) {
		put(Field.IPV4, value);
	}
}
