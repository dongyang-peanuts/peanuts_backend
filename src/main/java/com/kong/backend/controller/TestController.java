package com.kong.backend.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final DataSource dataSource;

    @GetMapping("/db-test")
    public String testConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return "DB 연결 성공: " + conn.getMetaData().getURL();
        }
    }
}
