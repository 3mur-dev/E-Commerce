package com.omar.ecommerce;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class DbCheck {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void checkConnection() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            System.out.println("Connected to DB: " + connection.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("Failed to connect to DB: " + e.getMessage());
        }
    }
}
