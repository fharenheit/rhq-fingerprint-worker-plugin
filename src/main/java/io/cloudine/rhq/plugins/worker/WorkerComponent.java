
package io.cloudine.rhq.plugins.worker;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.system.ProcessInfo;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class WorkerComponent implements ResourceComponent, MeasurementFacet {

    private static final Log LOG = LogFactory.getLog(WorkerComponent.class);

    private CassandraHandler handler;

    private ResourceContext resourceContext;

    private PluginContext pluginContext;

    public void initialize(PluginContext context) throws Exception {
        this.pluginContext = context;
        LOG.info("Initialized Worker.");
    }

    public void shutdown() {
        LOG.info("Ended Worker.");
    }

    public AvailabilityType getAvailability() {
        try {
            Configuration conf = resourceContext.getPluginConfiguration();
            String pidfile = conf.getSimpleValue("pidfile");

            FileSystemResource resource = new FileSystemResource(pidfile);
            long pid = 0;
            try {
                String pidString = ResourceUtils.getResourceTextContents(resource);
                pid = Long.parseLong(pidString);
            } catch (IOException e) {
                throw new InvalidPluginConfigurationException("Unable to load process id file", e);
            }

            ProcessInfo pinfo = new ProcessInfo(pid);
            return pinfo.priorSnaphot().isRunning() ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception ex) {
            return AvailabilityType.DOWN;
        }
    }

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;

        LOG.info("Started Worker.");
    }

    public void stop() {
        LOG.info("Stopped Worker.");
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Configuration conf = resourceContext.getPluginConfiguration();

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


        LOG.info("[Values] ---------------------------------------");
        LOG.info("name: " + name);
        LOG.info("host: " + host);
        LOG.info("port: " + port);
        LOG.info("username: " + username);
        LOG.info("password: " + password);
        LOG.info("keyspace: " + keyspace);
        LOG.info("pid: " + pid);
        LOG.info("[Values] ---------------------------------------");

        CassandraHandler handler = new CassandraHandler();
        handler.openSession(host, port, username, password, keyspace);

        for (MeasurementScheduleRequest request : metrics) {
            if ("playtime".equals(request.getName())) {
                String sql = "SELECT size FROM audio_total_fingerprint WHERE id = 1";

                PreparedStatement statement = handler.getSession().prepare(sql);
                ResultSet results = handler.getSession().execute(statement.bind());
                Row row = results.one();

                int expected = row.getInt(0);
                float millis = (expected * 2048 * 1000) / (16 * 11025);

                report.addData(new MeasurementDataNumeric(request, Double.parseDouble(String.valueOf(millis))));
            } else if ("count".equals(request.getName())) {
                String sql = "SELECT COUNT(*) FROM AUDIO_FILE_LIST";

                PreparedStatement statement = handler.getSession().prepare(sql);
                ResultSet results = handler.getSession().execute(statement.bind());
                Row row = results.one();
                long expected = row.getLong(0);

                report.addData(new MeasurementDataNumeric(request, Double.parseDouble("" + expected)));
            } else if ("cpu".equals(request.getName())) {
                ProcessInfo processInfo = new ProcessInfo(pid);
                ProcCpu cpu = processInfo.priorSnaphot().getCpu();

                report.addData(new MeasurementDataNumeric(request, Double.parseDouble("" + cpu.getTotal())));
            } else if ("ram".equals(request.getName())) {
                ProcessInfo processInfo = new ProcessInfo(pid);
                ProcMem memory = processInfo.priorSnaphot().getMemory();

                report.addData(new MeasurementDataNumeric(request, Double.parseDouble("" + memory.getSize())));
            }
        }

        try {
            handler.close();
        } catch (Exception e) {
        }
    }
}
