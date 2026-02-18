package com.loadgen.oltp;

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
 * Database Connection Manager using HikariCP
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

        // Connection pool settings for high load
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "100")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "20")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.timeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.idle.timeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.max.lifetime", "1800000")));

        // Performance settings
        config.setAutoCommit(true);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // Oracle specific settings
        config.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "50");
        config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "30000");

        config.setPoolName("OltpConnectionPool");

        this.dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized: {}", config.getJdbcUrl());
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
        props.setProperty("db.username", System.getenv().getOrDefault("DB_USERNAME", "oltp_user"));
        props.setProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", "OltpPass123!"));
        props.setProperty("db.pool.max", System.getenv().getOrDefault("DB_POOL_MAX", "100"));
        props.setProperty("db.pool.min", System.getenv().getOrDefault("DB_POOL_MIN", "20"));
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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
            logger.info("Database connection pool closed");
        }
    }
}
