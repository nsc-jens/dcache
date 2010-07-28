package org.dcache.boot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dcache.util.DeprecatableProperties;
import org.dcache.util.NetworkUtils;
import org.dcache.util.ReplaceableProperties;

import dmg.cells.nucleus.CellShell;
import dmg.cells.nucleus.SystemCell;
import dmg.util.Args;
import dmg.util.CommandException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Domain encapsulates the configuration of a domain and its
 * services. Provides the logic for starting a domain.
 */
public class Domain
{
    private static final String PROPERTY_DOMAIN_NAME = "domain.name";
    private static final String PROPERTY_DOMAIN_SERVICE = "domain.service";
    private static final String PROPERTY_DOMAIN_SERVICE_URI = "domain.service.uri";
    private static final String PROPERTY_DOMAIN_PRELOAD = "domain.preload";

    private static final String PROPERTY_LOG_CONFIG = "dcache.log.configuration";

    private static final String SCHEME_FILE = "file";
    private static final String SUFFIX_PROPERTIES = ".properties";
    private static final String SUFFIX_XML = ".xml";

    private static final Logger _log =
        LoggerFactory.getLogger(SystemCell.class);

    private final ReplaceableProperties _properties;
    private final List<ReplaceableProperties> _services;

    public Domain(String name, ReplaceableProperties defaults)
    {
        _properties = new DeprecatableProperties(defaults);
        _properties.put(PROPERTY_DOMAIN_NAME, name);
        _services = new ArrayList<ReplaceableProperties>();
    }

    public ReplaceableProperties properties()
    {
        return _properties;
    }

    public ReplaceableProperties createService(String name)
    {
        ReplaceableProperties service =
            new DeprecatableProperties(_properties);
        service.put(PROPERTY_DOMAIN_SERVICE, name);
        _services.add(service);
        return service;
    }

    public String getName()
    {
        return _properties.getReplacement(PROPERTY_DOMAIN_NAME);
    }

    List<ReplaceableProperties> getServices()
    {
        return _services;
    }

    public void start()
        throws URISyntaxException, CommandException, IOException
    {
        initializeLogging();

        String domainName = getName();
        SystemCell systemCell = new SystemCell(domainName);
        _log.info("Starting " + domainName);

        importUnscopedParameters(systemCell, _properties);
        executePreload(systemCell);
        for (ReplaceableProperties serviceConfig: _services) {
            executeService(systemCell, serviceConfig);
        }

        if (_services.isEmpty()) {
            _log.warn("No services found. Domain appears to be empty.");
        }
    }

    private void initializeLogging()
        throws URISyntaxException, IOException
    {
        try {
            String property = _properties.getReplacement(PROPERTY_LOG_CONFIG);
            if (property == null) return;

            URI uri = new URI(property);
            String path = uri.getPath();
            if (path == null) {
                throw new URISyntaxException(property, "Path is missing");
            }

            if (uri.getScheme() == null || uri.getScheme().equals(SCHEME_FILE)) {
                File f = new File(path);
                uri = f.toURI();
            }

            LoggerContext loggerContext =
                (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                configurator.doConfigure(NetworkUtils.toURL(uri));
            } finally {
                StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
            }
        } catch (JoranException e) {
            throw new IOException("Failed to load log configuration:" + e.getMessage(), e);
        }
    }

    /**
     * Returns whether a name is scoped.
     *
     * A scoped name begins with the name of the scope followed by the
     * scoping operator, a forward slash.
     */
    private boolean isScoped(String name)
    {
        return name.indexOf('/') > -1;
    }

    /**
     * Returns whether a name has a particular scope.
     */
    private boolean isScoped(String scope, String name)
    {
        return scope.length() < name.length() &&
            name.startsWith(scope) && name.charAt(scope.length()) == '/';
    }

    /**
     * Returns the unscoped name.
     */
    private String stripScope(String name)
    {
        int pos = name.indexOf('/');
        return (pos == -1) ? name : name.substring(pos + 1);
    }

    /**
     * Imports unscoped parameters into a SystemCell.
     */
    private void importUnscopedParameters(SystemCell cell,
                                          ReplaceableProperties properties)
    {
        Map<String,Object> domainContext = cell.getDomainContext();
        for (String key: properties.stringPropertyNames()) {
            if (!isScoped(key)) {
                domainContext.put(key, properties.getReplacement(key));
            }
        }
    }

    /**
     * Imports service scoped parameters into a CellShell.
     */
    private void importScopedParameters(CellShell shell,
                                        ReplaceableProperties properties)
        throws CommandException
    {
        Map<String,Object> environment = shell.environment();
        String service = properties.getReplacement(PROPERTY_DOMAIN_SERVICE);
        for (String key: properties.stringPropertyNames()) {
            if (isScoped(service, key)) {
                environment.put(stripScope(key), properties.getReplacement(key));
            }
        }
    }

    /**
     * Imports service local parameters into a CellShell.
     */
    private void importLocalParameters(CellShell shell,
                                       ReplaceableProperties properties)
        throws CommandException
    {
        Map<String,Object> environment = shell.environment();
        for (Object o: properties.keySet()) {
            String key = (String) o;
            if (!isScoped(key)) {
                environment.put(key, properties.getReplacement(key));
            }
        }
    }

    /**
     * Executes a preload batch script, if defined.
     */
    private void executePreload(SystemCell cell)
        throws URISyntaxException, IOException, CommandException
    {
        String preload = _properties.getReplacement(PROPERTY_DOMAIN_PRELOAD);
        if (preload != null) {
            CellShell shell = new CellShell(cell.getNucleus());
            executeBatchFile(shell, new URI(preload));
        }
    }

    /**
     * Executes the batch file of the service.
     */
    private void executeService(SystemCell cell, ReplaceableProperties service)
        throws URISyntaxException, IOException, CommandException
    {
        /* The per service configuration is loaded into the
         * environment of the CellShell used to execute the batch
         * file.
         */
        CellShell shell = new CellShell(cell.getNucleus());
        importScopedParameters(shell, service);
        importLocalParameters(shell, service);

        URI uri = new URI(service.getReplacement(PROPERTY_DOMAIN_SERVICE_URI));
        executeBatchFile(shell, uri);
    }

    /**
     * Executes the batch file in the resource.
     */
    private void executeBatchFile(CellShell shell, URI resource)
        throws URISyntaxException, IOException, CommandException
    {
        InputStream input = NetworkUtils.toURL(resource).openStream();
        try {
            shell.execute(resource.toString(), new InputStreamReader(input),
                          new Args(""));
        } finally {
            input.close();
        }
    }
}
