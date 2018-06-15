/*
 * Copyright 2016 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.masi.plugin.search.impl;

import edu.kit.masi.plugin.AbstractServicePlugin;
import edu.kit.masi.plugin.search.ISearchPlugin.Combination;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.kit.masi.plugin.search.ISearchPlugin;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.configuration.Configuration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * Plugin implementing search queries for elasticsearch.
 *
 * @author hartmann-v
 */
public class ElasticsearchServicePlugin extends AbstractServicePlugin implements ISearchPlugin {

  /**
   * The logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchServicePlugin.class);

  /**
   * Name of the service.
   */
  private static final String SERVICE_NAME = "elasticsearch";
  /**
   * Key for hostname.
   */
  private static final String KEY_HOSTNAME = "hostname";
  /**
   * Key for port.
   */
  private static final String KEY_PORT = "port";
  /**
   * Key for cluster name. default: KITDataManager@hostname
   */
  private static final String KEY_CLUSTER_NAME = "clustername";
  /**
   * Key for elasticsearch index. default: kitdatamanager
   */
  private static final String KEY_INDEX = "index";

  private static final String ALL_INDICES = "_all";
  /**
   * Cluster name.
   */
  String cluster;
  /**
   * Hostname.
   */
  String host;
  /**
   * Port
   */
  int port;
  /**
   * Map with all clients.
   */
  private final static Map<String, TransportClient> ALL_CLIENTS = new HashMap<>();
  /**
   * Index used for search.
   */
  String index = ALL_INDICES;

  /**
   * Constructor.
   */
  public ElasticsearchServicePlugin() {
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  public void configureService(Configuration pConfig) {
    host = pConfig.getString(KEY_HOSTNAME);
    port = pConfig.getInt(KEY_PORT);
    cluster = pConfig.getString(KEY_CLUSTER_NAME);
    index = pConfig.getString(KEY_INDEX);
    LOGGER.debug("Configure elasticsearch: url: {}:{}\ncluster name: {}, index: {}", host, port, cluster, index);
  }

  @Override
  public String[] searchForMets(Combination pCombination, String[] pValues) {

    return searchForMets(pCombination, null, pValues);
  }

  @Override
  public String[] searchForMets(Combination pCombination, String[] types, String[] pValues) {
    String[] indices = {ALL_INDICES};
    return searchForMets(pCombination, indices, types, pValues);
  }

  /**
   * Full text search for given search terms. Disjunction: At least one of the
   * search terms has to fit. Conjunction: All search terms have to fit.
   *
   * @param pCombination dis- or conjunction
   * @param indices Restrict to given indices.
   * @param types Restrict to given types of documents
   * @param pValues Search terms.
   * @return IDs of all fitting documents.
   */
  public String[] searchForMets(Combination pCombination, String[] indices, String[] types, String[] pValues) {
    boolean validSearchTerm = false;
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Search for metadata: " + pCombination + ", Indices: " + String.join(", ", indices) + ", Types: " + String.join(", ", types) + ", Terms: " + String.join(", ", pValues));
    }

    Client client = getTransportClient(host, port, cluster);
    BoolQueryBuilder query2 = QueryBuilders.boolQuery();
    int minimumNumber = 0;
    for (String term : pValues) {
      term = term.toLowerCase();
      for (String value : term.split("[^a-z0-9äöüß]+")) {
        if (value.length() > 2) {
          LOGGER.info("Search for regexp: '.*{}.*'", value);
          if (pCombination == Combination.CONJUNCTION) {
            minimumNumber++;
          }
          query2.should(QueryBuilders.regexpQuery("_all", ".*" + value + ".*"));
          validSearchTerm = true;
        } else {
          LOGGER.info("Term '{}' is to short: term skipped!", value);
        }
      }
    }
    if (pCombination == Combination.DISJUNCTION) {
      minimumNumber = 1;
    }
    query2.minimumNumberShouldMatch(minimumNumber);
//    return search(types, query2.toString());
    Set<String> results = new HashSet<>();
    if (validSearchTerm) {
      SearchRequestBuilder prepareSearch = client.prepareSearch(indices);
      SearchResponse searchResponse;
      int pageIndex = 0;
      int pageSize = 1000;
      long maxNumberOfHits = 10000;
      long maxNumberOfReturnedHits = 10000;
      long totalNumberOfHits;
      // Add pagination as the number of results is limited to 10 by default and
      // the maximum number is limited to 1000.
      do {
        searchResponse = prepareSearch.setSearchType(SearchType.DEFAULT).setQuery(query2).setSize(pageSize).setFrom(pageIndex * pageSize).execute().actionGet();
        totalNumberOfHits = searchResponse.getHits().getTotalHits();
        if (totalNumberOfHits > maxNumberOfHits) {
          LOGGER.warn("Number of results is cut from  '{}' to '{}'!", totalNumberOfHits, maxNumberOfHits);
          totalNumberOfHits = maxNumberOfHits;
        }
        LOGGER.debug("Estimated number of results: '{}' [Retrieve index {} - {}]!", totalNumberOfHits, pageIndex * pageSize, (pageIndex + 1) * pageSize);
        pageIndex++;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
          String elasticSearchId = hit.id();
          String[] partsOfId;
          partsOfId = elasticSearchId.split("_", 2);
          results.add(partsOfId[0]);
          if (results.size() >= maxNumberOfReturnedHits) {
            break;
          }
          LOGGER.debug("Found DigitalObject with id: '{}' in index '{}'", partsOfId[0], partsOfId[1]);
          if (LOGGER.isTraceEnabled()) {
            Map<String, Object> source = hit.getSource();
            LOGGER.trace("ID: " + hit.id());
            for (String key : source.keySet()) {
              LOGGER.trace(key + ": " + source.get(key));
            }
          }
        }
        if (results.size() >= maxNumberOfReturnedHits) {
          break;
        }
      } while (totalNumberOfHits > pageIndex * pageSize);
    }
    LOGGER.debug("Found '{}' results!", results.size());
    return results.toArray(new String[results.size()]);
  }

  @Override
  public String[] search(String query) {
    return search(null, query);
  }

  @Override
  public String[] search(String[] types, String query) {
    LOGGER.warn("Search with proprietary query is not working yet!");
    LOGGER.debug("Query elasticsearch: " + query);
    Client client = getTransportClient(host, port, cluster);
    SearchRequestBuilder prepareSearch = client.prepareSearch(index);
    if (types != null) {
      prepareSearch.setTypes(types);
    }
    String completeQuery = query; //queryString.format("{ \"from\" : %d, \"size\" : %d, \"stored_fields\" : [], %s }", 0, 1000, query);
    LOGGER.debug("Query elasticsearch: " + completeQuery);
    LOGGER.debug(QueryBuilders.wrapperQuery(completeQuery).buildAsBytes().utf8ToString());
    SearchResponse searchResponse = prepareSearch.setSearchType(SearchType.DEFAULT).setQuery(QueryBuilders.wrapperQuery(completeQuery)).execute().actionGet();
    List<String> results = new ArrayList<>();
    LOGGER.debug("Estimated number of results: '{}'!", searchResponse.getHits().getTotalHits());
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      Map<String, Object> source = hit.getSource();
      for (String key : source.keySet()) {
        LOGGER.debug(key + ": " + source.get(key));
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(key + ": " + source.get(key));
        }
        results.add((source.get(key).toString()));
      }
    }
    LOGGER.debug("Found '{}' results!", results.size());
    return results.toArray(new String[results.size()]);
  }

  /**
   * Test the plugin.
   *
   * @param args search terms.
   */
  public static void main(String[] args) {

//    try {
    ElasticsearchServicePlugin elasticsearchServicePlugin = new ElasticsearchServicePlugin();
    elasticsearchServicePlugin.host = "localhost";
    elasticsearchServicePlugin.port = 9300;
    elasticsearchServicePlugin.cluster = "kitdatamanager";

//    String[] searchForMets = elasticsearchServicePlugin.searchForMets(Combination.CONJUNCTION, new String[]{"ein", "zwei"});
    int index = 0;
    for (String arg : args) {

      System.out.println(index++ + " - " + arg);
    }
    String[] searchForMets = elasticsearchServicePlugin.search(null, args[0]);
    printResult(searchForMets);

//    for (ISearchPlugin plugin : ServiceLoader.load(ISearchPlugin.class)) {
//      LOGGER.info(plugin.getName());
//      LOGGER.info(plugin.getVersion());
//      String[] searchForMets = plugin.searchForMets(Combination.DISJUNCTION, args);
//      printResult(searchForMets);
//      searchForMets = plugin.searchForMets(Combination.CONJUNCTION, args);
//      printResult(searchForMets);
//    }
//    } catch (UnknownHostException ex) {
//      java.util.logging.Logger.getLogger(ElasticsearchServicePlugin.class.getName()).log(Level.SEVERE, null, ex);
//    }
  }

  /**
   * Get transport client used to query for json documents.
   *
   * @param hostname Hostname of the server.
   * @param port Port of the server.
   * @param cluster Cluster name of the server.
   * @return TransportClient for indexing documents.
   */
  public synchronized static TransportClient getTransportClient(String hostname, int port, String cluster) {
    String hash = hostname + "_" + cluster;
    TransportClient client = ALL_CLIENTS.get(hash);
    if (client == null) {
      LOGGER.trace("Create TransportClient for hash '{}'", hash);
      Settings settings = Settings.builder().put("cluster.name", cluster).build();
      client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(hostname, port)));
      ALL_CLIENTS.put(hash, client);
    }
    return client;
  }

  /**
   * Print results. For quick testing only.
   *
   * @param hits Array holding the results.
   */
  private static void printResult(String[] hits) {
    System.out.println("Print results: ");
    org.json.JSONArray array = new JSONArray();
    for (String hit : hits) {
      array.put(hit);
      System.out.println("..." + hit);
    }
    System.out.println("JSONArray: ");
    System.out.println(array.toString(4));

  }

}
