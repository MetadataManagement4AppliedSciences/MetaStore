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
package edu.kit.masi.metastore.control;

import edu.kit.masi.metastore.db.ArangoDB;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.metastore.model.ReturnType;
import edu.kit.masi.metastore.model.SectionDocument;
import edu.kit.masi.metastore.utils.MetaStoreUtility;
import edu.kit.masi.metastore.utils.MetsUtility;
import edu.kit.masi.metastore.utils.XmlUtility;
import edu.kit.masi.metastore.utils.XsdUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.fzk.tools.xml.JaxenUtil;
import org.jdom.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.DocumentCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import edu.kit.masi.plugin.index.IIndexPlugin;
import edu.kit.masi.plugin.index.impl.IndexPluginFactory;

import org.jdom.Document;

/**
 * Controller managing all services.
 *
 * @author hartmann-v
 */
public class MetaStoreController implements IMetaStoreController {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MetaStoreController.class);

  /**
   * Connection to database.
   */
  private static final ArangoDB arango = new ArangoDB();
  /**
   * Utilities handling METS file.
   */
  private final MetsUtility metsUtility = new MetsUtility(arango);

  /**
   * Default constructor.
   */
  public MetaStoreController() {
  }
  
  public static final ArangoDB getArangoDb() {
    return arango;
  }

  @Override
  public String registerXsdDocument(String pXsdDocument, String pPrefix) throws MetaStoreException {
    XsdUtil xsdUtil = new XsdUtil();

    String targetNameSpace = xsdUtil.getNamespaceAndVersionFromXsd(pXsdDocument);

    if (!targetNameSpace.equals(XsdUtil.NO_NAMESPACE_DEFINED)) {
      String hashedKey = MetaStoreUtility.getHashValue(targetNameSpace);
      LOGGER.debug("Store XSD for namespace '{}' in MetaStore.", targetNameSpace);
      arango.postXSD(hashedKey, pXsdDocument, xsdUtil.getNamespace(), pPrefix);
      LOGGER.debug("Successfully stored XSD for namespace '{}' in MetaStore.", targetNameSpace);
    } else {
      throw new MetaStoreException("No target namespace defined!");
    }
    return "Register namespace '" + targetNameSpace + "'";
  }

  @Override
  public String getMetsDocument(String pDigitalObjectId, ReturnType returnType) throws MetaStoreException {
    // TODO:
    if (pDigitalObjectId == null || pDigitalObjectId.trim().equals("")) {
      throw new MetaStoreException("No id provided for METS file", StatusCode.BAD_REQUEST.getStatusCode());
    }
    String hashedValue = MetaStoreUtility.getHashValue(pDigitalObjectId);
    if (!arango.getRegisteredXsdType(hashedValue).equals(MetsUtility.METS_NAMESPACE)) {
      throw new MetaStoreException("Invalid id: Not a METS file", StatusCode.NOT_FOUND.getStatusCode());
    }
    return metsUtility.getMetsDocument(pDigitalObjectId, returnType);
  }

  @Override
  public String storeMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException {
    // validate METS document
    // validate sections
    // store and index METS document
    // store and index sections.
    try {
      String hashedKey = MetaStoreUtility.getHashValue(pDigitalObjectId);
      Document document = JaxenUtil.getDocument(pMetsDocument);
      Namespace documentNamespace = document.getRootElement().getNamespace();
      // Check for METS namespace
      if (!MetsUtility.METS_NAMESPACE.equalsIgnoreCase(documentNamespace.getURI())) {
        throw new MetaStoreException("Invalid METS document: Namespace mismatch",
                StatusCode.BAD_REQUEST.getStatusCode());
      }
      // <editor-fold defaultstate="collapsed" desc="validate wellformed
      // XML">
      String xsdForMets = getXsdAsString(documentNamespace.getURI());
      if (!XmlUtility.validate(pMetsDocument, xsdForMets)) {
        throw new MetaStoreException("XML is not valid!", StatusCode.BAD_REQUEST.getStatusCode());
      }
      // </editor-fold>

      // <editor-fold defaultstate="collapsed" desc="Validate nested
      // sections.">
      List<SectionDocument> nestedSections = metsUtility.validateNestedSections(pMetsDocument, pDigitalObjectId);
      // </editor-fold>

      // <editor-fold defaultstate="collapsed" desc="Store nested sections
      // plus METS file">
      arango.postXML(hashedKey, pMetsDocument, documentNamespace.getURI());
      for (SectionDocument sectionDoc : nestedSections) {
        MetaStoreUtility.storeNestedSection(arango, sectionDoc);
      }
      // </editor-fold>
    } catch (MetaStoreException ex) {
      throw ex;
    } catch (Exception ex) {
      LOGGER.error("Invalid METS file!", ex);
      throw new MetaStoreException("Invalid METS file! - " + ex.getMessage(), StatusCode.BAD_REQUEST.getStatusCode());
    }
    return "METS document stored successfully.";
  }

  @Override
  public String updateMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException {
    // SecurityHelper.checkForAuthorization(pContext, pGroupId,
    // Role.MEMBER);
    // validate METS document
    // validate sections
    // store and index METS document
    // store and index sections.
    throw new MetaStoreException("Update METS not implemented yet!",
            StatusCode.SERVICE_UNAVAILABLE.getStatusCode());
    // return "METS document updated successfully.";
  }

  @Override
  public String getPartialMetsDocument(String pType, String pDigitalObjectId, ReturnType returnType)
          throws MetaStoreException {
    // load section.
    String allSections = metsUtility.getMetsSectionByNameSpace(pType, pDigitalObjectId, returnType);
    return allSections;
  }

  @Override
  public String updatePartialMetsDocument(String pSectionDocument, String pDigitalObjectId, String pSectionId)
          throws MetaStoreException {
    // extract namespace of section document
    // look for section using namespace (and sectionId if section id is not
    // null --> not needed right now)
    // store and index sections.
    // thow exception if multiple section having similar type of namespace
    // and section id = null
    Document document;
    try {
      document = JaxenUtil.getDocument(pSectionDocument);
      String nameSpace = document.getRootElement().getNamespace().getURI();
      String xsdForSectionDocument = getXsdAsString(nameSpace);
      // validating the section against the valid XSD Before updating
      if (!XmlUtility.validate(pSectionDocument, xsdForSectionDocument)) {
        throw new MetaStoreException("XML is not valid!", StatusCode.BAD_REQUEST.getStatusCode());
      }
      // Checking for no of documents present with same namespace and ID
      // if multiple throws exceptions without updating
      DocumentCursor<BaseDocument> sectionDocumentKey = arango.getSectionDocumentKey(nameSpace, pDigitalObjectId,
              pSectionId);
      int counter = 1;
      String documentToUpdateKey = "";
      String olderXmlData = "";
      for (DocumentEntity<BaseDocument> documentEntity : sectionDocumentKey) {
        if (counter > 1) {
          throw new MetaStoreException("Zero Or More Section Found Please Provide Valid SectionID",
                  StatusCode.BAD_REQUEST.getStatusCode());
        }
        documentToUpdateKey = documentEntity.getEntity().getDocumentKey();
        olderXmlData = documentEntity.getEntity().getAttribute("xmlData").toString();
        counter++;
      }
      // code for updating all the json data, XmlData and storing older
      // xml data into array and finally appliying indexes;
      Date toDaysDate = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");

      Map<String, String> arrayData = new HashMap<String, String>();
      arrayData.put("xmlData", olderXmlData);
      arrayData.put("modifiedDate", sdf.format(toDaysDate).toString());
      String jsonString = XML.toJSONObject(pSectionDocument).toString();

      String finalStr = "{\"json\":" + jsonString + "}";
      String xmlKey = arango.updateXMLData(documentToUpdateKey, pSectionDocument, finalStr, arrayData);

      // Index section document
      IIndexPlugin indexPlugin = IndexPluginFactory.getIndexPlugin();
      // if indexing is available index document.
      if (indexPlugin != null) {
        String prefix = arango.getRegisteredXsdPrefix(MetaStoreUtility.getHashValue(nameSpace));
        indexPlugin.indexJsonDocument(jsonString, pDigitalObjectId, prefix);
      }

      // applying index on new json
      JSONObject getStoredJson = arango.getJsonObject(documentToUpdateKey);
      Set<String> indexSet = MetaStoreUtility.applyIndexing(getStoredJson);
      arango.applyIndexes(indexSet);

    } catch (MetaStoreException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error("No namespace found!", e);
      throw new MetaStoreException("No namespace found! - " + e.getMessage(), StatusCode.BAD_REQUEST.getStatusCode());
    }
    return "METS document updated successfully.";
  }

  @Override
  public boolean validateDocument(String pXmlDocument) throws MetaStoreException {
    boolean isValid = false;
    try {
      Document document = JaxenUtil.getDocument(IOUtils.toInputStream(pXmlDocument));
      Namespace documentNamespace = document.getRootElement().getNamespace();
      // <editor-fold defaultstate="collapsed" desc="validate wellformed
      // XML">
      String xsdString = getXsdAsString(documentNamespace.getURI());
      if (!XmlUtility.validate(pXmlDocument, xsdString)) {
        throw new MetaStoreException("XML is not valid!");
      }
      isValid = true;
      // </editor-fold>
    } catch (MetaStoreException e) {
      throw e;
    } catch (Exception ex) {
      LOGGER.error("Error parsing xml!", ex);
      throw new MetaStoreException("XML is not valid! - " + ex.getMessage(), ex);
    }

    return isValid;
  }

  @Override
  public String searchForMetsDocuments(String searchTerm, int maxNoOfHits, ReturnType returnType)
          throws MetaStoreException {
    Set<String> allDigitalObjectIds = metsUtility.searchFullTextWithId(searchTerm);
    if (allDigitalObjectIds.size() > 0) {
      // filter Digital Object IDs
      int noOfDocuments = 1;
      if (returnType.equals(ReturnType.JSON)) {
        JSONArray array = new JSONArray();
        for (String digitalObjectIds : allDigitalObjectIds) {
          // JSONObject jsonObject =
          // arango.getJsonObject(MetaStoreUtility.getHashValue(digitalObjectIds));
          // returnValue.put(jsonObject);
          JSONObject item = new JSONObject(metsUtility.getMetsDocument(digitalObjectIds, returnType));
          array.put(item);
          noOfDocuments++;
          if (noOfDocuments > maxNoOfHits) {
            break;
          }
        }
        return array.toString();
      } else {
        StringBuilder returnValue = new StringBuilder();
        returnValue.append("<array>\n");
        for (String digitalObjectIds : allDigitalObjectIds) {
          returnValue.append(
                  metsUtility.getMetsDocument(digitalObjectIds, returnType)
                          .replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "") + "\n");
          noOfDocuments++;
          if (noOfDocuments > maxNoOfHits) {
            break;
          }
        }
        returnValue.append("</array>");
        return returnValue.toString();
      }

    }
    // TODO: if no documents found for the required terms then which error
    // to throw ?
    return null;
  }

  @Override
  public String getXsdAsString(String pNamespace) throws MetaStoreException {
    String xsdHashValue = MetaStoreUtility.getHashValue(pNamespace);
    String xsdForMets = arango.getRegisteredXsd(xsdHashValue);
    return xsdForMets;
  }
}
