package io.openems.backend.odoo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.Row;

/**
 * Represents a record object in Odoo
 *
 * @author stefan.feilmeier
 *
 */
public abstract class OdooObject {
	private final Logger log = LoggerFactory.getLogger(OdooObject.class);
	private Row row;
	private OdooModel<?> model;
	private boolean isChangedSinceLastWrite = false;
	private long lastWrite = 0;

	public OdooObject(OdooModel<?> model, Row row) {
		this.model = model;
		this.row = row;
	}

	public void refreshFrom(OdooObject o) {
		this.row = o.row;
		this.model = o.model;
	}

	/**
	 * Gets the value of the given field
	 *
	 * @param fieldName
	 * @return
	 */
	public Object get(String fieldName) {
		return this.row.get(fieldName);
	}

	/**
	 * Gets the value of the given field or a defaultValue (to avoid null)
	 *
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public Object getOr(String fieldName, Object defaultValue) {
		Object value = this.row.get(fieldName);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	public void put(String fieldName, Object value) {
		try {
			this.row.put(fieldName, value);
		} catch (OdooApiException e) {
			log.warn("Unable to set Odoo-value: " + e.getMessage());
		}
		isChangedSinceLastWrite = true;
	}

	public void writeObject() throws OdooApiException, XmlRpcException {
		this.writeObject(true);
	}

	public void writeObject(boolean changesOnly) throws OdooApiException, XmlRpcException {
		long now = System.currentTimeMillis();
		try {
			if (isChangedSinceLastWrite && now - lastWrite > 60000) {
				// send max once per minute
				this.model.writeObject(this.row, changesOnly);
				this.lastWrite = now;
				log.info("Updated Odoo record");
			}
		} finally {
			isChangedSinceLastWrite = false;
		}
	}

	public Date odooCompatibleNow() {
		Instant instant = Instant.now();
		int seconds = ZonedDateTime.of(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), ZoneId.systemDefault())
				.getOffset().getTotalSeconds();
		return Date.from(instant.minusSeconds(seconds));
	}
}
