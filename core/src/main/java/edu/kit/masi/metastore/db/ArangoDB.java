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
package edu.kit.masi.metastore.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;
import com.arangodb.DocumentCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.IndexesEntity;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.metastore.utils.ArangoPropertyHandler;
import edu.kit.masi.metastore.model.MetsArangoPOJO;
import org.json.JSONException;

/**
 * Class handling all communication with database.
 *
 * @author vaibhav
 */
public class ArangoDB {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDB.class);

  /**
   * Attribute names for storing attributes. Type: Namespace of the XML
   */
  private static final String TYPE_ATTRIBUTE = "type";
  /**
   * Attribute names for storing attributes. Prefix: prefix used for this
   * namespace (should be unique)
   */
  private static final String PREFIX_ATTRIBUTE = "prefix";
  /**
   * Attribute names for storing attributes. XSD: Schema
   */
  private static final String XSD_ATTRIBUTE = "xsd";
  /**
   * Attribute names for storing attributes. XML: XML
   */
  private static final String XML_ATTRIBUTE = "xml";
  /**
   * Driver for accessing data from arangodb.
   */
  private ArangoDriver driver;
  /**
   * Collection name.
   */
  private String collectionName;

  /**
   * Default constructor.
   */
  public ArangoDB() {
    ArangoPropertyHandler propertyHdlr = new ArangoPropertyHandler();
    propertyHdlr.loadProperty();

    collectionName = propertyHdlr.getCollectionName();

    ArangoConfigure configure = new ArangoConfigure();
    ArangoHost arangoHost = new ArangoHost(propertyHdlr.getUrl(), propertyHdlr.getIntPort());
    configure.setArangoHost(arangoHost);

    configure.setDefaultDatabase(propertyHdlr.getDbName());
    configure.setUser(propertyHdlr.getUsername());
    configure.setPassword((null != propertyHdlr.getPassword() ? propertyHdlr.getPassword() : ""));
    configure.init();
    this.driver = new ArangoDriver(configure);
    if (propertyHdlr.getDbDelete()) {
      try {
        LOGGER.warn("Database will be deleted! Set 'dbDelete' in DatabaseProperties.properties to false to avoid deleting database.");
        driver.deleteDatabase(propertyHdlr.getDbName());
      } catch (ArangoException e) {
        LOGGER.error("Error while deleting database!", e);
      }
    }

    try {
      List<String> dblist = driver.getDatabases().getResult();
      String dbName = propertyHdlr.getDbName();
      if (!dblist.contains(dbName)) {
        LOGGER.warn(
                "Database '" + dbName + "' not found during instantiating driver for arango database!");
        driver.createDatabase(propertyHdlr.getDbName());
        LOGGER.info(
                "Database '" + dbName + "' created!");
      }
      HashMap allCollectionName = (HashMap) driver.getCollections().getNames();
      if (!allCollectionName.containsKey(collectionName)) {
        LOGGER.warn(
                "Collection '" + collectionName + "' not found during instantiating driver for arango database!");
        driver.createCollection(collectionName);
        LOGGER.info("Collection '" + collectionName + "' created!");
      }
    } catch (ArangoException e1) {
      LOGGER.error("Error while instantiating driver for arango database!", e1);
    }
  }

  /**
   * Register XSD
   *
   * @param hashedKey hashed key
   * @param xsdString string containing xsd
   * @param type target namespace of the xsd
   * @param prefix prefix of the given namespace (should be unique)
   * @return Message
   * @throws MetaStoreException If something went wrong.
   */
  public String postXSD(String hashedKey, String xsdString, String type, String prefix) throws MetaStoreException {
    BaseDocument xmlForArango = new BaseDocument();
    xmlForArango.addAttribute(PREFIX_ATTRIBUTE, prefix);
    xmlForArango.addAttribute(TYPE_ATTRIBUTE, type);
    xmlForArango.addAttribute(XSD_ATTRIBUTE, xsdString);
    xmlForArango.setDocumentKey(hashedKey);
    try {
      synchronized (driver) {
        driver.createDocument(collectionName, xmlForArango);
      }
    } catch (ArangoException e) {
      if (e.getMessage().equals("[1210] cannot create document, unique constraint violated")) {
        throw new MetaStoreException(e, StatusCode.CONFLICT.getStatusCode()); // conflict
      } else {
        throw new MetaStoreException(e);
      }
    }
    return "Successfully registered";
  }

  /**
   * Get xsd
   *
   * @param hashValue hashed value
   * @return Message
   * @throws MetaStoreException
   */
  public String getRegisteredXsd(String hashValue) throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.getDocument(collectionName + "/" + hashValue, BaseDocument.class).getEntity()
                .getAttribute(XSD_ATTRIBUTE).toString();
      }
    } catch (ArangoException e) {
      throw new MetaStoreException("No XSD registered for hash '" + hashValue + "'", e);
    }
  }

  /**
   * Get namespace of xsd
   *
   * @param hashValue hashed value
   * @return message
   * @throws MetaStoreException If something went wrong.
   */
  public String getRegisteredXsdType(String hashValue) throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.getDocument(collectionName + "/" + hashValue, BaseDocument.class).getEntity()
                .getAttribute(TYPE_ATTRIBUTE).toString();
      }
    } catch (ArangoException e) {
      throw new MetaStoreException("No type for XSD registered for hash '" + hashValue + "'", e);
    }
  }

  /**
   * Get prefix of xsd. Prefix should be unique.
   *
   * @param hashValue hashed value
   * @return message
   * @throws MetaStoreException If something went wrong.
   */
  public String getRegisteredXsdPrefix(String hashValue) throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.getDocument(collectionName + "/" + hashValue, BaseDocument.class).getEntity()
                .getAttribute(PREFIX_ATTRIBUTE).toString();
      }
    } catch (ArangoException e) {
      throw new MetaStoreException("No prefix of XSD registered for hash '" + hashValue + "'", e);
    }
  }

  /**
   * Write xml to Database
   *
   * @param hashedKey hashed key of unique id
   * @param xmlString XML document
   * @param type type of the XML document (e.g.: prefix of namespace)
   * @return Success otherwise an exception will be thrown.
   * @throws MetaStoreException Error during storage.
   */
  public String postXML(String hashedKey, String xmlString, String type) throws MetaStoreException {
    BaseDocument xmlForArango = new BaseDocument();
    xmlForArango.addAttribute(TYPE_ATTRIBUTE, type);
    xmlForArango.addAttribute(XML_ATTRIBUTE, xmlString);
    xmlForArango.setDocumentKey(hashedKey);
    String returnMsg;
    try {
      synchronized (driver) {
        driver.createDocument(collectionName, xmlForArango);
      }
      returnMsg = "Successfully Registered.";
    } catch (ArangoException e) {
      if (e.getMessage().equals("[1210] cannot create document, unique constraint violated")) {
        throw new MetaStoreException(e, StatusCode.CONFLICT.getStatusCode()); // Conflict
      } else {
        throw new MetaStoreException(e);
      }
    }
    return returnMsg;
  }

  /**
   * Store raw json
   *
   * @param hashedKey hashed key
   * @param finalString json as string
   * @throws MetaStoreException If something went wrong.
   */
  public void storeJSONRawDocument(String hashedKey, String finalString) throws MetaStoreException {
    try {
      synchronized (driver) {
        driver.updateDocumentRaw(collectionName + "/" + hashedKey, finalString, null, false, null);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * Get json object
   *
   * @param hashedKey hashed key
   * @return JSON object
   * @throws MetaStoreException If something went wrong.
   */
  public JSONObject getJsonObject(String hashedKey) throws MetaStoreException {
    try {
      String fetchData1;
      synchronized (driver) {
        fetchData1 = driver.getDocumentRaw(collectionName + "/" + hashedKey, null, null);
      }
      return new JSONObject(fetchData1).getJSONObject("json");

    } catch (ArangoException | JSONException e) {
      throw new MetaStoreException(e, StatusCode.NOT_FOUND.getStatusCode()); // resource
      // not
      // found
    }
  }

  /**
   * Apply indexes.
   *
   * @param indexSet Set of indexes.
   */
  public void applyIndexes(Set<String> indexSet) {
    for (String indSet : indexSet) {
      try {
        synchronized (driver) {
          driver.createFulltextIndex(collectionName, indSet);
        }
      } catch (ArangoException e) {
        LOGGER.error("Error while creating index for '" + indSet + "'!", e);
      }
    }
  }

  /**
   * Get collection name.
   *
   * @return collection name.
   */
  public String getCollectionName() {
    return collectionName;
  }

  /**
   * Store XML document.
   *
   * @param metsPOJO Plain old java object representing METS file.
   * @return Message
   * @throws MetaStoreException If something went wrong
   */
  public String storeXmlDocument(MetsArangoPOJO metsPOJO) throws MetaStoreException {
    DocumentEntity<MetsArangoPOJO> entity = null;
    try {
      synchronized (driver) {
        entity = driver.createDocument(collectionName, metsPOJO);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
    return entity.getDocumentKey();
  }

  /**
   * Get indexes.
   *
   * @return entities with indexes
   * @throws MetaStoreException If something went wrong
   */
  public IndexesEntity getIndexes() throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.getIndexes(collectionName);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * Apply full text search.
   *
   * @param indexEntity entity
   * @param text search term
   * @return Set of documents.
   * @throws MetaStoreException If something went wrong
   */
  public DocumentCursor<ArangoDriver> applyFullTextSearch(String indexEntity, String text) throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.executeSimpleFulltextWithDocuments(collectionName, indexEntity, "prefix:" + text, 0, 0, null,
                null);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * Get XML document
   *
   * @param hashedValue hashed value.
   * @return XML document
   * @throws MetaStoreException If something went wrong
   */
  public String getXMLData(String hashedValue) throws MetaStoreException {
    try {
      synchronized (driver) {
        return driver.getDocument(collectionName + "/" + hashedValue, BaseDocument.class).getEntity()
                .getAttribute(XML_ATTRIBUTE).toString();
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * Get all child documents
   *
   * @param pUniqueId hashed value of document
   * @return Set with all documents.
   * @throws MetaStoreException If something went wrong
   */
  public DocumentCursor<BaseDocument> getAllChildDocuments(String pUniqueId) throws MetaStoreException {
    LOGGER.debug("getAllChildDocuments: " + pUniqueId);
    String allIDDocQuery = "FOR doc IN " + collectionName
            + " filter doc.mainXmlHandler==@mainXmlHandler && doc.xmlData != null return doc";
    Map<String, Object> bindingVals = new HashMap<>();
    bindingVals.put("mainXmlHandler", pUniqueId);
    try {
      synchronized (driver) {
        return driver.executeDocumentQuery(allIDDocQuery, bindingVals, null, BaseDocument.class);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * Get content metadata
   *
   * @param uniqueID hashed value of document.
   * @param nameSpace namespace of document.
   * @return Set with all documents.
   * @throws MetaStoreException If something went wrong
   */
  public DocumentCursor<BaseDocument> getContentMetaDataXml(String uniqueID, String nameSpace) throws MetaStoreException {

    LOGGER.debug("getContentMetaDataXml: {}, {}", uniqueID, nameSpace);
    String allIDDocQuery = "FOR doc IN " + collectionName
            + " filter doc.mainXmlHandler==@mainXmlHandler && doc.type==@type return doc";
    Map<String, Object> bindingVals = new HashMap<>();
    bindingVals.put("mainXmlHandler", uniqueID);
    bindingVals.put("type", nameSpace);
    try {
      synchronized (driver) {
        return driver.executeDocumentQuery(allIDDocQuery, bindingVals, null, BaseDocument.class);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /*
	 * public String updateXMLData(String documentHandle, String xmlData, String
	 * finalStr, Map<String, String> arrayData) throws MetaStoreException {
	 * String updateQuery = "FOR doc IN " + collectionName +
	 * " FILTER doc._id==@mainXmlHandler UPDATE doc WITH { xmlData: @xmlData ,json :@json} IN "
	 * + collectionName; Map<String, Object> bindingVals = new HashMap<>();
	 * bindingVals.put("mainXmlHandler", documentHandle);
	 * bindingVals.put("xmlData", xmlData); bindingVals.put("json", ""); try {
	 * driver.executeDocumentQuery(updateQuery, bindingVals, null,
	 * BaseDocument.class); String updateArray = "for doc in " + collectionName
	 * +
	 * " FILTER doc._id==@mainXmlHandler UPDATE doc WITH {oldXMLData: PUSH(doc.oldXMLData, @arrayData)} IN "
	 * + collectionName; Map<String, Object> bindingVals2 = new HashMap<>();
	 * bindingVals2.put("mainXmlHandler", documentHandle);
	 * bindingVals2.put("arrayData", arrayData);
	 * 
	 * driver.updateDocumentRaw(documentHandle, finalStr, null, false, null);
	 * driver.executeAqlQuery(updateArray, bindingVals2, null,
	 * BaseDocument.class);
	 * 
	 * } catch (ArangoException e) { throw new
	 * MetaStoreException("Error while storing data!", e); } return
	 * "Successfully stored the json and XmlData"; }
   */
  /**
   * Get digtial object ID of document.
   *
   * @param hashedKey hashed value.
   * @return digital object id.
   * @throws MetaStoreException If something went wrong
   */
  public String getDigitalObjectIdForDocument(String hashedKey) throws MetaStoreException {
    String digitalObjectId;
    try {
      String fetchData1;
      synchronized (driver) {
        fetchData1 = driver.getDocumentRaw(collectionName + "/" + hashedKey, null, null);
      }
      digitalObjectId = new JSONObject(fetchData1).getString("mainXmlHandler").toString();
    } catch (Exception e) {
      LOGGER.error("Excepiton :" + e.getMessage());
      throw new MetaStoreException(e);
    }
    return digitalObjectId;
  }

  /**
   * Get section document key.
   *
   * @param nameSpace namespace
   * @param pDigitalObjectId id of document
   * @param pSectionId id of section
   * @return Array with all documents
   * @throws MetaStoreException something went wrong.
   */
  public DocumentCursor<BaseDocument> getSectionDocumentKey(String nameSpace, String pDigitalObjectId,
          String pSectionId) throws MetaStoreException {
    StringBuilder allIDDocQuery = new StringBuilder();
    allIDDocQuery.append(
            "FOR doc IN " + collectionName + " filter doc.mainXmlHandler==@mainXmlHandler && doc.type==@type ");
    Map<String, Object> bindingVals = new HashMap<>();
    bindingVals.put("mainXmlHandler", pDigitalObjectId);
    bindingVals.put("type", nameSpace);
    if (!pSectionId.equals("")) {
      allIDDocQuery.append("&& doc.id==@id");
      bindingVals.put("id", pSectionId);
    }
    allIDDocQuery.append(" return doc");

    try {
      synchronized (driver) {
        return driver.executeDocumentQuery(allIDDocQuery.toString(), bindingVals, null, BaseDocument.class);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e);
    }
  }

  /**
   * This Method is used to update the partial section of the complete mets
   * file. This method does update process by performing following steps
   * 1.stores older xml data into array 2. delete exsiting Json 3. store new xml
   * data 4. store new json data 5. apply indexing to new json data
   *
   * @param documentToUpdateKey key
   * @param pSectionDocument document
   * @param finalStr don't know
   * @param arrayData features
   * @return message
   * @throws MetaStoreException something goes wrong
   */
  public String updateXMLData(String documentToUpdateKey, String pSectionDocument, String finalStr,
          Map<String, String> arrayData) throws MetaStoreException {

    String updateQuery = "FOR doc IN " + collectionName
            + " FILTER doc._id==@mainXmlHandler UPDATE doc WITH { xmlData: @xmlData ,json :@json} IN "
            + collectionName;
    Map<String, Object> bindingVals = new HashMap<>();
    bindingVals.put("mainXmlHandler", collectionName + "/" + documentToUpdateKey);
    bindingVals.put("xmlData", pSectionDocument);
    bindingVals.put("json", "");
    try {
      synchronized (driver) {
        driver.executeDocumentQuery(updateQuery, bindingVals, null, BaseDocument.class);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e.getMessage(), StatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }
    String updateArray = "for doc in " + collectionName + " FILTER doc._id==@mainXmlHandler UPDATE doc WITH {oldXMLData: PUSH(doc.oldXMLData, @arrayData)} IN "
            + collectionName;
    Map<String, Object> bindingVals2 = new HashMap<>();
    bindingVals2.put("mainXmlHandler", collectionName + "/" + documentToUpdateKey);
    bindingVals2.put("arrayData", arrayData);
    try {
      synchronized (driver) {
        driver.updateDocumentRaw(collectionName + "/" + documentToUpdateKey, finalStr, null, false, null);
        driver.executeAqlQuery(updateArray, bindingVals2, null, BaseDocument.class);
      }
    } catch (ArangoException e) {
      throw new MetaStoreException(e.getMessage(), StatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }
    return "Successfully stored the json and XmlData";

  }

}
