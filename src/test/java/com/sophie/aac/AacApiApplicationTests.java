package com.sophie.aac;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    // CI-safe: don't try to wire a datasource / JPA / Flyway for this smoke test
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class AacApiApplicationTests {

  @Test
  void contextLoads() {
    // Intentionally empty: proves Spring context starts without DB dependency.
  }
}
