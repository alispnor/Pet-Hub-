package com.alispnor.pethub.config;

import com.alispnor.pethub.common.storage.PhotoStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PhotoStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final PhotoStorageProperties photoStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var absoluteDir = Path.of(photoStorageProperties.dir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/files/**").addResourceLocations(absoluteDir);
    }
}
