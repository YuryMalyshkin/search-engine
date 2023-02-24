package searchengine.services;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Status;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class SiteTree extends RecursiveTask<TreeSet<String>> {
    private final String rootSiteName;

    private static boolean isIndexing = false;

    private static final List<Site> sites = new ArrayList<>();

    private static Integer currentSite = 0;

    private static Integer activeTasks = 0;

    private static Map<String, Integer> lemmasID = new HashMap<>();

    private static final TreeSet<String> subSites = new TreeSet<>();

    private final String currentSiteName;


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
            try{
                String sql = "TRUNCATE TABLE `page`";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "TRUNCATE TABLE `site`";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "TRUNCATE TABLE `lemma`";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "TRUNCATE TABLE `lemma_index`";
                ConnectionService.connect().createStatement().execute(sql);
                String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                for (Site site : sites){
                    sql = "INSERT INTO `site` (`status`, `status_time`, `url`, `name`) VALUES ('"
                            + Status.INDEXING.ordinal() + "', '" + date
                             + "', '" + site.getUrl() +
                            "', '" + site.getName() +"');";
                    ConnectionService.connect().createStatement().execute(sql);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isIndexing = true;
            return true;
        }
        return false;
    }

    public SiteTree(SitesList siteNames){
        sites.clear();
        subSites.clear();
        sites.addAll(siteNames.getSites());
        startIndexing();
        currentSite = 0;
        rootSiteName = sites.get(currentSite).getUrl();
        currentSiteName = rootSiteName;
        addSubSite(rootSiteName);
        addSubSite("");
    }
    public SiteTree(String rootSiteName) {
        this.rootSiteName = rootSiteName;
        currentSiteName = rootSiteName;
        addSubSite(rootSiteName);
        addSubSite("");
    }

    public SiteTree(String rootSiteName, String currentSiteName) {
        this.rootSiteName = rootSiteName;
        this.currentSiteName = currentSiteName;
    }
    private static synchronized boolean addSubSite(String subSite){
        return subSites.add(subSite);
    }

    private static void deletePage(String url){
        try{
            String sql = "SELECT * FROM `page` WHERE path = '" + url + "';";
            ResultSet resultSet = ConnectionService.connect().createStatement().executeQuery(sql);
            while (resultSet.next()){
                PageParser parser = new PageParser();
                Map<String, Integer> lemmas = parser.collectLemmas(resultSet.getString("content"));
                StringBuilder lemmaBuilder = new StringBuilder();
                for (String lemma : lemmas.keySet()){
                    lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : " OR ") +
                            " lemma = '" + lemma + "'");
                }
                sql =  "UPDATE `lemma` SET frequency = frequency - 1 WHERE " +
                        lemmaBuilder.toString() + ";";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "DELETE FROM `lemma_index` WHERE page_id = " + resultSet.getInt("id") + ";";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "DELETE FROM `lemma` WHERE frequency = 0;";
                ConnectionService.connect().createStatement().execute(sql);
                sql = "DELETE FROM `page` WHERE id = " + resultSet.getInt("id") + ";";
                ConnectionService.connect().createStatement().execute(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updatePage(String url, Integer site_id, Document doc){
        deletePage(url);
        addNewPage(url, site_id, doc);
    }

    private static void addNewPage(String url, Integer site_id, Document doc){
        String htmlContent = doc.outerHtml();

        String content = doc.text();
        try{
            String sql = "INSERT INTO `page` (`site_id`, `path`, `code`, `content`) VALUES (?,?,?,?);";
            PreparedStatement pageInsert = ConnectionService.connect().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pageInsert.setInt(1, site_id);
            pageInsert.setString(2, url);
            pageInsert.setInt(3, 200);
            pageInsert.setString(4, htmlContent);

            pageInsert.execute();
            ResultSet pageId = pageInsert.getGeneratedKeys();
            pageId.next();

            PageParser parser = new PageParser();
            Map<String, Integer> lemmas = parser.collectLemmas(content);
            StringBuilder lemmaBuilder = new StringBuilder();
            StringBuilder indexBuilder = new StringBuilder();
            for (String lemma : lemmas.keySet()){
                int id = addLemma(lemma);
                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : ",") +
                        "('" + id + "', '" + site_id + "', '" +  lemma + "', 1)");
                indexBuilder.append((indexBuilder.length() == 0 ? "" : ",") +
                        "('" + pageId.getInt(1) + "', '" +  id + "', '"  + lemmas.get(lemma) + "')");
            }
            sql =  "INSERT INTO `lemma`(`id`, `site_id`, `lemma`, `frequency`) VALUES" +
                    lemmaBuilder.toString() +
                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1";
            ConnectionService.connect().createStatement().execute(sql);
            sql = "INSERT INTO `lemma_index` (`page_id`, `lemma_id`, `lemma_rank`) VALUES" +
                    indexBuilder.toString();
            ConnectionService.connect().createStatement().execute(sql);
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            sql = "UPDATE `site` SET status_time ='" + date + "' WHERE id = " + site_id + ";";
            ConnectionService.connect().createStatement().execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
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

        addNewPage(currentSiteName, currentSite + 1, doc);

        List<SiteTree> taskList = new ArrayList<>();

        Elements sourceLines = doc.select("a");
        String regex1 = rootSiteName + "[[^\\s,/]+/]+/";
        String regex2 = "/" + "[[^\\s,/]+/]+";

        sourceLines.forEach(element -> {
            String s;
            if (element.attr("href").matches(regex1)){
                s = element.attr("href");
            } else if (element.attr("href").matches(regex2)){
                s = rootSiteName + "/" + element.attr("href").replaceFirst("/","");
            } else {
                return;
            }
            if (addSubSite(s)){
                SiteTree task = new SiteTree(rootSiteName, s);
                synchronized (activeTasks) {
                    activeTasks++;
                }
                task.fork();
                taskList.add(task);
            }
        });
        for (SiteTree task : taskList){
            synchronized (activeTasks) {
                activeTasks--;
            }
            task.join();
        }
        if (activeTasks == 0){
            try{
                String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                String sql = "UPDATE `site` SET status_time ='" + date + "', status = " + Status.INDEXED.ordinal()
                        + " WHERE id = " + (currentSite + 1) + ";";
                ConnectionService.connect().createStatement().execute(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lemmasID = new HashMap<>();
            if (currentSite < sites.size() - 1) {
                currentSite++;
                SiteTree task = new SiteTree(sites.get(currentSite).getUrl());
                task.fork();
                task.join();
            }
        }
        return subSites;
    }
}
