# search-engine

This project implements backend for search engine based on frontend from https://github.com/sortedmap/searchengine.

Search engine provides search over text content (in Russian) of the sites listed in application.yaml. To do the search, the statistics related to lemmas (basic form of russian words) is kept in sql database on local server along with list of indexed pages and sites. For building list of lemmas out of text org.apache.lucene.morphology is used.

On main page (that opens at dashboard), the general statistics over sites is shown. This statistic includes the number of pages and lemmas on each site.
In management, the user could either start indexing/reindexing over all sites from the list, or index/reindex specific page from the site on the list. 
In search, user could search query over all sites or over specific site from the list.
