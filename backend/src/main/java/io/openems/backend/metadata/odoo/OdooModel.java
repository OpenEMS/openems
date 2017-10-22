package io.openems.backend.metadata.odoo;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.odoojava.api.FilterCollection;
import com.odoojava.api.ObjectAdapter;
import com.odoojava.api.OdooApiException;
import com.odoojava.api.Row;
import com.odoojava.api.RowCollection;
import com.odoojava.api.Session;

/**
 * Represents an abstract model in Odoo object relational mapper
 *
 * @author stefan.feilmeier
 *
 * @param <T>
 */
public abstract class OdooModel<T extends OdooObject> {
	private final ObjectAdapter oa;

	/**
	 * Initializes the model with a Odoo session
	 *
	 * @param session
	 * @throws XmlRpcException
	 * @throws OdooApiException
	 */
	public OdooModel(Session session) throws XmlRpcException, OdooApiException {
		oa = session.getObjectAdapter(getModelId());
	}

	/**
	 * Reads all objects of this model
	 *
	 * @return
	 * @throws XmlRpcException
	 * @throws OdooApiException
	 */
	public List<T> readAllObjects() throws XmlRpcException, OdooApiException {
		FilterCollection filter = new FilterCollection();
		RowCollection rows = oa.searchAndReadObject(filter, getFields());
		return convertRowCollectionToList(rows);
	}

	/**
	 * Reads all objects of this model
	 *
	 * @return
	 * @throws XmlRpcException
	 * @throws OdooApiException
	 */
	public List<T> readObjectsWhere(String fieldName, String comparison, Object value)
			throws XmlRpcException, OdooApiException {
		FilterCollection filter = new FilterCollection();
		filter.add(fieldName, comparison, value);
		RowCollection rows = oa.searchAndReadObject(filter, getFields());
		return convertRowCollectionToList(rows);
	}

	/**
	 * Converts a RowCollection to a list of POJOs
	 *
	 * @param rows
	 * @return
	 */
	protected abstract List<T> convertRowCollectionToList(RowCollection rows);

	protected void writeObject(Row row, boolean changesOnly) throws OdooApiException, XmlRpcException {
		oa.writeObject(row, changesOnly);
	}

	protected abstract String getModelId();

	protected abstract String[] getFields();
}
