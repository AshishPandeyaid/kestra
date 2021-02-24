package org.kestra.core.runners;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@MicronautTest
public class MultipleConditionTriggerTest extends AbstractMemoryRunnerTest {
    @Inject
    private MultipleConditionTriggerCaseTest runnerCaseTest;

    @Test
    void trigger() throws Exception {
        runnerCaseTest.trigger();
    }
}
