package io.openems.common.accesscontrol;

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

// FIXME: this should be in a separate bundle. Also I don't like the Component-Name too much - it's quite redundant
@Designate(ocd = ConfigLdap.class, factory = true)
@Component(//
		name = "common.AccessControlProvider.AccessControlProviderLdap", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AccessControlProviderLdap implements AccessControlProvider {

	private String path;
	private int priority;

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, ConfigLdap config) {
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
