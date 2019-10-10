package org.floworc.core.storages;

import org.floworc.core.models.executions.Execution;
import org.floworc.core.models.flows.Flow;
import org.floworc.core.models.flows.Input;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public interface StorageInterface {
    InputStream get(URI uri) throws FileNotFoundException;

    StorageObject put(URI uri, InputStream data) throws IOException;

    default StorageObject from(Flow flow, Execution execution, Input input, File file) throws IOException {
        try {
            URI uri = new URI(String.join(
                "/",
                Arrays.asList(
                    flow.getNamespace().replace(".", "/"),
                    flow.getId(),
                    "executions",
                    execution.getId(),
                    "inputs",
                    input.getName(),
                    file.getName()
                )
            ));

            return this.put(uri, new FileInputStream(file));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}