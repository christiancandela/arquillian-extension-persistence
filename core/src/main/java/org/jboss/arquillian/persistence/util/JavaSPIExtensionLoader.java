package org.jboss.arquillian.persistence.util;

import org.jboss.arquillian.core.spi.ExtensionLoader;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.core.spi.Validate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ServiceLoader implementation that use META-INF/services/interface files to registered Services.
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JavaSPIExtensionLoader implements ExtensionLoader {
    //-------------------------------------------------------------------------------------||
    // Class Members ----------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private static final String SERVICES = "META-INF/services";

    //-------------------------------------------------------------------------------------||
    // Required Implementations - ExtensionLoader -----------------------------------------||
    //-------------------------------------------------------------------------------------||

    @Override
    public Collection<LoadableExtension> load() {
        return all(JavaSPIExtensionLoader.class.getClassLoader(), LoadableExtension.class);
    }

    //-------------------------------------------------------------------------------------||
    // General JDK SPI Loader -------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    public <T> Collection<T> all(ClassLoader classLoader, Class<T> serviceClass) {
        Validate.notNull(classLoader, "ClassLoader must be provided");
        Validate.notNull(serviceClass, "ServiceClass must be provided");

        return createInstances(
                serviceClass,
                load(serviceClass, classLoader));
    }

    //-------------------------------------------------------------------------------------||
    // Internal Helper Methods - Service Loading ------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private <T> Set<Class<? extends T>> load(Class<T> serviceClass, ClassLoader loader) {
        String serviceFile = SERVICES + "/" + serviceClass.getName();

        LinkedHashSet<Class<? extends T>> providers = new LinkedHashSet<Class<? extends T>>();
        LinkedHashSet<Class<? extends T>> vetoedProviders = new LinkedHashSet<Class<? extends T>>();

        try {
            Enumeration<URL> enumeration = loader.getResources(serviceFile);
            while (enumeration.hasMoreElements()) {
                final URL url = enumeration.nextElement();
                final InputStream is = url.openStream();
                BufferedReader reader = null;

                try {
                    reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = reader.readLine();
                    while (null != line) {
                        line = skipCommentAndTrim(line);

                        if (line.length() > 0) {
                            try {
                                boolean mustBeVetoed = line.startsWith("!");
                                if (mustBeVetoed) {
                                    line = line.substring(1);
                                }

                                Class<? extends T> provider = loader.loadClass(line).asSubclass(serviceClass);

                                if (mustBeVetoed) {
                                    vetoedProviders.add(provider);
                                }

                                if (vetoedProviders.contains(provider)) {
                                    providers.remove(provider);
                                } else {
                                    providers.add(provider);
                                }
                            } catch (ClassCastException e) {
                                throw new IllegalStateException("Service " + line + " does not implement expected type "
                                        + serviceClass.getName());
                            }
                        }
                        line = reader.readLine();
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load services for " + serviceClass.getName(), e);
        }
        return providers;
    }

    private String skipCommentAndTrim(String line) {
        final int comment = line.indexOf('#');
        if (comment > -1) {
            line = line.substring(0, comment);
        }

        line = line.trim();
        return line;
    }

    private <T> Set<T> createInstances(Class<T> serviceType, Set<Class<? extends T>> providers) {
        Set<T> providerImpls = new LinkedHashSet<T>();
        for (Class<? extends T> serviceClass : providers) {
            providerImpls.add(createInstance(serviceClass));
        }
        return providerImpls;
    }

    /**
     * Create a new instance of the found Service. <br/>
     * <p>
     * Verifies that the found ServiceImpl implements Service.
     *
     * @param <T>
     * @param serviceType The Service interface
     * @param className   The name of the implementation class
     * @param loader      The ClassLoader to load the ServiceImpl from
     * @return A new instance of the ServiceImpl
     * @throws Exception If problems creating a new instance
     */
    private <T> T createInstance(Class<? extends T> serviceImplClass) {
        try {
            return SecurityActions.newInstance(serviceImplClass, new Class<?>[0], new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException("Could not create a new instance of Service implementation " + serviceImplClass.getName(), e);
        }
    }
}