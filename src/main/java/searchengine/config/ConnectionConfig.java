package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.repository.*;

@Configuration
public class ConnectionConfig {
    @Bean
    public ConnectionHibernate ConnectionHibernate(PageRepository pageRepository, LemmaRepository lemmaRepository, SiteRepository siteRepository, IndexRepository indexRepository){
        return new ConnectionHibernate(pageRepository, lemmaRepository, siteRepository, indexRepository);
    }
}
