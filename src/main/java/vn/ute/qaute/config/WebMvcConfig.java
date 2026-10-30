package vn.ute.qaute.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Tài nguyên người dùng tải lên lưu trữ cục bộ (Ưu tiên ánh xạ trước)
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException ignored) {
            }
        }
        String uploadPathUri = uploadDir.toUri().toString();
        if (!uploadPathUri.endsWith("/")) {
            uploadPathUri += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPathUri);

        // Tài nguyên tĩnh mặc định trong classpath
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
