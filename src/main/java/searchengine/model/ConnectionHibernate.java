package searchengine.model;

import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConnectionHibernate implements DatabaseConnection{

    private PageRepository pageRepository;

    private LemmaRepository lemmaRepository;

    private SiteRepository siteRepository;

    private IndexRepository indexRepository;

    public ConnectionHibernate(PageRepository pageRepository, LemmaRepository lemmaRepository, SiteRepository siteRepository, IndexRepository indexRepository) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
    }

    public int countPages(int siteId){
        return pageRepository.countPages(siteId);
    }
    public int countLemmas(int siteId){
        return lemmaRepository.countLemmas(siteId);
    }
    public Site getSite(int siteId){
        return siteRepository.findById(siteId).get();
    }
    public int getSiteIdByUrl(String siteUrl){
        return siteRepository.getSiteByUrl(siteUrl).get(0).getId();
    }
    public Page getPage(int pageId){
        return pageRepository.findById(pageId).get();
    }
    public Page getPage(String url){
        return pageRepository.getPageByUrl(url).get(0);
    }
    public ArrayList<Lemma> getLemmas(String lemma) {
        return new ArrayList<>(lemmaRepository.getLemmas(lemma));
    }
    public ArrayList<Index> getIndices(int lemmaId){
        return new ArrayList<>(indexRepository.getIndices(lemmaId));
    }
    public ArrayList<Lemma> getLemmas(String lemma, int site_id){
        return new ArrayList<>(lemmaRepository.getLemmasFromSite(lemma, site_id));
    }
    public void deleteLemmas(Set<String> lemmas, int pageId){
        for (String lemma : lemmas){
            lemmaRepository.deleteLemmas(lemma);
        }
        indexRepository.deleteIndexByPage(pageId);
        lemmaRepository.deleteEmptyLemmas();
        pageRepository.deleteById(pageId);
    }
    public int addPage(Page page){
        return pageRepository.save(page).getId();
    }
    public void addLemmas(Map<String, Integer> lemmas, int site_id, int pageId){
        for (String lemma : lemmas.keySet()) {
            lemmaRepository.updateFrequency(site_id, lemma);
        }
        ArrayList<Lemma> changedLemmas = new ArrayList<>();
        for (String lemma : lemmas.keySet()) {
            Index index = new Index(pageId, lemmaRepository.getLemmasFromSite(lemma, site_id).get(0).getId(), lemmas.get(lemma));
            indexRepository.save(index);
        }

    }
    public void resetIndex(List<searchengine.config.Site> sites){
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        for (searchengine.config.Site site : sites){
            Site site1 = new Site();
            site1.setStatus(Status.INDEXING);
            site1.setUrl(site.getUrl());
            site1.setName(site.getName());
            site1.setStatus_time(Date.valueOf(date));
            site1.setLast_error("");
            siteRepository.save(site1);
        }
    }
    public void updateSite(Site site){
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        site.setStatus_time(Date.valueOf(date));
        siteRepository.save(site);
    }
    public void updateSiteTime(int site_id){
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        siteRepository.updateSiteTime(date, site_id);
    }
    public void setSiteIndexed(int siteId){
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        siteRepository.setSiteIndexed(date, Status.INDEXED.ordinal(), siteId);
    }

}
