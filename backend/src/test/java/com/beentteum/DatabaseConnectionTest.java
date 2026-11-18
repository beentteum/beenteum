
package com.beentteum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void databaseConnectionTest() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT current_database(), current_user, PostGIS_Version()"
             )) {

            assertTrue(connection.isValid(5));
            assertTrue(resultSet.next());

            assertEquals("beentteum", resultSet.getString(1));
            assertEquals("beentteum_app", resultSet.getString(2));

            System.out.println("DB 연결 성공!");
            System.out.println("Database: " + resultSet.getString(1));
            System.out.println("User: " + resultSet.getString(2));
            System.out.println("PostGIS: " + resultSet.getString(3));
        }
    }
}
