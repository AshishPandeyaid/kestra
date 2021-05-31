package io.kestra.core.storages;

import io.micronaut.core.annotation.Introspected;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.TaskRun;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.flows.Input;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.utils.Slugify;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Introspected
public interface StorageInterface {
    InputStream get(URI uri) throws FileNotFoundException;

    Long size(URI uri) throws FileNotFoundException;

    URI put(URI uri, InputStream data) throws IOException;

    boolean delete(URI uri) throws IOException;

    List<URI> deleteByPrefix(URI storagePrefix) throws IOException;

    default String executionPrefix(Flow flow, Execution execution) {
        return String.join(
            "/",
            Arrays.asList(
                flow.getNamespace().replace(".", "/"),
                Slugify.of(flow.getId()),
                "executions",
                execution.getId()
            )
        );
    }

    default Optional<String> extractExecutionId(URI path) {
        Pattern pattern = Pattern.compile("^/(.+)/executions/([^/]+)/", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(path.getPath());

        if (!matcher.find() || matcher.group(2).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(matcher.group(2));
    }

    default URI uri(Flow flow, Execution execution, String inputName, String file) throws  URISyntaxException {
        return new URI("/" + String.join(
            "/",
            Arrays.asList(
                executionPrefix(flow, execution),
                "inputs",
                inputName,
                file
            )
        ));
    }

    default URI from(Flow flow, Execution execution, String input, File file) throws IOException {
        try {
            return this.put(
                this.uri(flow, execution, input, file.getName()),
                new BufferedInputStream(new FileInputStream(file))
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    default URI from(Flow flow, Execution execution, Input input, File file) throws IOException {
        return this.from(flow, execution, input.getName(), file);
    }

    default URI outputPrefix(Flow flow)  {
        try {
            return new URI("/" + String.join(
                "/",
                Arrays.asList(
                    flow.getNamespace().replace(".", "/"),
                    Slugify.of(flow.getId())
                )
            ));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    default URI outputPrefix(Flow flow, Task task, Execution execution, TaskRun taskRun)  {
        try {
            return new URI("/" + String.join(
                "/",
                Arrays.asList(
                    flow.getNamespace().replace(".", "/"),
                    Slugify.of(flow.getId()),
                    "executions",
                    execution.getId(),
                    "tasks",
                    Slugify.of(taskRun.getTaskId()),
                    taskRun.getId()
                )
            ));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
