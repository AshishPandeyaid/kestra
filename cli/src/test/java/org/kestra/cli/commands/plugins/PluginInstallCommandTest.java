package org.kestra.cli.commands.plugins;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kestra.cli.contexts.KestraClassLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PluginInstallCommandTest {
    @BeforeAll
    static void init() {
        KestraClassLoader.create(PluginInstallCommandTest.class.getClassLoader());
    }

    @Test
    void run() throws IOException {
        Path pluginsPath = Files.createTempDirectory(PluginInstallCommandTest.class.getSimpleName());
        pluginsPath.toFile().deleteOnExit();

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = {"--plugins", pluginsPath.toAbsolutePath().toString(), "org.kestra.task.notifications:task-notifications:0.1.0"};
            PicocliRunner.run(PluginInstallCommand.class, ctx, args);

            List<Path> files = Files.list(pluginsPath).collect(Collectors.toList());

            assertThat(files.size(), is(1));
            assertThat(files.get(0).getFileName().toString(), is("task-notifications-0.1.0.jar"));
        }
    }
}