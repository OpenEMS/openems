package io.openems.common.accesscontrolproviderldap;

import io.openems.common.accesscontrol.AccessControlDataManager;
import io.openems.common.accesscontrol.AccessControlProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * This implementation of a {@link AccessControlProvider} can be configured used for connecting to a LDAP server which is already holding
 * the configuration of the roles, users and machines
 *
 * Does currently not work yet.
 *
 * @author Sebastian.Walbrun
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Access Control Provider LDAP", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AccessControlProviderLdap implements AccessControlProvider {

	private String path;
	private int priority;

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, Config config) {
		this.path = path;
		this.priority = config.priority();
	}

	@Override
	public void initializeAccessControl(AccessControlDataManager accessControlDataManager) {
		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://" + path);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system"); // specify the username
		env.put(Context.SECURITY_CREDENTIALS, "secret"); // specify the password
		try {
			DirContext ctx = new InitialDirContext(env);
			System.out.println(ctx);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int priority() {
		return this.priority;
	}
}
