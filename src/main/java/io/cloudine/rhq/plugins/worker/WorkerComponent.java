
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

import java.util.Set;

public class WorkerComponent implements ResourceComponent, MeasurementFacet {

    private static final Log LOG = LogFactory.getLog(WorkerComponent.class);

    private String name;
    private String host;
    private String username;
    private String password;
    private String keyspace;
    private String port;
    private String pid;

    private CassandraHandler handler;

    public void initialize(PluginContext context) throws Exception {
        LOG.info("Initialized Worker.");
    }

    public void shutdown() {
        LOG.info("Ended Worker.");
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        LOG.info("Started Worker.");

        Configuration conf = context.getPluginConfiguration();

        this.pid = conf.getSimpleValue("pid");
        this.name = conf.getSimpleValue("name");
        this.host = conf.getSimpleValue("host");
        this.port = conf.getSimpleValue("port");
        this.username = conf.getSimpleValue("username");
        this.password = conf.getSimpleValue("password");
        this.keyspace = conf.getSimpleValue("keyspace");

        LOG.info("[Component] =======================================");
        LOG.info("name: " + name);
        LOG.info("host: " + host);
        LOG.info("port: " + port);
        LOG.info("username: " + username);
        LOG.info("password: " + password);
        LOG.info("keyspace: " + keyspace);
        LOG.info("[Component] =======================================");
    }

    public void stop() {
        LOG.info("Stopped Worker.");
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
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
                ProcessInfo processInfo = new ProcessInfo(Long.parseLong(pid));
                ProcCpu cpu = processInfo.priorSnaphot().getCpu();

                report.addData(new MeasurementDataNumeric(request, Double.parseDouble("" + cpu.getTotal())));
            } else if ("ram".equals(request.getName())) {
                ProcessInfo processInfo = new ProcessInfo(Long.parseLong(pid));
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
