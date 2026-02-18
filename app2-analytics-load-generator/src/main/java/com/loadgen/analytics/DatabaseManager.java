package com.loadgen.analytics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Connection Manager for Analytics queries
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final HikariDataSource dataSource;

    public DatabaseManager() {
        Properties props = loadProperties();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));

        // Connection pool settings optimized for analytical queries
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "50")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "10")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.timeout", "60000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.idle.timeout", "900000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.max.lifetime", "1800000")));

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
        logger.info("Analytics database connection pool initialized: {}", config.getJdbcUrl());
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("application.properties not found, using environment variables");
                loadFromEnvironment(props);
            } else {
                props.load(input);
            }
        } catch (IOException e) {
            logger.error("Error loading properties", e);
            loadFromEnvironment(props);
        }
        return props;
    }

    private void loadFromEnvironment(Properties props) {
        props.setProperty("db.url", System.getenv().getOrDefault("DB_URL",
                "jdbc:oracle:thin:@localhost:1521:ORCL"));
        props.setProperty("db.username", System.getenv().getOrDefault("DB_USERNAME", "analytics_user"));
        props.setProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", "AnalyticsPass123!"));
        props.setProperty("db.pool.max", System.getenv().getOrDefault("DB_POOL_MAX", "50"));
        props.setProperty("db.pool.min", System.getenv().getOrDefault("DB_POOL_MIN", "10"));
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

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Analytics database connection pool closed");
        }
    }
}
