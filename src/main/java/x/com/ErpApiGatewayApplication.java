package x.com;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import x.com.filter.JwtValidationFilter;

@SpringBootApplication
public class ErpApiGatewayApplication {
	@Autowired
	JwtValidationFilter jwtValidationFilter;
	public static void main(String[] args) {
		SpringApplication.run(ErpApiGatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("service1_route", r -> r.path("/auth/**")
						.uri("http://localhost:8085"))
				.route("service2_route", r -> r.path("/login.html")
						.filters(f -> f.prefixPath("/auth"))
						.uri("http://localhost:8085"))
				.route("service3_route", r -> r.path("/page/**", "/erp/**")
//						.filters(f -> f.stripPrefix(1))
						.filters(f -> f.filter(jwtValidationFilter))
						.uri("http://localhost:8080"))
				.build();
	}
}
