package com.sophie.aac;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = AacApiApplication.class,
    properties = {
        "spring.main.web-application-type=none",

        // IMPORTANT: correct key is spring.autoconfigure.exclude (NOT spring.autoconfigure.exclude= in code)
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",

        // Extra safety: even if something sneaks in, don't run migrations
        "spring.flyway.enabled=false",

        // Extra safety: avoid any attempt at schema actions
        "spring.jpa.hibernate.ddl-auto=none"
    }
)
@ActiveProfiles("test")
class AacApiApplicationTests {

  @Test
  void contextLoads() {
    // CI-safe: verifies Spring context loads without needing Postgres on localhost.
  }
}
