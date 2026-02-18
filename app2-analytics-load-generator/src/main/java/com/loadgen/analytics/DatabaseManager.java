package com.loadgen.analytics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Connection Manager for Analytics queries
 */
@Component
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private HikariDataSource dataSource;

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.pool.max:15}")
    private int maxPoolSize;

    @Value("${db.pool.min:5}")
    private int minIdle;

    @PostConstruct
    public void initialize() {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);

        // Connection pool settings optimized for analytical queries
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(60000);
        config.setIdleTimeout(900000);
        config.setMaxLifetime(1800000);

        // CRITICAL: Prevent threads from hanging indefinitely
        config.setValidationTimeout(5000);  // 5 second validation timeout
        config.setLeakDetectionThreshold(60000);  // Detect connection leaks after 60 seconds

        // Performance settings for long-running queries
        config.setAutoCommit(true);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        config.addDataSourceProperty("defaultRowPrefetch", "100");

        // Oracle specific settings for analytics - TIMEOUTS ARE CRITICAL
        config.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "30");
        config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "60000");  // 60s connection timeout
        config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "180000");  // 3 min for analytics queries
        config.addDataSourceProperty("oracle.net.READ_TIMEOUT", "180000");  // 3 min socket read timeout

        config.setPoolName("AnalyticsConnectionPool");

        this.dataSource = new HikariDataSource(config);
        logger.info("Analytics database connection pool initialized: {} (max: {}, min: {})",
                    config.getJdbcUrl(), maxPoolSize, minIdle);
    }

    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        // Set default query timeout for all statements created from this connection
        // Analytics queries can take longer, but still need a timeout
        try {
            conn.setNetworkTimeout(null, 180000); // 3 minute timeout for analytics queries
        } catch (SQLException e) {
            // Some JDBC drivers don't support this, log but continue
            logger.debug("Could not set network timeout: {}", e.getMessage());
        }
        return conn;
    }

    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }

    @PreDestroy
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Analytics database connection pool closed");
        }
    }
}
