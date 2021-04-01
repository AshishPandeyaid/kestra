package io.kestra.core.repositories;

import io.kestra.core.models.SearchResult;
import io.micronaut.data.model.Pageable;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.flows.Flow;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;

public interface FlowRepositoryInterface {
    Optional<Flow> findById(String namespace, String id, Optional<Integer> revision);

    default Flow findByExecution(Execution execution) {
        Optional<Flow> find = this.findById(
            execution.getNamespace(),
            execution.getFlowId(),
            Optional.of(execution.getFlowRevision())
        );

        if (find.isEmpty()) {
            throw new IllegalStateException("Unable to find flow '" + execution.getNamespace() + "." +
                execution.getFlowId() + "' with revision " + execution.getFlowRevision() + " on execution " +
                execution.getId()
            );
        } else {
            return find.get();
        }
    }

    default Optional<Flow> findById(String namespace, String id) {
        return this.findById(namespace, id, Optional.empty());
    }

    List<Flow> findRevisions(String namespace, String id);

    List<Flow> findAll();

    List<Flow> findAllWithRevisions();

    List<Flow> findByNamespace(String namespace);

    ArrayListTotal<Flow> find(String query, Pageable pageable);

    ArrayListTotal<SearchResult<Flow>> findSourceCode(String query, Pageable pageable);

    List<String> findDistinctNamespace();

    Flow create(Flow flow) throws ConstraintViolationException;

    Flow update(Flow flow, Flow previous) throws ConstraintViolationException;

    Flow delete(Flow flow);
}
