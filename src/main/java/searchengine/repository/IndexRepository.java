package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    @Query(value = "SELECT * FROM `index` where lemma_id = :lemmaId", nativeQuery = true)
    List<Index> getIndices(int lemmaId);
    @Query(value = "DELETE from index where page_id = :pageId", nativeQuery = true)
    void deleteIndexByPage(int pageId);
}
