package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.ConnectionHibernate;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private static final ForkJoinPool pool = new ForkJoinPool();
    private final SitesList sites;
    private final ConnectionHibernate connectionHibernate;

    @Override
    public StatisticsResponse getStatistics() {
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
                int pages = connectionHibernate.countPages(i + 1);
                int lemmas = connectionHibernate.countLemmas(i + 1);
                item.setPages(pages);
                item.setLemmas(lemmas);
                searchengine.model.Site site1 = connectionHibernate.getSite(i + 1);
                item.setStatus(site1.getStatus().name());
                item.setStatusTime(site1.getStatus_time().getTime());
                item.setError(site1.getLast_error());
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
        if (SiteTreeBuilder.isIndexing()){
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            pool.execute(new SiteTreeBuilder(sites));
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
                    SiteTreeBuilder siteTreeBuilder = new SiteTreeBuilder();
                    siteTreeBuilder.updatePage(url, sites.getSites().indexOf(site1) + 1, doc);
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
        if (SiteTreeBuilder.isIndexing()){
            SiteTreeBuilder.stopIndexing();
            response.setResult(true);
            for (int i = 0; i<sites.getSites().size(); i++){
                searchengine.model.Site site = connectionHibernate.getSite(i + 1);
                if (site.getStatus() == Status.INDEXING){
                    site.setStatus(Status.FAILED);
                    site.setLast_error("Индексация остановлена пользователем");
                    connectionHibernate.updateSite(site);
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
        try {
            for (String lemma : lemmas.keySet()) {
                ArrayList<Lemma> lemmasFound
                        = (site_id != 0 ? connectionHibernate.getLemmas(lemma, site_id) : connectionHibernate.getLemmas(lemma));
                int frequency = 0;
                ArrayList<Integer> ids = new ArrayList<>();
                for (Lemma lemma1 : lemmasFound){
                    frequency += lemma1.getFrequency();
                    ids.add(lemma1.getId());
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
                        ArrayList<Index> indices = connectionHibernate.getIndices(id);
                        for (Index index : indices){
                            Integer page_id = index.getPage_id();
                            Integer lemma_rank = index.getLemma_rank();
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

    private SearchResponse getEmptyResponse(){
        SearchResponse response = new SearchResponse();
        response.setCount(0);
        response.setResult(true);
        return response;
    }

    private SearchInfo getSearchInfo(float relevance, String[] lemmas, Page page){
        SearchInfo searchInfo = new SearchInfo();
        try{
            PageParser parser = new PageParser();
            searchInfo.setRelevance(relevance);
            int site_id = page.getSite_id() - 1;
            searchInfo.setSite(sites.getSites().get(site_id).getUrl());
            searchInfo.setSiteName(sites.getSites().get(site_id).getName());
            String htmlContent = page.getContent();
            Document doc = Jsoup.parse(htmlContent);
            Elements elements = doc.select("title");
            searchInfo.setTitle(elements.size() > 0 ? elements.get(0).text() : "");
            searchInfo.setSnippet(parser.buildSnippet(lemmas, doc.text()));
            searchInfo.setUri(page.getPath().replaceFirst(sites.getSites().get(site_id).getUrl(),""));
            searchInfo.setSite(sites.getSites().get(site_id).getUrl());
            searchInfo.setSiteName(sites.getSites().get(site_id).getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return searchInfo;
    }

    @Override
    public SearchResponse search(SearchData data) {
        String text = data.getQuery();
        try{
            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(text);
            SearchResponse response = new SearchResponse();
            int site_id = 0;
            if (data.getSite() != null){
                site_id = connectionHibernate.getSiteIdByUrl(data.getSite());
            }
            TreeMap<Integer, TreeMap<String,List<Integer>>> lemmasFrequency = findLemmasFrequency(lemmas, site_id);
            if (lemmasFrequency == null){
                return getEmptyResponse();
            }
            ArrayList<Pairs> orderedIds = getSortedPageIds(lemmasFrequency);
            if (orderedIds == null){
                return getEmptyResponse();
            }
            response.setCount(orderedIds.size());
            for (int i = data.getOffset(); i < Math.min(orderedIds.size(), data.getOffset() + data.getLimit()); i++){
                Page page = connectionHibernate.getPage(orderedIds.get(i).getX());
                SearchInfo searchInfo = getSearchInfo((float)orderedIds.get(i).getY()/orderedIds.get(0).getY(),
                        lemmas.keySet().toArray(new String[0]), page);
                response.addSearchInfo(searchInfo);
            }
            response.setResult(true);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
