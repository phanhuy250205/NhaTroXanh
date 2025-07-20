package nhatroxanh.com.Nhatroxanh.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ánh xạ cho /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/")
                .setCachePeriod(3600);
        System.out.println("Resource handler registered for /uploads/** to classpath:/static/uploads/");

        // Ánh xạ cho /uploads/cccd/**
        registry.addResourceHandler("/uploads/cccd/**")
                .addResourceLocations("classpath:/static/uploads/cccd/", "file:/path/to/your/project/uploads/cccd/")
                .setCachePeriod(3600);
        System.out.println("Resource handler registered for /uploads/cccd/** to classpath:/static/uploads/cccd/ and file:/path/to/your/project/uploads/cccd/");
    }
}