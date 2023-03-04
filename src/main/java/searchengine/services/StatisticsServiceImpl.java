package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.SearchData;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.Status;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private static final ForkJoinPool pool = new ForkJoinPool();
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        try{
            for(int i = 0; i < sitesList.size(); i++) {
                Site site = sitesList.get(i);
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());
                String sql = "SELECT COUNT(*) FROM page WHERE site_id =" + (i + 1) + ";";
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                int pages = 0;
                if (resultSet.next()){
                    pages = resultSet.getInt(1);
                }
                sql = "SELECT COUNT(*) FROM lemma WHERE site_id =" + (i + 1) + ";";
                resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                int lemmas = 0;
                if (resultSet.next()){
                    lemmas = resultSet.getInt(1);
                }
                item.setPages(pages);
                item.setLemmas(lemmas);
                sql = "SELECT status, status_time, last_error FROM site WHERE id =" + (i + 1) + ";";
                resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                if (resultSet.next()){
                    item.setStatus(Status.values()[resultSet.getInt(1)].name());
                    item.setStatusTime(resultSet.getTime(2).getTime());
                    item.setError(resultSet.getString(3) == null ? "" : resultSet.getString(3));
                }
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
                detailed.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    @Override
    public ResultResponse startIndexing() {
        ResultResponse response = new ResultResponse();
        if (SiteTree.isIndexing()){
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            pool.execute(new SiteTree(sites));
            response.setResult(true);
        }
        return response;
    }

    public ResultResponse addPage(Site site) {
        ResultResponse response = new ResultResponse();
        String url = site.getUrl();
        for (Site site1 : sites.getSites()){
            if (url.startsWith(site1.getUrl())){
                try{
                    Document doc;
                    doc = Jsoup.connect(url).get();
                    SiteTree.updatePage(url, sites.getSites().indexOf(site1) + 1, doc);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                response.setResult(true);
                response.setError("");
                return response;
            }
        }
        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return response;
    }

    @Override
    public ResultResponse stopIndexing() {
        ResultResponse response = new ResultResponse();
        if (SiteTree.isIndexing()){
            SiteTree.stopIndexing();
            response.setResult(true);
            for (int i = 0; i<sites.getSites().size(); i++){
                try{
                    String sql = "SELECT status FROM site WHERE id =" + (i + 1) + ";";
                    ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                    if (resultSet.next()){
                        if (resultSet.getInt(1) == Status.INDEXING.ordinal()){
                            sql = "UPDATE `site` SET status = '" + Status.FAILED.ordinal() + "' last_error = '" +
                                    "'Индексация остановлена пользователем'"
                                    + "' WHERE id = " + (i + 1) + ";";
                            ConnectionService.connect().createStatement().execute(sql);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }

    private TreeMap<Integer, TreeMap<String,List<Integer>>> findLemmasFrequency (Map<String, Integer> lemmas, int site_id){
        TreeMap<Integer, TreeMap<String,List<Integer>>> lemmasFrequency = new TreeMap<>();
        String sql;
        try {
            for (String lemma : lemmas.keySet()) {
                if (site_id != 0) {
                    sql = "SELECT * FROM lemma WHERE lemma = '" + lemma + "' AND site_id = '" + site_id + "';";
                } else {
                    sql = "SELECT * FROM lemma WHERE lemma = '" + lemma + "';";
                }
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                int frequency = 0;
                ArrayList<Integer> ids = new ArrayList<>();
                while (resultSet.next()) {
                    frequency += resultSet.getInt("frequency");
                    ids.add(resultSet.getInt("id"));
                }
                if (frequency == 0) {
                    return null;
                }
                if (!lemmasFrequency.containsKey(frequency)) {
                    TreeMap<String, List<Integer>> lemmaIds = new TreeMap<>();
                    lemmaIds.put(lemma, ids);
                    lemmasFrequency.put(frequency, lemmaIds);
                }
                lemmasFrequency.get(frequency).put(lemma, ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lemmasFrequency;
    }

    private ArrayList<Pairs> getSortedPageIds(TreeMap<Integer, TreeMap<String,List<Integer>>> lemmasFrequency){
        ArrayList<Pairs> orderedIds = new ArrayList<>();
        try{
            ArrayList<Integer> pageIds = new ArrayList<>();
            TreeMap<Integer,Integer> pageRanks = new TreeMap<>();
            for (Integer frequency : lemmasFrequency.keySet()){
                for (String lemma : lemmasFrequency.get(frequency).keySet()){
                    ArrayList<Integer> currentIds = new ArrayList<>();
                    for (Integer id : lemmasFrequency.get(frequency).get(lemma)){
                        String sql = "SELECT * FROM `lemma_index` WHERE lemma_id = " + id + ";";
                        ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                        while (resultSet.next()){
                            Integer page_id = resultSet.getInt("page_id");
                            Integer lemma_rank =  resultSet.getInt("lemma_rank");
                            currentIds.add(page_id);
                            if (pageRanks.containsKey(page_id)){
                                pageRanks.put(page_id, pageRanks.get(page_id) + lemma_rank);
                            } else {
                                pageRanks.put(page_id, lemma_rank);
                            }
                        }
                    }
                    if (pageIds.size() == 0){
                        pageIds.addAll(currentIds);
                    } else {
                        pageIds.retainAll(currentIds);
                    }
                    if (pageIds.size() == 0){
                        return null;
                    }
                }
            }
            for (int i = 0; i < pageIds.size(); i++)
            {
                orderedIds.add(new Pairs(pageIds.get(i), pageRanks.get(pageIds.get(i))));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        orderedIds.sort((Pairs p1, Pairs p2) -> -p1.getY().compareTo(p2.getY()));
        return  orderedIds;
    }

    @Override
    public SearchResponse search(SearchData data) {
        String text = data.getQuery();
        try{
            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(text);
            SearchResponse response = new SearchResponse();
            int site_id = 0;
            String sql;
            if (data.getSite() != null){
                sql = "SELECT * FROM site WHERE url = '" + data.getSite() + "';";
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                if (resultSet.next()){
                    site_id = resultSet.getInt("id");
                }
            }
            TreeMap<Integer, TreeMap<String,List<Integer>>> lemmasFrequency = findLemmasFrequency(lemmas, site_id);
            if (lemmasFrequency == null){
                response.setCount(0);
                response.setResult(true);
                return response;
            }
            ArrayList<Pairs> orderedIds = getSortedPageIds(lemmasFrequency);
            if (orderedIds == null){
                response.setCount(0);
                response.setResult(true);
                return response;
            }
            response.setCount(orderedIds.size());
            for (int i = data.getOffset(); i < Math.min(orderedIds.size(), data.getOffset() + data.getLimit()); i++){
                SearchInfo searchInfo = new SearchInfo();
                searchInfo.setRelevance((float)orderedIds.get(i).getY()/orderedIds.get(0).getY());
                sql = "SELECT * FROM `page` WHERE id = '" + orderedIds.get(i).getX() + "';";
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                if (resultSet.next()){
                    site_id = resultSet.getInt("site_id") - 1;
                    searchInfo.setSite(sites.getSites().get(site_id).getUrl());
                    searchInfo.setSiteName(sites.getSites().get(site_id).getName());
                    String htmlContent = resultSet.getString("content");
                    Document doc = Jsoup.parse(htmlContent);
                    Elements elements = doc.select("title");
                    searchInfo.setTitle(elements.size() > 0 ? elements.get(0).text() : "");
                    searchInfo.setSnippet(parser.buildSnippet(lemmas.keySet().toArray(new String[0]), doc.text()));
                    searchInfo.setUri(resultSet.getString("path").replaceFirst(sites.getSites().get(site_id).getUrl(),""));
                    response.addSearchInfo(searchInfo);
                }
            }
            response.setResult(true);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
