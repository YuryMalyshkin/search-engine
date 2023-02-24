package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "lemma_index")
public class IndexSQL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private Integer page_id;
    @Column(nullable = false)
    private Integer lemma_id;
    @Column(nullable = false)
    private Integer lemma_rank;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPage_id() {
        return page_id;
    }

    public void setPage_id(Integer page_id) {
        this.page_id = page_id;
    }

    public Integer getLemma_id() {
        return lemma_id;
    }

    public void setLemma_id(Integer lemma_id) {
        this.lemma_id = lemma_id;
    }

    public Integer getLemma_rank() {
        return lemma_rank;
    }

    public void setLemma_rank(Integer lemma_rank) {
        this.lemma_rank = lemma_rank;
    }
}
