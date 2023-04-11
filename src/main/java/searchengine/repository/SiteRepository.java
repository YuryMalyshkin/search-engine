package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {

    @Query(value = "SELECT * FROM site WHERE url = :siteUrl", nativeQuery = true)
    List<Site> getSiteByUrl(String siteUrl);
    @Modifying
    @Transactional
    @Query(value = "UPDATE site SET status_time = :date WHERE id = :site_id", nativeQuery = true)
    void updateSiteTime(String date, int site_id);
    @Modifying
    @Transactional
    @Query(value = "UPDATE site SET status_time = :date, status = :status WHERE id = :site_id", nativeQuery = true)
    void setSiteIndexed(String date, int status, int site_id);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE `site`", nativeQuery = true)
    void truncate();
}
