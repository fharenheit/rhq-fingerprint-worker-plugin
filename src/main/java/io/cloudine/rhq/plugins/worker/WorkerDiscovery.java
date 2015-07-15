package io.cloudine.rhq.plugins.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.pluginapi.inventory.*;
import org.rhq.core.system.ProcessInfo;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class WorkerDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final Log LOG = LogFactory.getLog(WorkerComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        LOG.info("Started Fingerprint Worker Discovery Component. (Auto)");
        Set<DiscoveredResourceDetails> servers = new LinkedHashSet<DiscoveredResourceDetails>();
        LOG.info("Ended Fingerprint Worker Discovery Component. (Auto)");
        return servers;
    }

    protected DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext context, Configuration conf, ProcessInfo processInfo) throws InvalidPluginConfigurationException {
        String pidfile = conf.getSimpleValue("pidfile");
        String propertiesfile = conf.getSimpleValue("propertiesfile");

        FileSystemResource resource = new FileSystemResource(pidfile);
        long pid = 0;
        try {
            String pidString = ResourceUtils.getResourceTextContents(resource);
            pid = Long.parseLong(pidString);
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException("Unable to load process id file", e);
        }

        Properties props = new Properties();
        try {
            File propertiesFile = new File(propertiesfile);
            props.load(new FileInputStream(propertiesFile));
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Unable to load process properties file", e);
        }

        String name = conf.getSimpleValue("name");
        String host = props.getProperty(name + "." + "host");
        String port = props.getProperty(name + "." + "port");
        String keyspace = props.getProperty(name + "." + "keyspace");
        String username = props.getProperty(name + "." + "username");
        String password = props.getProperty(name + "." + "password");
        String key = "" + (host + port + keyspace).hashCode();

        context.getDefaultPluginConfiguration().setSimpleValue("key", key);
        context.getDefaultPluginConfiguration().setSimpleValue("pid", String.valueOf(pid));
        context.getDefaultPluginConfiguration().setSimpleValue("host", host);
        context.getDefaultPluginConfiguration().setSimpleValue("port", port);
        context.getDefaultPluginConfiguration().setSimpleValue("keyspace", keyspace);
        context.getDefaultPluginConfiguration().setSimpleValue("username", username);
        context.getDefaultPluginConfiguration().setSimpleValue("password", password);

        LOG.info("[Manual] =======================================");
        LOG.info("key: " + key);
        LOG.info("name: " + conf.getSimpleValue("name"));
        LOG.info("host: " + host);
        LOG.info("port: " + port);
        LOG.info("username: " + username);
        LOG.info("password: " + password);
        LOG.info("keyspace: " + keyspace);
        LOG.info("[Manual] =======================================");

        return new DiscoveredResourceDetails(context.getResourceType(), name, name, "0.1", "Worker", conf, new ProcessInfo(pid));
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration conf, ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {
        LOG.info("Started Fingerprint Worker Discovery Component. (Manual)");

        DiscoveredResourceDetails resourceDetails = createResourceDetails(resourceDiscoveryContext, conf, null);
        LOG.info("Ended Fingerprint Worker Discovery Component. (Manual)");
        return resourceDetails;
    }
}