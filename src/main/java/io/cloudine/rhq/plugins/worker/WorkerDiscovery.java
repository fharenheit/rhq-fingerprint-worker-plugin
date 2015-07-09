package io.cloudine.rhq.plugins.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.*;
import org.rhq.core.system.ProcessInfo;

import java.util.LinkedHashSet;
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
		String host = conf.getSimpleValue("host");
		String port = conf.getSimpleValue("port");
		String keyspace = conf.getSimpleValue("keyspace");
		String key = "" + (host + port + keyspace).hashCode();
		String name = conf.getSimpleValue("name");

		LOG.info("[Manual] =======================================");
		LOG.info("key: " + key);
		LOG.info("name: " + conf.getSimpleValue("name"));
		LOG.info("host: " + conf.getSimpleValue("host"));
		LOG.info("port: " + conf.getSimpleValue("port"));
		LOG.info("username: " + conf.getSimpleValue("username"));
		LOG.info("password: " + conf.getSimpleValue("password"));
		LOG.info("keyspace: " + conf.getSimpleValue("keyspace"));
		LOG.info("[Manual] =======================================");

		return new DiscoveredResourceDetails(context.getResourceType(), name, name, "0.1", "Worker", conf, processInfo);
	}

	@Override
	public DiscoveredResourceDetails discoverResource(Configuration conf, ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {
		LOG.info("Started Fingerprint Worker Discovery Component. (Manual)");

		DiscoveredResourceDetails resourceDetails = createResourceDetails(resourceDiscoveryContext, conf, null);
		LOG.info("Ended Fingerprint Worker Discovery Component. (Manual)");
		return resourceDetails;
	}
}