package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "page", uniqueConstraints = { @UniqueConstraint(columnNames = { "site_id", "path"}) })
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private Integer site_id;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    public Page(Integer site_id, String path, Integer code, String content) {
        this.site_id = site_id;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public Page() {
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
