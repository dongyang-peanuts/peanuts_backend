package com.kong.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SshDataSourceConfig {
    private final SshTunnelingInitializer initializer;

    @Bean("dataSource")
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {

        Integer forwardedPort = initializer.buildSshConnection();  // ssh 연결 및 터널링 설정
        String url = properties.getUrl().replace("[forwardedPort]", Integer.toString(forwardedPort));
        log.info(url);
        return DataSourceBuilder.create()
                .url(url)
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
    }
}
