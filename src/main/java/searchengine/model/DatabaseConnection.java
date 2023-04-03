package searchengine.model;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface DatabaseConnection {
    int countPages(int siteId);
    int countLemmas(int siteId);
    Site getSite(int siteId);
    int getSiteIdByUrl(String siteUrl);
    Page getPage(int pageId);
    Page getPage(String url);
    ArrayList<Lemma> getLemmas(String lemma);
    ArrayList<Index> getIndices(int lemmaId);
    ArrayList<Lemma> getLemmas(String lemma, int site_id);
    void deleteLemmas(Set<String> lemmas, int pageId);
    int addPage(Page page);
    void resetIndex(List<searchengine.config.Site> sites);
    void updateSite(Site site);
    void updateSiteTime(int site_id);
    void setSiteIndexed(int siteId);
}
