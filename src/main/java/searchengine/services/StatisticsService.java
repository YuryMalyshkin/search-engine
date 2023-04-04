package searchengine.services;

import searchengine.dto.SearchData;
import searchengine.config.Site;
import searchengine.dto.ResultResponse;
import searchengine.dto.SearchResponse;
import searchengine.dto.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    ResultResponse startIndexing();
    ResultResponse stopIndexing();
    SearchResponse search(SearchData data);
    ResultResponse addPage(Site site);
}
