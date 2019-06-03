package io.openems.common.access_control;

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

@Designate(ocd = ConfigLdap.class, factory = true)
@Component( //
        name = "common.AccessControlProvider.AccessControlProviderLdap", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AccessControlProviderLdap implements AccessControlProvider {

    private String path;

    @Activate
    void activate(ComponentContext componentContext, BundleContext bundleContext, ConfigLdap config) {
        this.path = path;
    }

    @Override
    public void initializeAccessControl(AccessControl accessControl) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + path);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system"); // specify the username
        env.put(Context.SECURITY_CREDENTIALS, "secret");           // specify the password
        try {
            DirContext ctx = new InitialDirContext(env);
            System.out.println(ctx);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}
