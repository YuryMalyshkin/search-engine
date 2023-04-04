package searchengine.repository;


import searchengine.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConnectionSQL implements DatabaseConnection{
    private final static String dbName = "search_engine";
    private final static String dbUser = "root";
    private final static String dbPass = "testtest";

    private static Connection connection;
    public static Connection connect() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
    public int countPages(int siteId){
        int pages = 0;
        try {
            String sql = "SELECT COUNT(*) FROM page WHERE site_id =" + siteId + ";";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()) {
                pages = resultSet.getInt(1);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return pages;
    }
    public int countLemmas(int siteId){
        int lemmas = 0;
        try {
            String sql = "SELECT COUNT(*) FROM lemma WHERE site_id =" + siteId + ";";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()) {
                lemmas = resultSet.getInt(1);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return lemmas;
    }
    public Site getSite(int siteId){
        Site site = new Site();
        try{
            String sql = "SELECT status, status_time, last_error, url FROM site WHERE id =" + siteId + ";";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()){
                site.setStatus(Status.values()[resultSet.getInt(1)]);
                site.setStatus_time(resultSet.getTimestamp(2));
                site.setLast_error(resultSet.getString(3) == null ? "" : resultSet.getString(3));
                site.setId(siteId);
                site.setUrl(resultSet.getString(4));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return site;
    }
    public int getSiteIdByUrl(String siteUrl){
        try{
            String sql = "SELECT * FROM site WHERE url = '" + siteUrl + "';";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()){
                return resultSet.getInt("id");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public Page getPage(int pageId){
        Page page = new Page();
        try{
            String sql = "SELECT * FROM page WHERE id =" + pageId + ";";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()){
                page.setCode(resultSet.getInt("code"));
                page.setSite_id(resultSet.getInt("site_id"));
                page.setId(pageId);
                page.setPath(resultSet.getString("path"));
                page.setContent(resultSet.getString("content"));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return page;
    }

    public Page getPage(String url){
        Page page = new Page();
        try{
            String sql = "SELECT * FROM `page` WHERE path = '" + url + "';";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            if (resultSet.next()){
                page.setCode(resultSet.getInt("code"));
                page.setSite_id(resultSet.getInt("site_id"));
                page.setId(resultSet.getInt("id"));
                page.setPath(resultSet.getString("path"));
                page.setContent(resultSet.getString("content"));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return page;
    }


    public ArrayList<Lemma> getLemmas(String lemma){
        ArrayList<Lemma> lemmas = new ArrayList<>();
        try{
            String sql = "SELECT * FROM lemma WHERE lemma = '" + lemma + "';";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            while (resultSet.next()) {
                Lemma newLemma = new Lemma(resultSet.getInt("id"), resultSet.getInt("site_id"),
                        resultSet.getString("lemma"), resultSet.getInt("frequency"));
                lemmas.add(newLemma);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return lemmas;
    }
    public ArrayList<Index> getIndices(int lemmaId){
        ArrayList<Index> indices = new ArrayList<>();
        try{
            String sql = "SELECT * FROM `index` WHERE lemma_id = " + lemmaId + ";";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            while (resultSet.next()) {
                Index index = new Index(resultSet.getInt("id"), resultSet.getInt("page_id"),
                        resultSet.getInt("lemma_id"), resultSet.getInt("lemma_rank"));
                indices.add(index);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return indices;
    }

    public ArrayList<Lemma> getLemmas(String lemma, int site_id){
        ArrayList<Lemma> lemmas = new ArrayList<>();
        try{
            String sql = "SELECT * FROM lemma WHERE lemma = '" + lemma + "' AND site_id = '" + site_id + "';";
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            while (resultSet.next()) {
                Lemma newLemma = new Lemma(resultSet.getInt("id"), resultSet.getInt("site_id"),
                        resultSet.getString("lemma"), resultSet.getInt("frequency"));
                lemmas.add(newLemma);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return lemmas;
    }

    public void deleteLemmas(Set<String> lemmas, int pageId){
        try{
            StringBuilder lemmaBuilder = new StringBuilder();
            for (String lemma : lemmas){
                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : " OR ") +
                        " lemma = '" + lemma + "'");
            }
            String sql =  "UPDATE `lemma` SET frequency = frequency - 1 WHERE " +
                    lemmaBuilder.toString() + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "DELETE FROM `index` WHERE page_id = " + pageId + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "DELETE FROM `lemma` WHERE frequency = 0;";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "DELETE FROM `page` WHERE id = " + pageId + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

//    private static void deletePage(String url){
//        try{
//            Page page = ConnectionSQL.getPage(url);
////            String sql = "SELECT * FROM `page` WHERE path = '" + url + "';";
////            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
////            while (resultSet.next()){
//            PageParser parser = new PageParser();
////            Map<String, Integer> lemmas = parser.collectLemmas(resultSet.getString("content"));
//            Map<String, Integer> lemmas = parser.collectLemmas(page.getContent());
//            ConnectionSQL.deleteLemmas(lemmas.keySet(), page.getId());
////            StringBuilder lemmaBuilder = new StringBuilder();
////            for (String lemma : lemmas.keySet()){
////                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : " OR ") +
////                            " lemma = '" + lemma + "'");
////                }
////                sql =  "UPDATE `lemma` SET frequency = frequency - 1 WHERE " +
////                        lemmaBuilder.toString() + ";";
////                ConnectionSQL.connect().createStatement().execute(sql);
////                sql = "DELETE FROM `index` WHERE page_id = " + resultSet.getInt("id") + ";";
////                ConnectionSQL.connect().createStatement().execute(sql);
////                sql = "DELETE FROM `lemma` WHERE frequency = 0;";
////                ConnectionSQL.connect().createStatement().execute(sql);
////                sql = "DELETE FROM `page` WHERE id = " + resultSet.getInt("id") + ";";
////                ConnectionSQL.connect().createStatement().execute(sql);
////            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static void updatePage(String url, Integer site_id, Document doc){
//        deletePage(url);
//        addNewPage(url, site_id, doc);
//    }
//    public static void addNewPage(String url, Integer site_id, Document doc){
//        String htmlContent = doc.outerHtml();
//        Page page = new Page(site_id, url, 200, htmlContent);
//
//        String content = doc.text();
//
//        int pageId = ConnectionSQL.addPage(page);
//        try{
//            PageParser parser = new PageParser();
//            Map<String, Integer> lemmas = parser.collectLemmas(content);
//            ConnectionSQL.addLemmas(lemmas, site_id, pageId);
//            ConnectionSQL.updateSiteTime(site_id);
////            String sql = "INSERT INTO `page` (`site_id`, `path`, `code`, `content`) VALUES (?,?,?,?);";
////            PreparedStatement pageInsert = ConnectionSQL.connect().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
////            pageInsert.setInt(1, site_id);
////            pageInsert.setString(2, url);
////            pageInsert.setInt(3, 200);
////            pageInsert.setString(4, htmlContent);
//
////            pageInsert.execute();
////            ResultSet pageId = pageInsert.getGeneratedKeys();
////            pageId.next();
//
////            StringBuilder lemmaBuilder = new StringBuilder();
////            StringBuilder indexBuilder = new StringBuilder();
////            for (String lemma : lemmas.keySet()) {
////                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : ",") +
////                        "('"  + site_id + "', '" + lemma + "', 1)");
////            }
////            String sql =  "INSERT INTO `lemma`(`site_id`, `lemma`, `frequency`) VALUES" +
////                    lemmaBuilder.toString() +
////                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1";
////            ConnectionSQL.connect().createStatement().execute(sql);
////            lemmaBuilder = new StringBuilder();
////            for (String lemma : lemmas.keySet()){
////                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "(" : " OR ") +
////                        "lemma = '" + lemma + "'");
////            }
////            lemmaBuilder.append(");");
////            sql = "SELECT * FROM `lemma` WHERE site_id = '" + site_id + "' AND " + lemmaBuilder;
////            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
////            Map<String, Integer> lemmaIDs = new TreeMap<>();
////            while (resultSet.next()){
////                lemmaIDs.put(resultSet.getString("lemma"), resultSet.getInt("id"));
////            }
//
////            for (String lemma : lemmas.keySet()){
////                indexBuilder.append((indexBuilder.length() == 0 ? "" : ",") +
////                        "('" + pageId + "', '" +  lemmaIDs.get(lemma) + "', '"  + lemmas.get(lemma) + "')");
////            }
//
////            sql = "INSERT INTO `index` (`page_id`, `lemma_id`, `lemma_rank`) VALUES" +
////                    indexBuilder.toString();
////            ConnectionSQL.connect().createStatement().execute(sql);
////            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
////            sql = "UPDATE `site` SET status_time ='" + date + "' WHERE id = " + site_id + ";";
////            ConnectionSQL.connect().createStatement().execute(sql);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    public int addPage(Page page){
        try{
            String sql = "INSERT INTO `page` (`site_id`, `path`, `code`, `content`) VALUES (?,?,?,?);";
            PreparedStatement pageInsert = ConnectionSQL.connect().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pageInsert.setInt(1, page.getSite_id());
            pageInsert.setString(2, page.getPath());
            pageInsert.setInt(3, 200);
            pageInsert.setString(4, page.getContent());
            pageInsert.execute();
            ResultSet pageId = pageInsert.getGeneratedKeys();
            pageId.next();
            return  pageId.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void addLemmas(Map<String, Integer> lemmas, int site_id, int pageId){
        try{
            ArrayList<Lemma> savedLemmas= new ArrayList<>();
            StringBuilder lemmaBuilder = new StringBuilder();
            for (String lemma : lemmas.keySet()) {
                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "" : ",") +
                        "('"  + site_id + "', '" + lemma + "', 1)");
            }
            String sql =  "INSERT INTO `lemma`(`site_id`, `lemma`, `frequency`) VALUES" +
                    lemmaBuilder.toString() +
                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1";
            ConnectionSQL.connect().createStatement().execute(sql);
            lemmaBuilder = new StringBuilder();
            for (String lemma : lemmas.keySet()){
                lemmaBuilder.append((lemmaBuilder.length() == 0 ? "(" : " OR ") +
                        "lemma = '" + lemma + "'");
            }
            lemmaBuilder.append(");");
            sql = "SELECT * FROM `lemma` WHERE site_id = '" + site_id + "' AND " + lemmaBuilder;
            ResultSet resultSet = ConnectionSQL.connect().createStatement().executeQuery(sql);
            Map<String, Integer> lemmaIDs = new TreeMap<>();
            while (resultSet.next()){
                lemmaIDs.put(resultSet.getString("lemma"), resultSet.getInt("id"));
            }
            StringBuilder indexBuilder = new StringBuilder();
            for (String lemma : lemmas.keySet()){
                indexBuilder.append((indexBuilder.length() == 0 ? "" : ",") +
                        "('" + pageId + "', '" +  lemmaIDs.get(lemma) + "', '"  + lemmas.get(lemma) + "')");
            }
            sql = "INSERT INTO `index` (`page_id`, `lemma_id`, `lemma_rank`) VALUES" +
                    indexBuilder.toString();
            ConnectionSQL.connect().createStatement().execute(sql);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void resetIndex(List<searchengine.config.Site> sites){
        try{
            String sql = "TRUNCATE TABLE `page`";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "TRUNCATE TABLE `site`";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "TRUNCATE TABLE `lemma`";
            ConnectionSQL.connect().createStatement().execute(sql);
            sql = "TRUNCATE TABLE `index`";
            ConnectionSQL.connect().createStatement().execute(sql);
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            for (searchengine.config.Site site : sites){
                sql = "INSERT INTO `site` (`status`, `status_time`, `url`, `name`) VALUES ('"
                        + Status.INDEXING.ordinal() + "', '" + date
                        + "', '" + site.getUrl() +
                        "', '" + site.getName() +"');";
                ConnectionSQL.connect().createStatement().execute(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSite(Site site){
        try{
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String sql = "UPDATE `site` SET status_time ='" + date + "', status = " + site.getStatus().ordinal()
                    + "', error = " + site.getLast_error() + " WHERE id = " + site.getId() + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateSiteTime(int site_id){
        try{
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String sql = "UPDATE `site` SET status_time ='" + date + "' WHERE id = " + site_id + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSiteIndexed(int siteId){
        try{
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String sql = "UPDATE `site` SET status_time ='" + date + "', status = " + Status.INDEXED.ordinal()
                    + " WHERE id = " + siteId + ";";
            ConnectionSQL.connect().createStatement().execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
