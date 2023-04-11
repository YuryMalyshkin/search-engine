package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    @Query(value = "SELECT COUNT(*) from lemma where  site_id = :siteId", nativeQuery = true)
    int countLemmas(int siteId);
    @Query(value = "SELECT * FROM lemma WHERE lemma = :word", nativeQuery = true)
    List<Lemma> getLemmas(String word);
    @Query(value = "SELECT * FROM lemma WHERE lemma = :word AND  site_id = :siteId", nativeQuery = true)
    List<Lemma> getLemmasFromSite(String word, int siteId);
    @Query(value = "SELECT * FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    List<Lemma> getLemmasFromSite(int siteId);
    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma set frequency = frequency - 1 where lemma = :word", nativeQuery = true)
    void deleteLemmas(String word);
    @Query(value = "DELETE from lemma where frequency = 0", nativeQuery = true)
    void deleteEmptyLemmas();
    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma SET frequency = frequency + 1 WHERE id = :lemma_id", nativeQuery = true)
    void updateFrequency(int lemma_id);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO `lemma`(`site_id`, `lemma`, `frequency`) VALUES (:site_id, :lemma, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1", nativeQuery = true)
    void updateFrequency(int site_id, String lemma);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE `lemma`", nativeQuery = true)
    void truncate();
}
