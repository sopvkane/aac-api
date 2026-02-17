package com.sophie.aac.phrases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PhraseControllerIT {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("aac")
          .withUsername("app")
          .withPassword("app");

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @LocalServerPort
  int port;

  // HttpClient is reusable and not Closeable; keep one instance per JVM.
  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static final ObjectMapper OM = new ObjectMapper();

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  @Test
  void phrases_crud_happy_path() throws Exception {
    var list1 = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases")).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(list1.statusCode()).isEqualTo(200);

    JsonNode arr1 = OM.readTree(list1.body());
    assertThat(arr1.isArray()).isTrue();
    assertThat(arr1.size()).isEqualTo(0);

    var created = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"I need help\"}"))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(created.statusCode()).isEqualTo(201);

    JsonNode createdJson = OM.readTree(created.body());
    assertThat(createdJson.get("id").asText()).isNotBlank();
    assertThat(createdJson.get("text").asText()).isEqualTo("I need help");
    String id = createdJson.get("id").asText();

    var list2 = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases")).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(list2.statusCode()).isEqualTo(200);
    assertThat(OM.readTree(list2.body()).size()).isEqualTo(1);

    var del = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases/" + id)).DELETE().build(),
        HttpResponse.BodyHandlers.discarding()
    );
    assertThat(del.statusCode()).isEqualTo(204);

    var list3 = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases")).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(list3.statusCode()).isEqualTo(200);
    assertThat(OM.readTree(list3.body()).size()).isEqualTo(0);
  }

  @Test
  void create_blank_text_returns_400() throws Exception {
    var res = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"   \"}"))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(res.statusCode()).isEqualTo(400);
  }

  @Test
  void delete_missing_returns_404() throws Exception {
    var res = CLIENT.send(
        HttpRequest.newBuilder(uri("/api/phrases/00000000-0000-0000-0000-000000000000"))
            .DELETE()
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(res.statusCode()).isEqualTo(404);
  }
}
