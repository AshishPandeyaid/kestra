package org.kestra.core.tasks.storages;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;
import org.kestra.core.models.annotations.Documentation;
import org.kestra.core.models.annotations.Example;
import org.kestra.core.models.annotations.InputProperty;
import org.kestra.core.models.annotations.OutputProperty;
import org.kestra.core.models.tasks.RunnableTask;
import org.kestra.core.models.tasks.Task;
import org.kestra.core.runners.RunContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;

import static org.kestra.core.utils.Rethrow.throwConsumer;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Documentation(
    description = "Concat files from internal storage."
)
@Example(
    code = {
        "files: ",
        "  - \"kestra://long/url/file1.txt\"",
        "  - \"kestra://long/url/file2.txt\"",
        "separator: \"\\n\"",
    }
)
public class Concat extends Task implements RunnableTask<Concat.Output> {
    @InputProperty(
        description = "List of files to be concatenated.",
        body = {
            "Must be a `kestra://` storage url"
        },
        dynamic = true
    )
    private List<String> files;

    @InputProperty(
        description = "The separator to used between files, default is no seprator",
        dynamic = false
    )
    private String separator;

    @Override
    public Concat.Output run(RunContext runContext) throws Exception {
        File tempFile = File.createTempFile("concat_", "");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            if (files != null) {
                files.forEach(throwConsumer(s -> {
                    URI from = new URI(runContext.render(s));
                    IOUtils.copyLarge(runContext.uriToInputStream(from), fileOutputStream);

                    if (separator != null) {
                        IOUtils.copy(new ByteArrayInputStream(this.separator.getBytes()), fileOutputStream);
                    }
                }));
            }
        }

        return Concat.Output.builder()
            .uri(runContext.putTempFile(tempFile))
            .build();
    }

    @Builder
    @Getter
    public static class Output implements org.kestra.core.models.tasks.Output {
        @OutputProperty(
            description = "The concatenate file uri."
        )
        private final URI uri;
    }
}
