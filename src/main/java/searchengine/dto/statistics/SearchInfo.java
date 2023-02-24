package searchengine.dto.statistics;

import lombok.Data;

@Data
public class SearchInfo {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;
}
