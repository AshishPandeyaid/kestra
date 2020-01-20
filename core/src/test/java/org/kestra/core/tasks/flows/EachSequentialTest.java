package org.kestra.core.tasks.flows;

import org.hamcrest.core.StringContains;
import org.kestra.core.models.executions.TaskRun;
import org.kestra.core.runners.AbstractMemoryRunnerTest;
import org.kestra.core.models.executions.Execution;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;

public class EachSequentialTest extends AbstractMemoryRunnerTest {
    @Test
    void sequential() throws TimeoutException {
        Execution execution = runnerUtils.runOne("org.kestra.tests", "each-sequential");

        assertThat(execution.getTaskRunList(), hasSize(8));
    }

    @Test
    void sequentialNested() throws TimeoutException {
        Execution execution = runnerUtils.runOne("org.kestra.tests", "each-sequential-nested");

        assertThat(execution.getTaskRunList(), hasSize(23));

        TaskRun last = execution.findTaskRunByTaskId("2_return");
        TaskRun vars = execution.findTaskRunByTaskIdAndValue("1_2_1-return", Arrays.asList("s1", "a"));
        assertThat((String) last.getOutputs().get("return"), containsString((String) vars.getOutputs().get("return")));
    }
}
