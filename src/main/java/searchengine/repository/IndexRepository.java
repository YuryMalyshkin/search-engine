package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    @Query(value = "SELECT * FROM `index` where lemma_id = :lemmaId", nativeQuery = true)
    List<Index> getIndices(int lemmaId);
    @Query(value = "DELETE from index where page_id = :pageId", nativeQuery = true)
    void deleteIndexByPage(int pageId);
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE `index`", nativeQuery = true)
    void truncate();
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO `index` (`page_id`, `lemma_id`, `lemma_rank`) VALUES (:page_id, :lemma_id, :lemma_rank)", nativeQuery = true)
    void saveIndex(Integer page_id, Integer lemma_id, Integer lemma_rank);

}
