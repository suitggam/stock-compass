package dev.a301.stock.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

  @Value("${cors.allowed-origins:http://localhost:5173}")
  private String allowedOriginsProp;

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry reg) {
        var c = reg.addMapping("/**")
            .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);

        // 콤마로 분리된 오리진들 반영
        for (String o : allowedOriginsProp.split(",")) {
          String v = o.trim();
          if (!v.isEmpty()) c.allowedOrigins(v);
        }
      }
    };
  }
}
