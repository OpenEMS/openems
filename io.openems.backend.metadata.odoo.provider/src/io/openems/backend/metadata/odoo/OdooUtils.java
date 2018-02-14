package io.openems.backend.metadata.odoo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import io.openems.common.exceptions.OpenemsException;

public class OdooUtils {

	private OdooUtils() {
	}

	private static Object[] executeKw(String url, Object[] params) throws XmlRpcException, MalformedURLException {
		final XmlRpcClient client = new XmlRpcClient();
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setEnabledForExtensions(true);
		config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
		config.setReplyTimeout(5000);
		client.setConfig(config);
		return (Object[]) client.execute("execute_kw", params);
	}

	/**
	 * Executes a search on Odoo
	 * 
	 * @param url
	 *            URL of Odoo instance
	 * @param database
	 *            Database name
	 * @param uid
	 *            UID of user (e.g. '1' for admin)
	 * @param password
	 *            Password of user
	 * @param model
	 *            Odoo model to query (e.g. 'res.partner')
	 * @param domains
	 *            Odoo domain filters
	 * @return Odoo object ids
	 * @throws OpenemsException
	 */
	protected static int[] search(String url, String database, int uid, String password, String model,
			Domain... domains) throws OpenemsException {
		// Add domain filter
		Object[] domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = new Object[] { domain };
		// Create request params
		HashMap<Object, Object> paramsRules = new HashMap<Object, Object>();
		String action = "search";
		Object[] params = new Object[] { database, uid, password, model, action, paramsDomain, paramsRules };
		try {
			// Execute XML request
			Object[] resultObjs = executeKw(url, params);
			// Parse results
			int[] results = new int[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				results[i] = (int) resultObjs[i];
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads a record from Odoo
	 * 
	 * @param url
	 *            URL of Odoo instance
	 * @param database
	 *            Database name
	 * @param uid
	 *            UID of user (e.g. '1' for admin)
	 * @param password
	 *            Password of user
	 * @param model
	 *            Odoo model to query (e.g. 'res.partner')
	 * @param ids
	 *            ids of model to read
	 * @param fields
	 *            fields that should be read
	 * @return
	 * @throws OpenemsException
	 */
	protected static Map<String, Object> readOne(String url, String database, int uid, String password, String model,
			int id, Fields... fields) throws OpenemsException {
		// Create request params
		String action = "read";
		// Add ids
		Object[] paramsIds = new Object[1];
		paramsIds[0] = id;
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].n();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = new Object[] { database, uid, password, model, action, paramsIds, paramsFields };
		try {
			// Execute XML request
			Object[] resultObjs = executeKw(url, params);
			// Parse results
			for (int i = 0; i < resultObjs.length;) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) resultObjs[i];
				return result;
			}
			throw new OpenemsException("No matching entry found for id [" + id + "]");
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads multiple records from Odoo
	 * 
	 * @param url
	 *            URL of Odoo instance
	 * @param database
	 *            Database name
	 * @param uid
	 *            UID of user (e.g. '1' for admin)
	 * @param password
	 *            Password of user
	 * @param model
	 *            Odoo model to query (e.g. 'res.partner')
	 * @param ids
	 *            ids of model to read
	 * @param fields
	 *            fields that should be read
	 * @return
	 * @throws OpenemsException
	 */
	protected static Map<String, Object>[] readMany(String url, String database, int uid, String password, String model,
			int[] ids, Fields... fields) throws OpenemsException {
		// Create request params
		String action = "read";
		// Add ids
		Object[] paramsIds = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) {
			paramsIds[i] = ids[i];
		}
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].toString();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = new Object[] { database, uid, password, model, action, paramsIds, paramsFields };
		try {
			// Execute XML request
			Object[] resultObjs = executeKw(url, params);
			// Parse results
			@SuppressWarnings("unchecked")
			Map<String, Object>[] results = (Map<String, Object>[]) new Map[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) resultObjs[i];
				results[0] = result;
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search from Odoo: " + e.getMessage());
		}
	}
}
