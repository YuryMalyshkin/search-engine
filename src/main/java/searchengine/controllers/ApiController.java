package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.SearchData;
import searchengine.config.Site;
import searchengine.dto.ResultResponse;
import searchengine.dto.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<ResultResponse>  startIndexing(){
        return ResponseEntity.ok(statisticsService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<ResultResponse> stopIndexing(){
        return ResponseEntity.ok(statisticsService.stopIndexing());
    }
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(SearchData data){
        return ResponseEntity.ok(statisticsService.search(data));
    }
    @PostMapping("/indexPage")
    public ResponseEntity<ResultResponse> addPage(Site site){
        return ResponseEntity.ok(statisticsService.addPage(site));
    }
}
