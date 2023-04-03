package searchengine.model;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Configuration
public class ConnectionConfig {
    @Bean
    public ConnectionHibernate ConnectionHibernate(PageRepository pageRepository, LemmaRepository lemmaRepository, SiteRepository siteRepository, IndexRepository indexRepository){
        return new ConnectionHibernate(pageRepository, lemmaRepository, siteRepository, indexRepository);
    }
}
