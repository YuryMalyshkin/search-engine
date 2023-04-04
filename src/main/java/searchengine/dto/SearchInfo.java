package searchengine.dto;

import lombok.Data;

@Data
public class SearchInfo {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;

    public SearchInfo(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public SearchInfo() {
    }
}
