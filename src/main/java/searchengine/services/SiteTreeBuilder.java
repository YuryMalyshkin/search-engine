package searchengine.services;


import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repository.ConnectionHibernate;
import searchengine.repository.ConnectionSQL;
import searchengine.model.Page;
import searchengine.repository.DatabaseConnection;

import java.util.*;
import java.util.concurrent.RecursiveTask;

@Service
public class SiteTreeBuilder extends RecursiveTask<TreeSet<String>> {
    private final String rootSiteName;

    private static boolean isIndexing = false;

    private static final List<Site> sites = new ArrayList<>();

    private static Integer currentSite = 0;

    private static Integer activeTasks = 0;

    private static Map<String, Integer> lemmasID = new HashMap<>();

    private static final TreeSet<String> subSites = new TreeSet<>();

    private final String currentSiteName;

    private final Integer site_id;

    //private final ConnectionSQL connection = new ConnectionSQL();
    private DatabaseConnection connection;
    public SiteTreeBuilder() {
        currentSite = 0;
        site_id = 1;
        rootSiteName = "";
        currentSiteName = rootSiteName;
    }
    public SiteTreeBuilder(DatabaseConnection connection) {
        currentSite = 0;
        site_id = 1;
        rootSiteName = "";
        currentSiteName = rootSiteName;
        this.connection = connection;
    }
    public SiteTreeBuilder(SitesList siteNames, DatabaseConnection connection){
        this.connection = connection;
        sites.clear();
        subSites.clear();
        sites.addAll(siteNames.getSites());
        startIndexing();
        currentSite = 0;
        site_id = 1;
        rootSiteName = sites.get(currentSite).getUrl();
        currentSiteName = rootSiteName;
        addSubSite(rootSiteName);
        addSubSite("");
    }
    public SiteTreeBuilder(String rootSiteName, DatabaseConnection connection) {
        this.rootSiteName = rootSiteName;
        currentSiteName = rootSiteName;
        addSubSite(rootSiteName);
        addSubSite("");
        site_id = currentSite + 1;
        this.connection = connection;
    }

    public SiteTreeBuilder(String rootSiteName, String currentSiteName, Integer site_id, DatabaseConnection connection) {
        this.rootSiteName = rootSiteName;
        this.currentSiteName = currentSiteName;
        this.site_id = site_id;
        this.connection = connection;
    }

    public static boolean isIndexing(){
        return isIndexing;
    }
    public static void stopIndexing(){
        isIndexing = false;
    }
    private static synchronized int addLemma (String lemma){
        if (lemmasID.containsKey(lemma)){
            return lemmasID.get(lemma);
        }
        lemmasID.put(lemma,lemmasID.size());
        return lemmasID.size();
    }

    private synchronized boolean startIndexing(){
        if (!isIndexing()){
            connection.resetIndex(sites);
            isIndexing = true;
            return true;
        }
        return false;
    }


    private static synchronized boolean addSubSite(String subSite){
        return subSites.add(subSite);
    }
    private void deletePage(String url){
        try{
            Page page = connection.getPage(url);
            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(page.getContent());
            connection.deleteLemmas(lemmas.keySet(), page.getId());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updatePage(String url, Integer site_id, Document doc){
        deletePage(url);
        addNewPage(url, site_id, doc);
    }
    public void addNewPage(String url, Integer site_id, Document doc){
        String htmlContent = doc.outerHtml();
        Page page = new Page(site_id, url, 200, htmlContent);

        String content = doc.text();

        int pageId = connection.addPage(page);
        try{
            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(content);
            connection.addLemmas(lemmas, site_id, pageId);
            connection.updateSiteTime(site_id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCorrectSiteName (String name){
        String regex1 = rootSiteName + "[[^\\s,/]+/]+/";
        String regex2 = "/" + "[[^\\s,/]+/]+";
        if (name.matches(regex1)){
            return name;
        } else if (name.matches(regex2)){
            return rootSiteName + (rootSiteName.endsWith("/") ? "" : "/") + name.replaceFirst("/","");
        } else {
            return null;
        }
    }

    @Override
    protected TreeSet<String> compute() {
        if (!isIndexing()){
            return subSites;
        }
        try {
            Thread.sleep(100);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        Document doc;
        try {
            doc = Jsoup.connect(currentSiteName).get();
        }
        catch (Exception ex){
            return null;
        }

        addNewPage(currentSiteName, site_id, doc);

        List<SiteTreeBuilder> taskList = new ArrayList<>();

        Elements sourceLines = doc.select("a");
        String regex1 = rootSiteName + "[[^\\s,/]+/]+/";
        String regex2 = "/" + "[[^\\s,/]+/]+";

        sourceLines.forEach(element -> {
            String s = getCorrectSiteName(element.attr("href"));
            if (s == null){
                return;
            }
            if (addSubSite(s)){
                SiteTreeBuilder task = new SiteTreeBuilder(rootSiteName, s, site_id, connection);
                synchronized (activeTasks) {
                    activeTasks++;
                }
                task.fork();
                taskList.add(task);
            }
        });
        for (SiteTreeBuilder task : taskList){
            synchronized (activeTasks) {
                activeTasks--;
            }
            task.join();
        }
        if (activeTasks == 0){
            connection.setSiteIndexed(currentSite + 1);
            lemmasID = new HashMap<>();
            if (currentSite < sites.size() - 1) {
                currentSite++;
                SiteTreeBuilder task = new SiteTreeBuilder(sites.get(currentSite).getUrl(), connection);
                task.fork();
                task.join();
            }
        }
        return subSites;
    }
}
