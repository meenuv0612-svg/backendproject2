package com.glowora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendDir = resolveFrontendDirectory();
        if (frontendDir != null) {
            String location = frontendDir.toUri().toString();
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            registry.addResourceHandler("/**")
                    .addResourceLocations(location, "classpath:/static/");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    private Path resolveFrontendDirectory() {
        Path current = Paths.get("").toAbsolutePath();
        Path candidate1 = current.resolve("frontend");
        if (Files.isDirectory(candidate1)) {
            return candidate1;
        }
        Path candidate2 = current.getParent() != null ? current.getParent().resolve("frontend") : null;
        if (candidate2 != null && Files.isDirectory(candidate2)) {
            return candidate2;
        }
        return null;
    }
}
