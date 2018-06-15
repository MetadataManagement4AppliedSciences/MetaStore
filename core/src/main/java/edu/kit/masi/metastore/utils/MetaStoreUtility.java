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
package edu.kit.masi.metastore.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.kit.masi.metastore.db.ArangoDB;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.model.MetsArangoPOJO;
import edu.kit.masi.metastore.model.SectionDocument;
import edu.kit.masi.plugin.index.IIndexPlugin;
import edu.kit.masi.plugin.index.impl.IndexPluginFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for the metastore.
 *
 * @author hartmann-v
 */
public class MetaStoreUtility {

  /**
   * Logger.
   */
  public static final Logger LOGGER = LoggerFactory.getLogger(MetaStoreUtility.class);
  /**
   * Define path to the xslt files in classpath
   */
  public static final String XSLT_PATH = "/xslt/";
  /**
   * Define extension of the xslt files.
   */
  public static final String XSLT_EXTENSION = ".xsl";

  private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance(
          "org.apache.xalan.processor.TransformerFactoryImpl", null);
  
  private static final Transformer NO_TRANSFORM = getNoTransformer();

  private static final LoadingCache<String, Transformer> TRANSFORMER_BY_TYPE = Caffeine.newBuilder()
          .initialCapacity(10)
          .expireAfterAccess(1, TimeUnit.MINUTES)
          .build(type -> {
            LOGGER.trace("Look for xslt for type '{}'", type);
            InputStream is = MetaStoreUtility.class.getResourceAsStream(XSLT_PATH + type + XSLT_EXTENSION);
            Transformer transformer = NO_TRANSFORM;
            if (is != null) {
             StreamSource xlsStreamSource = new StreamSource(is);
             try {
                transformer = TRANSFORMER_FACTORY.newTransformer(xlsStreamSource);
                LOGGER.trace("Load xslt for type '{}'", type);
              } catch (TransformerConfigurationException ex) {
                LOGGER.error(null, ex);
              }
            }
            return transformer;
          });

  /**
   * Transform string to byte array.
   *
   * @param targetNameSpace Any string.
   * @return Representation of string as byteArray.
   */
  public static String getHashValue(String targetNameSpace) {
    StringBuilder sb = new StringBuilder();
    for (byte b : targetNameSpace.getBytes(Charset.forName("UTF-8"))) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  /**
   * Index the new json object.
   *
   * @param jsonObj Object to index.
   * @return Set containing all indexes.
   */
  public static Set<String> applyIndexing(JSONObject jsonObj) {
    Set<String> indexSet = new HashSet<>();
    for (Object keys : jsonObj.keySet()) {
      String nKeys = (String) keys;
      Object keyvalue = jsonObj.get(nKeys);

      if (keyvalue instanceof JSONObject) {
        getRecursiveKeys(keyvalue, nKeys, indexSet);
      }
    }
    return indexSet;
  }

  /**
   * Get all keys from (JSON) object
   *
   * @param keys Object (JSONObject)
   * @param nKeys Key of object
   * @param indexSet Set with all indexes.(Set will be modified during
   * execution.)
   * @return Empty string.
   */
  private static String getRecursiveKeys(Object keys, String nKeys, Set<String> indexSet) {
    JSONObject jsonobj = (JSONObject) keys;

    for (Object keysets : jsonobj.keySet()) {
      StringBuilder pathKey = new StringBuilder();
      String nkeys = (String) keysets;

      pathKey.append(nKeys).append(".").append(nkeys);

      Object keyvalue = jsonobj.get(nkeys);

      if (keyvalue instanceof JSONArray) {

        JSONArray keyValueArray = (JSONArray) keyvalue;
        for (int i = 0; i < keyValueArray.length(); i++) {
          try {
            JSONObject arrayToObj = keyValueArray.getJSONObject(i);
            pathKey.append(".").append(getRecursiveKeys(arrayToObj, pathKey.toString(), indexSet));
          } catch (JSONException e) {
          }
        }
      }

      if (keyvalue instanceof JSONObject) {
        pathKey.append(".").append(getRecursiveKeys(keyvalue, pathKey.toString(), indexSet));
      }

      if (!pathKey.toString().endsWith(".")) {
        indexSet.add("json." + pathKey.toString().replaceAll("\\.\\.+", "."));
      }
    }
    return "";
  }

  /**
   * Write nested section document to arango.
   *
   * @param pArango Instance for communicating with database.
   * @param pSectionDoc Instance holding all information about nested section.
   * @throws MetaStoreException An error occurred.
   */
  public static void storeNestedSection(ArangoDB pArango, SectionDocument pSectionDoc) throws MetaStoreException {
    try {
      LOGGER.trace("Store nested section document for type: {}", pSectionDoc.getType());
      String xmlDocument = XmlUtility.xmlToString(pSectionDoc.getRootNode());
      String prefix = pArango.getRegisteredXsdPrefix(MetaStoreUtility.getHashValue(pSectionDoc.getType()));
      // Two step storing document into arangodb 
      // 1.Step is to store in model as we need to store the XML content into AranogDB and it cannot be stored with rawDocument in ArangoDB
      MetsArangoPOJO metsPojo = new MetsArangoPOJO();
      metsPojo.setId(pSectionDoc.getSectionId());
      metsPojo.setMainXmlHandler(pSectionDoc.getDigitalObjectId());
      metsPojo.setXmlData(xmlDocument);
      String arangoDocumentHandler = pArango.storeXmlDocument(metsPojo);
      // 2. Step is to update the above document in raw document format as arango allows in this way to store JSON data.
      String jsonStringOrig = XML.toJSONObject(xmlDocument).toString();
      xmlDocument = transformXml(xmlDocument, prefix);
      String jsonString = XML.toJSONObject(xmlDocument).toString();
      String finalStr = "{\"type\":\"" + pSectionDoc.getType() + "\",\"json\":" + jsonStringOrig
              + ",\"mainXmlHandler\":\"" + pSectionDoc.getDigitalObjectId() + "\"}";
      pArango.storeJSONRawDocument(arangoDocumentHandler, finalStr);

      // Index section document
      // Skip DataOrganization due to key value pairs with different types.
      if (!pSectionDoc.getType().equalsIgnoreCase("http://datamanager.kit.edu/dama/dataorganization")) {
        IIndexPlugin indexPlugin = IndexPluginFactory.getIndexPlugin();
        // if indexing is available index document.
        if (indexPlugin != null) {
          indexPlugin.indexJsonDocument(jsonString, pSectionDoc.getDigitalObjectId(), prefix);
        }
      }

      // Applying Indexing operations. 
      // Index are applied on the stored json as aranogdb adds MAP attribute to some arrays, So to avoid unknow indexing.
      LOGGER.trace("Start indexing  section in database for type: {}", pSectionDoc.getType());
      JSONObject getStoredJson = pArango.getJsonObject(arangoDocumentHandler);
      Set<String> indexSet = applyIndexing(getStoredJson);
      pArango.applyIndexes(indexSet);
      LOGGER.trace("Finished indexing  section in database for type: {}", pSectionDoc.getType());
    } catch (JSONException jex) {
      throw new MetaStoreException("Error creating JSON document for section id: " + pSectionDoc.getSectionId(), jex);
    }
  }

  public static String transformXml(String pXmlDoc, String type) {
    String transformedXml = pXmlDoc;
    Transformer transformer = TRANSFORMER_BY_TYPE.get(type);
    if (transformer != NO_TRANSFORM) {
      try {
        StreamSource xmlStreamSource = new StreamSource(new StringReader(pXmlDoc));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(xmlStreamSource, result);
        transformedXml = baos.toString();
        LOGGER.trace("XML of type '{}' was transformed to: '{}'", type, transformedXml);
      } catch (TransformerException ex) {
        LOGGER.error("Error while transforming XML!", ex);
      }
    }
    return transformedXml;

  }
  
  private static Transformer getNoTransformer() {
    Transformer noTransformer = null;
    try {
      noTransformer = TRANSFORMER_FACTORY.newTransformer();
    } catch (TransformerConfigurationException ex) {
      LOGGER.error(null, ex);
    }
    return noTransformer;
  }

}
