package org.ekstep.ep.samza.service;

import org.ekstep.ep.samza.domain.Event;
import org.ekstep.ep.samza.esclient.ElasticSearchService;
import org.ekstep.ep.samza.logger.Logger;
import org.ekstep.ep.samza.esclient.ClientResponse;
import org.ekstep.ep.samza.task.EsIndexerSecondarySink;
import org.ekstep.ep.samza.task.EsIndexerSecondarySource;

import java.io.IOException;

public class EsIndexerSecondaryService {
    static Logger LOGGER = new Logger(EsIndexerSecondaryService.class);
    private final ElasticSearchService elasticSearchService;

    public EsIndexerSecondaryService(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    public void process(EsIndexerSecondarySource source, EsIndexerSecondarySink sink) {

        Event event = source.getEvent();

        try {

            if(!event.can_be_indexed()){
                LOGGER.info("INDEX DETAILS ARE MISSING! SKIPPING", event.id());
                event.markSkipped();
                sink.toFailedTopic(event);
                return;
            }

            ClientResponse response = elasticSearchService.index(event.indexName(), event.indexType(), event.getJson(), event.id());

            if(success(response)) {
                LOGGER.info("ES INDEXER SUCCESS", event.id());
            } else {
                LOGGER.error("ES INDEXER FAILED : RESPONSE", response.toString());
                LOGGER.error("ES INDEXER FAILED : EVENT", event.toString());
                event.markFailed(response.getStatus(),response.getMessage());
                sink.toFailedTopic(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("ES INDEXER EXCEPTION : MESSAGE", e.getMessage());
            LOGGER.error("ES INDEXER EXCEPTION : EVENT", event.toString());
            event.markFailed("Error", e.getMessage());
            sink.toFailedTopic(event);
        }
    }

    private boolean success(ClientResponse response) {
        return (response.getStatus().equals("200") || response.getStatus().equals("201"));
    }
}