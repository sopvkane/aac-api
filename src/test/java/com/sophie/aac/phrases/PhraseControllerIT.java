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
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("aac")
          .withUsername("app")
          .withPassword("app");

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @LocalServerPort
  int port;

  @SuppressWarnings("resource") // HttpClient is not Closeable; IDE false positive
  private final HttpClient client = HttpClient.newHttpClient();

  private final ObjectMapper om = new ObjectMapper();

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri(path)).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  private HttpResponse<String> postJson(String path, String json) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  private HttpResponse<Void> delete(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri(path)).DELETE().build(),
        HttpResponse.BodyHandlers.discarding()
    );
  }

  @Test
  void phrases_crud_happy_path() throws Exception {
    // GET initially empty
    var list1 = get("/api/phrases");
    assertThat(list1.statusCode()).isEqualTo(200);

    JsonNode list1Json = om.readTree(list1.body());
    assertThat(list1Json.isArray()).isTrue();
    assertThat(list1Json.size()).isEqualTo(0);

    // POST create
    var created = postJson("/api/phrases", "{\"text\":\"I need help\"}");
    assertThat(created.statusCode()).isEqualTo(201);

    JsonNode createdJson = om.readTree(created.body());
    assertThat(createdJson.get("id").asText()).isNotBlank();
    assertThat(createdJson.get("text").asText()).isEqualTo("I need help");
    String id = createdJson.get("id").asText();

    // GET now has one
    var list2 = get("/api/phrases");
    assertThat(list2.statusCode()).isEqualTo(200);

    JsonNode list2Json = om.readTree(list2.body());
    assertThat(list2Json.isArray()).isTrue();
    assertThat(list2Json.size()).isEqualTo(1);

    // DELETE
    var del = delete("/api/phrases/" + id);
    assertThat(del.statusCode()).isEqualTo(204);

    // GET empty again
    var list3 = get("/api/phrases");
    assertThat(list3.statusCode()).isEqualTo(200);

    JsonNode list3Json = om.readTree(list3.body());
    assertThat(list3Json.isArray()).isTrue();
    assertThat(list3Json.size()).isEqualTo(0);
  }

  @Test
  void create_blank_text_returns_400() throws Exception {
    var res = postJson("/api/phrases", "{\"text\":\"   \"}");
    assertThat(res.statusCode()).isEqualTo(400);
  }

  @Test
  void delete_missing_returns_404() throws Exception {
    var res = client.send(
        HttpRequest.newBuilder(uri("/api/phrases/00000000-0000-0000-0000-000000000000"))
            .DELETE()
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
    assertThat(res.statusCode()).isEqualTo(404);
  }
}
