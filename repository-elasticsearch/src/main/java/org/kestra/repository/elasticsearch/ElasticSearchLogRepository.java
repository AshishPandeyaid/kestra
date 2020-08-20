package org.kestra.repository.elasticsearch;

import com.devskiller.friendly_id.FriendlyId;
import io.micronaut.data.model.Pageable;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.kestra.core.models.executions.LogEntry;
import org.kestra.core.models.validations.ModelValidator;
import org.kestra.core.repositories.ArrayListTotal;
import org.kestra.core.repositories.LogRepositoryInterface;
import org.kestra.core.utils.ThreadMainFactoryBuilder;
import org.kestra.repository.elasticsearch.configs.IndicesConfig;
import org.slf4j.event.Level;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ElasticSearchRepositoryEnabled
public class ElasticSearchLogRepository extends AbstractElasticSearchRepository<LogEntry> implements LogRepositoryInterface {
    private static final String INDEX_NAME = "logs";

    @Inject
    public ElasticSearchLogRepository(
        RestHighLevelClient client,
        List<IndicesConfig> indicesConfigs,
        ModelValidator modelValidator,
        ThreadMainFactoryBuilder threadFactoryBuilder
    ) {
        super(client, indicesConfigs, modelValidator, threadFactoryBuilder, LogEntry.class);
    }

    @Override
    public ArrayListTotal<LogEntry> find(String query, Pageable pageable) {
        return super.findQueryString(INDEX_NAME, query, pageable);
    }

    @Override
    public List<LogEntry> findByExecutionId(String id, Level minLevel) {
        BoolQueryBuilder bool = this.defaultFilter()
            .must(QueryBuilders.termQuery("executionId", id));

        if (minLevel != null) {
            bool.must(minLevel(minLevel));
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .query(bool)
            .sort("timestamp", SortOrder.ASC);

        return this.scroll(INDEX_NAME, sourceBuilder);
    }

    @Override
    public List<LogEntry> findByExecutionIdAndTaskRunId(String executionId, String taskRunId, Level minLevel) {
        BoolQueryBuilder bool = this.defaultFilter()
            .must(QueryBuilders.termQuery("executionId", executionId))
            .must(QueryBuilders.termQuery("taskRunId", taskRunId));

        if (minLevel != null) {
            bool.must(minLevel(minLevel));
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .query(bool)
            .sort("timestamp", SortOrder.ASC);

        return this.scroll(INDEX_NAME, sourceBuilder);
    }

    @Override
    public LogEntry save(LogEntry log) {
        this.putRequest(INDEX_NAME, FriendlyId.createFriendlyId(), log);

        return log;
    }

    private TermsQueryBuilder minLevel(Level minLevel) {
        return QueryBuilders.termsQuery("level", LogEntry.findLevelsByMin(minLevel));
    }
}