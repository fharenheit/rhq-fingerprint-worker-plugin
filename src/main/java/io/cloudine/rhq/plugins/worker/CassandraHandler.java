package io.cloudine.rhq.plugins.worker;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.util.StringUtils;

import java.util.Iterator;

public class CassandraHandler {

    protected Session session;
    protected CassandraOperations cassandraOps;
    protected Cluster cluster;

    public CassandraHandler openSession(String host, String port, String username, String password, String keyspace) {
        Cluster.Builder builder = Cluster.builder().addContactPoint(host);

        if(!StringUtils.isEmpty(port)) {
            builder.withPort(Integer.parseInt(port.trim()));
        }

        if (!StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
            builder.withCredentials(username, password);
        }

        cluster = builder.build();
        session = cluster.connect(keyspace);
        cassandraOps = new CassandraTemplate(session);
        return this;
    }

    public void close() {
        session.close();
        cluster.close();
    }

    public Iterator select(String table, String column1, String column2, Object value1, Object value2) {
        Select select = QueryBuilder.select().all().from(table);
        select.where(QueryBuilder.eq(column1, value1)).and(QueryBuilder.eq(column2, value2));
        ResultSet resultSet = session.execute(select);
        return resultSet.iterator();
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public CassandraOperations getCassandraOps() {
        return cassandraOps;
    }

    public void setCassandraOps(CassandraOperations cassandraOps) {
        this.cassandraOps = cassandraOps;
    }

}