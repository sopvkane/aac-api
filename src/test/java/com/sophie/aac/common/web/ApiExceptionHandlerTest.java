package com.sophie.aac.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void handleApiException_maps_status_and_detail() {
    var pd = handler.handleApiException(new ForbiddenException("No access"));
    assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(pd.getDetail()).isEqualTo("No access");
  }

  @Test
  void handleResponseStatus_maps_reason() {
    var pd = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No session"));
    assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(pd.getDetail()).isEqualTo("No session");
  }

  @Test
  void handleIllegalArgument_maps_bad_request() {
    var pd = handler.handleIllegalArgument(new IllegalArgumentException("Bad input"));
    assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(pd.getDetail()).isEqualTo("Bad input");
  }
}
