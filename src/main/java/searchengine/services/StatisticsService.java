package searchengine.services;

import searchengine.config.SearchData;
import searchengine.config.Site;
import searchengine.dto.statistics.ResultResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    ResultResponse startIndexing();
    ResultResponse stopIndexing();
    SearchResponse search(SearchData data);
    ResultResponse addPage(Site site);
}
