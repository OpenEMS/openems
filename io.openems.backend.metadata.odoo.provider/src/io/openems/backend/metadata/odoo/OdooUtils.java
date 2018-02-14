package io.openems.backend.metadata.odoo;

import java.net.URL;
import java.util.HashMap;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import io.openems.common.exceptions.OpenemsException;

public class OdooUtils {

	private OdooUtils() {
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
		final XmlRpcClient client = new XmlRpcClient();
		try {
			// Build XML request to Odoo
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setEnabledForExtensions(true);
			config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
			client.setConfig(config);
			// Add domain filter
			Object[] domain = new Object[domains.length];
			for (int i = 0; i < domains.length; i++) {
				Domain filter = domains[i];
				domain[i] = new Object[] { filter.field, filter.operator, filter.value };
			}
			Object[] paramsDomain = new Object[] { domain };
			HashMap<Object, Object> paramsRules = new HashMap<Object, Object>();
			String action = "search";
			Object[] params = new Object[] { database, uid, password, model, action, paramsDomain, paramsRules };
			// Execute XML request
			Object[] resultObjs = (Object[]) client.execute("execute_kw", params);
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
}
