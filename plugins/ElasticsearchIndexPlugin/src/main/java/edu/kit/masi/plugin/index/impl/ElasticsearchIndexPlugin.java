/*
 * Copyright 2017 Karlsruhe Institute of Technology.
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
package edu.kit.masi.plugin.index.impl;

import edu.kit.masi.plugin.AbstractServicePlugin;
import edu.kit.masi.plugin.index.IIndexPlugin;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index json documents by elasticsearch. To use a different setup extend this
 * class by override only the method 'getServiceName()'.
 *
 * @author hartmann-v
 */
public class ElasticsearchIndexPlugin extends AbstractServicePlugin implements IIndexPlugin {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchIndexPlugin.class);
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
  /**
   * Hostname of elasticsearch. default: localhost
   */
  private String hostname;
  /**
   * Port of elasticsearch. default: 9300
   */
  private int port;

  /**
   * Clustername of the elasticsearch cluster. default: KitDataManager@hostname
   */
  private String clusterName;
  /**
   * Index. default: kitdatamanager
   */
  private String index;

  /**
   * Map with all clients.
   */
  private final static Map<String, TransportClient> ALL_CLIENTS = new HashMap<>();

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void configureService(Configuration pConfig) {
    hostname = pConfig.getString(KEY_HOSTNAME);
    port = pConfig.getInt(KEY_PORT);
    clusterName = pConfig.getString(KEY_CLUSTER_NAME);
    index = pConfig.getString(KEY_INDEX);
    LOGGER.debug("Configure elasticsearch: url: {}:{}\ncluster name: {}, index: {}", hostname, port, clusterName, index);
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  /**
   * Get transport client used to index json documents.
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

  @Override
  public boolean indexJsonDocument(String pJsonDoc, String pDocumentId, String pType) {
    LOGGER.debug("Intitializing transport client..");
    IndexResponse response;
    TransportClient client = getTransportClient(hostname, port, clusterName);
//    try {
    if (LOGGER.isTraceEnabled()) {
      int length = Math.min(pJsonDoc.length(), 256);
      LOGGER.trace("Indexing document...{} ... length: {}", pJsonDoc.substring(0, length), pJsonDoc.length());
    } else {
      LOGGER.debug("Indexing document...");
    }
    response = client.prepareIndex(index,
            pType,
            pDocumentId + "_" + pType).
            setSource(pJsonDoc)
            .execute()
            .actionGet();
//    }
    LOGGER.debug("Document with id {} was {}. Current document version: {}", response.getId(), (response.status().equals(RestStatus.CREATED)) ? "created" : "updated", response.getVersion());

    return true;
  }

}
