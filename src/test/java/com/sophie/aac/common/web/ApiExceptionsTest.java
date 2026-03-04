package com.sophie.aac.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionsTest {

  @Test
  void exception_types_expose_expected_http_statuses() {
    assertThat(new BadRequestException("x").status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(new UnauthorizedException("x").status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(new ForbiddenException("x").status()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(new NotFoundException("x").status()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
