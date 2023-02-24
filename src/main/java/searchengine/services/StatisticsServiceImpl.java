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
import searchengine.model.LemmaSQL;
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

    @Override
    public SearchResponse search(SearchData data) {
        String text = data.getQuery();
        try{
            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(text);
            SearchResponse response = new SearchResponse();
            ArrayList<LemmaSQL> lemmasFounded = new ArrayList<>();
            TreeMap<Integer, TreeMap<String,List<Integer>>> lemmasFrequency = new TreeMap<>();
            for (String lemma : lemmas.keySet()){
                String sql = "SELECT * FROM lemma WHERE lemma = '" + lemma + "';";
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                int frequency = 0;
                ArrayList<Integer> ids = new ArrayList<>();
                while (resultSet.next()){
                    Integer id = resultSet.getInt("id");
                    LemmaSQL newLemma = new LemmaSQL();
                    newLemma.setLemma(resultSet.getString("lemma"));
                    newLemma.setId(id);
                    newLemma.setSite_id(resultSet.getInt("site_id"));
                    newLemma.setFrequency(resultSet.getInt("frequency"));
                    lemmasFounded.add(newLemma);
                    frequency += resultSet.getInt("frequency");
                    ids.add(id);
                }
                if (frequency == 0){
                    response.setCount(0);
                    response.setResult(true);
                    return response;
                }
                if (!lemmasFrequency.containsKey(frequency)){
                    TreeMap<String,List<Integer>> lemmaIds = new TreeMap<>();
                    lemmaIds.put(lemma, ids);
                    lemmasFrequency.put(frequency, lemmaIds);
                }
                lemmasFrequency.get(frequency).put(lemma, ids);
            }
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
                        response.setResult(true);
                        response.setCount(0);
                        return response;
                    }
                }
            }
            ArrayList<Pairs> orderedIds = new ArrayList<>();
            for (int i = 0; i < pageIds.size(); i++)
            {
                orderedIds.add(new Pairs(pageIds.get(i), pageRanks.get(pageIds.get(i))));
            }
            orderedIds.sort((Pairs p1, Pairs p2) -> -p1.getY().compareTo(p2.getY()));
            response.setCount(pageIds.size());
            for (int i = data.getOffset(); i < Math.min(pageIds.size(), data.getOffset() + data.getLimit()); i++){
                SearchInfo searchInfo = new SearchInfo();

                searchInfo.setRelevance((float)orderedIds.get(i).getY()/orderedIds.get(0).getY());
                String sql = "SELECT * FROM `page` WHERE id = '" + orderedIds.get(i).getX() + "';";
                ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
                if (resultSet.next()){
                    int site_id = resultSet.getInt("site_id") - 1;
                    searchInfo.setSite(sites.getSites().get(site_id).getUrl());
                    searchInfo.setSiteName(sites.getSites().get(site_id).getName());
                    String htmlContent = resultSet.getString("content");
                    Document doc = Jsoup.parse(htmlContent);
                    Elements elements = doc.select("title");
                    if (elements.size() > 0) {
                        searchInfo.setTitle(elements.get(0).text());
                    } else {
                        searchInfo.setTitle("");
                    }
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
