package com.au.example.springboot3;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@SpringBootApplication
public class Springboot3Application {

    public static void main(String[] args) {
        SpringApplication.run(Springboot3Application.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerService cs) {
        return event -> cs.all().forEach(System.out::println);
    }

}


@Controller
@ResponseBody
class CustomerHttpController {

    private final CustomerService customerService;

    private final ObservationRegistry observationRegistry;

    CustomerHttpController(CustomerService customerService, ObservationRegistry observationRegistry) {
        this.customerService = customerService;
        this.observationRegistry = observationRegistry;
    }

    @GetMapping("/customers")
    Collection<Customer> all() {
        return customerService.all();
    }

    @GetMapping("/customers/{name}")
    Customer byName(@PathVariable String name) {
        Observation.createNotStarted("byName",this.observationRegistry)
                .observe(() -> customerService.byName(name));
        return customerService.byName(name);
    }
}

@ControllerAdvice
class ErrorHandlingControllerAdvice {


    @ExceptionHandler
    public ProblemDetail handleIllegalStateException(IllegalStateException exception) {
        var pd = ProblemDetail.forStatus(HttpStatus.valueOf(404));
        pd.setDetail("the name must start with a capital letter!");
        return pd;
    }

}

@Service
class CustomerService {

    private final JdbcTemplate jdbcTemplate;


    private RowMapper<Customer> customerRowMapper = (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

    CustomerService(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    Customer byName(String name) {
        Assert.state(Character.isUpperCase(name.charAt(0)), "the name must start a capital letter!");
        return this.jdbcTemplate.queryForObject("select * from customers where name =?", this.customerRowMapper, name);
    }


    Collection<Customer> all() {
        return this.jdbcTemplate.query("select * from customers", this.customerRowMapper);
    }


}

record Customer(Integer id, String name) {
}
