package searchengine.dto.statistics;


import java.util.ArrayList;
import java.util.List;

public class SearchResponse {
    boolean result;
    int count;
    List<SearchInfo> data = new ArrayList<>();
    String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<SearchInfo> getData() {
        return data;
    }
    public void addSearchInfo(SearchInfo searchInfo){
        data.add(searchInfo);
    }
}
