package com.sophie.aac;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AacApiSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void minimal_context_starts() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }
}
