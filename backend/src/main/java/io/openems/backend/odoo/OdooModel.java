package io.openems.backend.odoo;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.abercap.odoo.FilterCollection;
import com.abercap.odoo.ObjectAdapter;
import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.Row;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

public abstract class OdooModel<T extends OdooObject> {
	private final ObjectAdapter oa;

	public OdooModel(Session session) throws XmlRpcException, OdooApiException {
		oa = session.getObjectAdapter(getObjectName());
	}
	
	protected void writeObject(Row row, boolean changesOnly) throws OdooApiException, XmlRpcException {
		oa.writeObject(row, changesOnly);
	}
	
	protected RowCollection _searchAndReadObject(FilterCollection filters) throws XmlRpcException, OdooApiException {
		return oa.searchAndReadObject(filters, getFields());	
	}
	
	public abstract List<T> searchAndReadObject(FilterCollection filters) throws XmlRpcException, OdooApiException;
	
	public List<T> searchAndReadObject(String fieldName, String comparison, Object value) throws XmlRpcException, OdooApiException {
		FilterCollection filters = new FilterCollection();
		filters.add(fieldName, comparison, value);
		return searchAndReadObject(filters);
	}
	
	protected abstract String getObjectName();
	
	protected abstract String[] getFields();
}
