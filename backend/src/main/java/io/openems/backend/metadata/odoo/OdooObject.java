package io.openems.backend.metadata.odoo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.odoojava.api.OdooApiException;
import com.odoojava.api.Row;

import io.openems.common.exceptions.OpenemsException;

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
	public Optional<Object> getOpt(String fieldName) {
		return Optional.ofNullable(this.row.get(fieldName));
	}

	protected void put(String fieldName, Object value) {
		try {
			this.row.put(fieldName, value);
		} catch (OdooApiException e) {
			log.warn("Unable to set Odoo-value: " + e.getMessage());
		}
		isChangedSinceLastWrite = true;
	}

	public void writeObject() throws OpenemsException {
		this.writeObject(true);
	}

	public void writeObject(boolean changesOnly) throws OpenemsException {
		long now = System.currentTimeMillis();
		try {
			if (isChangedSinceLastWrite && now - lastWrite > 60000) {
				// send max once per minute
				this.model.writeObject(this.row, changesOnly);
				this.lastWrite = now;
			}
		} catch (OdooApiException | XmlRpcException e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		} finally {
			isChangedSinceLastWrite = false;
		}
	}

	protected Date odooCompatibleNow() {
		Instant instant = Instant.now();
		int seconds = ZonedDateTime.of(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), ZoneId.systemDefault())
				.getOffset().getTotalSeconds();
		return Date.from(instant.minusSeconds(seconds));
	}
}
