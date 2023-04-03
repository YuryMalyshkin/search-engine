package searchengine.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Date status_time;

    private String last_error;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String url;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getStatus_time() {
        return status_time;
    }

    public void setStatus_time(Date status_time) {
        this.status_time = status_time;
    }

    public String getLast_error() {
        return last_error;
    }

    public void setLast_error(String last_error) {
        this.last_error = last_error;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
