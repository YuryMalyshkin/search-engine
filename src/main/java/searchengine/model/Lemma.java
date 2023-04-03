package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "lemma", uniqueConstraints = { @UniqueConstraint(columnNames = { "site_id", "lemma"}) })
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private Integer site_id;
    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;

    public Lemma(Integer id, Integer site_id, String lemma, Integer frequency) {
        this.id = id;
        this.site_id = site_id;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma(Integer site_id, String lemma, Integer frequency) {
        this.site_id = site_id;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSite_id() {
        return site_id;
    }

    public void setSite_id(Integer site_id) {
        this.site_id = site_id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }
}
