/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api.util;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * This class is used to customize the swagger-ui.css file.
 *
 * Credit: <a href="https://github.com/springdoc/springdoc-openapi/issues/737#issuecomment-846603705">manniar@GitHub</a>
 */
@RestController
@RequestMapping(path = "/api-docs/v1/swagger-ui")
public class SwaggerController {
    @GetMapping(path = "/swagger-ui.css", produces = "text/css")
    @Operation(hidden = true) // Hide this endpoint from the swagger-ui
    public String getCss() {
        String orig = toText(getClass().getResourceAsStream("/META-INF/resources/webjars/swagger-ui/4.15.5/swagger-ui.css"));
        String customCss = """
                /* Hide the example responses and models, as they are not accurate due to the use of custom serializers */
                .response .model-example {
                  display: none !important;
                }
                .response .response-controls {
                  display: none !important;
                }
                .wrapper .models {
                  display: none;
                }""";
        return orig + "\n" + customCss;
    }

    static String toText(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
    }
}