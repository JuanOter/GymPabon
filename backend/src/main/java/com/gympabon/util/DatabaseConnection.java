package com.gympabon.util;

import com.gympabon.exception.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Component;

/**
 * Single place where the system obtains JDBC connections to MySQL. The
 * connection data comes from application.properties; the DAO classes only ask
 * this class for a connection and close it when they finish.
 *
 * <p>The data source is wrapped in a {@link TransactionAwareDataSourceProxy}:
 * inside a method marked with {@code @Transactional}, every DAO receives the
 * same connection, so all its operations are committed or rolled back
 * together. Outside a transaction it works as a normal connection pool.</p>
 */
@Component
public class DatabaseConnection {

    private final DataSource dataSource;

    public DatabaseConnection(DataSource dataSource) {
        this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
    }

    /**
     * Returns an open connection from the pool, or the connection of the
     * current transaction. The caller must close it, ideally with
     * try-with-resources.
     *
     * @return an open JDBC connection
     * @throws DataAccessException if MySQL is not reachable
     */
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible conectarse a la base de datos", e);
        }
    }
}
