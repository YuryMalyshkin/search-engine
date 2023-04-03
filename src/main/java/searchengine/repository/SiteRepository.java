package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {

    @Query(value = "SELECT * FROM site WHERE url = :siteUrl", nativeQuery = true)
    List<Site> getSiteByUrl(String siteUrl);
    @Query(value = "UPDATE site SET status_time = :date WHERE id = :site_id", nativeQuery = true)
    List<Site> updateSiteTime(String date, int site_id);
    @Query(value = "UPDATE site SET status_time = :date, status = :status WHERE id = :site_id", nativeQuery = true)
    List<Site> setSiteIndexed(String date, int status, int site_id);
}
