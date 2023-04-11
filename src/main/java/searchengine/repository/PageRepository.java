package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    @Query(value = "SELECT COUNT(*) from page where site_id = :siteId", nativeQuery = true)
    int countPages(int siteId);
    @Query(value = "SELECT * FROM page where path = :url", nativeQuery = true)
    List<Page> getPageByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE `page`", nativeQuery = true)
    void truncate();
}
