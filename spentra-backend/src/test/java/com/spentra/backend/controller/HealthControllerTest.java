package com.spentra.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class HealthControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HealthController healthController;

    @Test
    void testCheckHealth_Success() {
        // Arrange
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        // Act
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("UP", response.getBody().get("database"));
        assertTrue(!response.getBody().containsKey("error"));
    }

    @Test
    void testCheckHealth_UnexpectedDatabaseResponse() {
        // Arrange
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(0);

        // Act
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("DOWN", response.getBody().get("database"));
        assertEquals("Unexpected database response", response.getBody().get("error"));
    }

    @Test
    void testCheckHealth_ExceptionThrown_ReturnsSanitizedMessage() {
        // Arrange
        String sensitiveErrorMessage = "Detailed DB connection failure: SQL Error: 1045, SQLState: 28000, Access denied for user 'root'@'localhost'";
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException(sensitiveErrorMessage));

        // Act
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("DOWN", response.getBody().get("database"));

        // Ensure the sensitive message is NOT in the response
        String actualError = (String) response.getBody().get("error");
        assertEquals("Database connection failed", actualError);
        assertTrue(!actualError.contains("SQL Error"));
    }
}
