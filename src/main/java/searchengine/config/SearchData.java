package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchData {
    String query;
    String site;
    Integer offset = 0;
    Integer limit = 20;
}
