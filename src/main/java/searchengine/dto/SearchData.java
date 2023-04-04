package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchData {
    String query;
    String site = null;
    Integer offset = 0;
    Integer limit = 20;
}
