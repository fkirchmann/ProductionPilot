/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot;

import com.productionpilot.api.serializers.BatchSerializer;
import com.productionpilot.api.serializers.MachineSerializer;
import com.productionpilot.api.serializers.MeasurementSerializer;
import com.productionpilot.api.serializers.ParameterSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Optional;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "productionpilot")
@PWA(name = "ProductionPilot", shortName = "ProductionPilot", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@Push(transport = Transport.WEBSOCKET)
@RequiredArgsConstructor
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "icons/icon.png", "192x192");
        settings.addLink("shortcut icon", "icons/favicon.ico");
    }

    @Bean
    public ObjectMapper customObjectMapper(final Jackson2ObjectMapperBuilder builder) {
        return builder.serializers(
                new BatchSerializer(),
                new MeasurementSerializer(),
                new ParameterSerializer(),
                new MachineSerializer()
        ).build();
    }

    /**
     * By default. the swagger endpoint is set to http://<host>:<port>/
     * However, this breaks with a https reverse proxy.
     * This customizer sets the swagger endpoint to /, which works with a reverse proxy.
     */
    @Bean
    public OpenApiCustomiser fixSwaggerEndpointCustomizer() {
        return openAPI -> Optional.ofNullable(openAPI.getServers())
                .ifPresent(servers -> servers.forEach(server -> {
            server.setDescription("This API Endpoint");
            server.setUrl("/");
        }));
    }
}
