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

import edu.kit.masi.metastore.model.MetsArangoPOJO;
import edu.kit.masi.metastore.model.SectionDocument;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.arangodb.ArangoDriver;
import com.arangodb.DocumentCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.IndexEntity;
import edu.kit.masi.metastore.db.ArangoDB;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.metastore.model.ReturnType;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;

/**
 * Utilities for handling METS files.
 *
 * @author vaibhav/hartmann-v
 */
public class MetsUtility {

  /**
   * Namespace of METS.
   */
  public static final String METS_NAMESPACE = "http://www.loc.gov/METS/";

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MetsUtility.class);
  /** Prefix of mets. */
  private static final String METS_PREFIX = "mets:";
  /** instance of ArangoDB. */
  private final ArangoDB arango;

  /**
   * Constructor 
   * @param pArango instance for access to ArangoDB.
   */
  public MetsUtility(ArangoDB pArango) {
    arango = pArango;
  }

  /**
   * Download METS with given unique ID as JSON or XML
   *
   * @param pUniqueId unique ID.
   * @param returnType String (JSON/XML)
   * @return XML document as string.
   * @throws MetaStoreException An error occurred.
   */
  public String getMetsDocument(String pUniqueId, ReturnType returnType) throws MetaStoreException {
    String xmlString = arango.getXMLData(MetaStoreUtility.getHashValue(pUniqueId));

    Document doc2 = XmlUtility.strToXmlDocument(xmlString);
    DocumentCursor cursor = arango.getAllChildDocuments(pUniqueId);
    Iterator iterator = cursor.entityIterator();
    LOGGER.trace("METS Document: {}", xmlString);

    XPath xPath = XPathFactory.newInstance().newXPath();
    while (iterator.hasNext()) {
      BaseDocument aDocument = (BaseDocument) iterator.next();

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Child Document: {}", aDocument.getAttribute("xmlData"));
        LOGGER.trace("ID: {}", aDocument.getAttribute("id"));
      }

      String xmpathExp = "//*[local-name()='xmlData'][../../@ID='" + aDocument.getAttribute("id") + "']";
      try {
        NodeList nodeList = (NodeList) xPath.compile(xmpathExp).evaluate(doc2, XPathConstants.NODESET);
        if (nodeList.getLength() > 0) {
          Node xmlDataNode = nodeList.item(0); // xmlData node
          Node child = null;
          int noOfChilds = xmlDataNode.getChildNodes().getLength();
          for (int item = 0; item < noOfChilds; item++) {
            child = xmlDataNode.getChildNodes().item(item);
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Node type: " + child.getNodeType());
              LOGGER.trace(XmlUtility.xmlToString(child));
            }
            if (child.getNodeType() == Node.ELEMENT_NODE) {
              break;
            }
          }

          Node importedNode = XmlUtility.strToXmlDocument(aDocument.getAttribute("xmlData").toString())
                  .getFirstChild();
          xmlDataNode.removeChild(child);
          xmlDataNode.appendChild(doc2.importNode(importedNode, true));
        }
      } catch (XPathExpressionException | DOMException e) {
        throw new MetaStoreException(e);
      }
    }
    // ***************************************************************************
    // The next 4 lines are no longer needed due to store xmlData childs
    // directly.
    // String filesecpathExp = "//*[local-name()='fileSec']";
    // String stuctpathExp = "//*[local-name()='structMap']";
    // changeStructure(filesecpathExp, doc2, xPath);
    // changeStructure(stuctpathExp, doc2, xPath);
    // ***************************************************************************
    String metsString = XmlUtility.xmlToString(doc2);

    LOGGER.trace("Updated METS Document: {}", metsString);
    if (returnType == ReturnType.JSON) {
      JSONObject toJSONObject = XML.toJSONObject(metsString);
      metsString = toJSONObject.toString();
    }
    return metsString;
  }

  // <editor-fold defaultstate="collapsed" desc="Handling sections>
  /**
   * Get all section documents (xmlData) of a mets file.
   *
   * @param pMetsXml mets file as string.
   * @param pDigitalObjectId uniqueId of the mets file.
   * @return List containing all documents.
   */
  private List<SectionDocument> getAllSections(String pMetsXml, String pDigitalObjectId) throws MetaStoreException {
    return getAllSections(XmlUtility.strToXmlDocument(pMetsXml), pDigitalObjectId);
  }

  /**
   * Get all section documents (xmlData) of a mets file.
   *
   * @param pMetsDocument mets file as string.
   * @param pDigitalObjectId uniqueId of the mets file.
   * @return List containing all documents.
   */
  private List<SectionDocument> getAllSections(Document pMetsDocument, String pDigitalObjectId)
          throws MetaStoreException {
    List<SectionDocument> allSections = new ArrayList<>();

    XPath xPath = XPathFactory.newInstance().newXPath();
    String xmpathExp = "//*[local-name()='xmlData']/*";
    try {
      NodeList nodeList = (NodeList) xPath.compile(xmpathExp).evaluate(pMetsDocument, XPathConstants.NODESET);
      for (int index = 0; index < nodeList.getLength(); index++) {
        SectionDocument sd;
        // Determining ID of the section.
        Node sectionNode = nodeList.item(index).getParentNode().getParentNode().getParentNode();
        String sectionId = sectionNode.getAttributes().getNamedItem("ID").getNodeValue();
        Node rootNode = nodeList.item(index);
        sd = new SectionDocument(pDigitalObjectId, sectionId, rootNode);
        allSections.add(sd);
      }
    } catch (XPathExpressionException | DOMException e) {
      throw new MetaStoreException(e);
    }
    return allSections;
  }

  /**
   * This Method checks for all the Documents in ArangoDb for type=namespace and
   * digitalObject ID. All the matched documents are return as string either in
   * JSON/XML format.
   *
   * @param pType nameSpace
   * @param pDigitalObjectId digitalObjectId
   * @param returnType String (JSON/XML)
   * @return JSON or XML String containing all section documents.
   * @throws MetaStoreException If something went wrong.
   */
  public String getMetsSectionByNameSpace(String pType, String pDigitalObjectId, ReturnType returnType)
          throws MetaStoreException {
    DocumentCursor<BaseDocument> allSections1 = arango.getContentMetaDataXml(pDigitalObjectId, pType);
    StringBuilder allSection = new StringBuilder();

    if (returnType.equals(ReturnType.XML)) {
//			this block creates array of xml data 
      allSection.append("<array>\n");
      for (DocumentEntity<BaseDocument> documentEntity : allSections1) {
        allSection.append(documentEntity.getEntity().getAttribute("xmlData").toString().replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")).append("\n");
      }
      allSection.append("</array>");
    } else if (returnType.equals(ReturnType.JSON)) {
//			this block creates json array 
      JSONArray allSectionJson = new JSONArray();
      for (DocumentEntity<BaseDocument> documentEntity : allSections1) {
        allSectionJson.put(documentEntity.getEntity().getAttribute("json"));
      }
      allSection.append(allSectionJson.toString());
    }
    return allSection.toString();
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Validation of document">
  /**
   * Validate METS document.
   *
   * @param pMetsDocument Mets document
   * @return valid or not
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateMets(Document pMetsDocument) throws MetaStoreException {
    boolean isValid;
    // Validate Mets document
    validateNode(pMetsDocument.getDocumentElement());
    // split in multiple sections and validate them separately
    // for all sections: validateNode(sectionNode);

    // Everything works fine. Set isValid to true;
    isValid = true;
    return isValid;
  }

  /**
   * Validate all section documents (xmlData) of a mets file.
   *
   * @param pMetsXml mets file as string.
   * @param pDigitalObjectId uniqueId of the mets file.
   * @return List containing all nested sections.
   * @throws edu.kit.masi.metastore.exception.MetaStoreException An error occurred.
   */
  public List<SectionDocument> validateNestedSections(String pMetsXml, String pDigitalObjectId)
          throws MetaStoreException {
    List<SectionDocument> allSections = getAllSections(pMetsXml, pDigitalObjectId);
    // Validate all nested sections
    for (SectionDocument sectionDoc : allSections) {
      if (!validateSectionDocument(sectionDoc)) {
        throw new MetaStoreException("Invalid section '" + sectionDoc.getSectionId() + "'!",
                StatusCode.BAD_REQUEST.getStatusCode());
      }
    }
    return allSections;
  }

  /**
   * Validate section document.
   *
   * @param pSection Section document.
   * @return valid or not.
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateSectionDocument(SectionDocument pSection) throws MetaStoreException {
    String hashValue = MetaStoreUtility.getHashValue(pSection.getType());
    boolean validateSection = false;
    try {
      validateSection = validateNode(pSection.getRootNode(), hashValue);
    } catch (MetaStoreException ex) {
      throw new MetaStoreException("Error while validating document for namespace '" + pSection.getType() + "'! - " + ex.getMessage(), ex);
    }

    return validateSection;
  }

  /**
   * Validate xml document.
   *
   * @param pRootNode Root node of the document.
   * @return valid or not.
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateNode(Node pRootNode) throws MetaStoreException {
    String hashValue = MetaStoreUtility.getHashValue(pRootNode.getNamespaceURI());

    return validateNode(pRootNode, hashValue);

  }

  /**
   * Validate xml document.
   *
   * @param pRootNode Root node of the document.
   * @param pHashOfXsd Hash of the xsd file.
   * @return valid or not.
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateNode(Node pRootNode, String pHashOfXsd) throws MetaStoreException {
    String xsdString = arango.getRegisteredXsd(pHashOfXsd);
    String xmlString = XmlUtility.xmlToString(pRootNode);
    return XmlUtility.validate(xmlString, xsdString);
    // return validateAgainstXsd(xsdString, xmlString);
  }
  // </editor-fold>

  /**
   * Get all digital object ids of mets documents with matching search term.
   *
   * @param searchTerm search term
   * @return Collection with all matching digital object ids.
   * @throws MetaStoreException an error occurred.
   */
  public Set<String> searchFullTextWithId(String searchTerm) throws MetaStoreException {

    Set<String> digitalObjID = new HashSet<>();

    for (IndexEntity indexEntity : arango.getIndexes().getIndexes()) {
      if (indexEntity.getType().toString().equalsIgnoreCase("fulltext")) {
        // below method is used to get all the
        DocumentCursor<ArangoDriver> cursuroResults = arango.applyFullTextSearch(indexEntity.getFields().get(0),
                searchTerm);
        for (DocumentEntity<ArangoDriver> documentEntity : cursuroResults) {
          digitalObjID.add(arango.getDigitalObjectIdForDocument(documentEntity.getDocumentKey()));
        }
      }
    }
    return digitalObjID;
  }

  /**
   * Segregate/Divide xml to different documents.
   *
   * @param incomingXML xml containing whole METS file
   * @param documentHandler handler?
   * @throws MetaStoreException An error occurred.
   */
  public void segregateNStoreJson(String incomingXML, String documentHandler) throws MetaStoreException {

    try {

      InputSource source = new InputSource(new StringReader(incomingXML));

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(true);
      DocumentBuilder dBuilder;

      dBuilder = dbFactory.newDocumentBuilder();

      Document doc = dBuilder.parse(source);
      doc.getDocumentElement().normalize();

      // XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "//*[local-name()='mets']/*";
      // this the expression to break the xml in different groups
      XPath xPath = XPathFactory.newInstance().newXPath();
      NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); i++) {

        MetsArangoPOJO metsPOJO = new MetsArangoPOJO();

        Node node = nodeList.item(i);
        if (node.getNodeName().replaceAll(METS_PREFIX, "").equalsIgnoreCase("metsHdr")
                || node.getNodeName().replaceAll(METS_PREFIX, "").equalsIgnoreCase("fileSec")
                || node.getNodeName().replaceAll(METS_PREFIX, "").equalsIgnoreCase("structMap")) {

        } else {

          StringWriter writer1 = new StringWriter();
          Transformer transformer = TransformerFactory.newInstance().newTransformer();

          transformer.transform(new DOMSource(node), new StreamResult(writer1));

          JSONObject str2json = XML.toJSONObject(writer1.toString());
          metsPOJO.setXmlData(writer1.toString());
          metsPOJO.setMainXmlHandler(documentHandler);
          metsPOJO.setSections(node.getNodeName().replaceAll(METS_PREFIX, ""));
          metsPOJO.setId(node.getAttributes().getNamedItem("ID").getNodeValue().toString());

          String xmlKey = arango.storeXmlDocument(metsPOJO);
          String finalStr = "{\"type\":\"" + node.getNodeName() + "\",\"json\":" + str2json
                  + ",\"mainXmlHandler\":\"" + documentHandler + "\"}";
          arango.storeJSONRawDocument(xmlKey, finalStr);

          // Applying Indexing operations
          JSONObject getStoredJson = arango.getJsonObject(xmlKey);
          Set<String> indexSet = MetaStoreUtility.applyIndexing(getStoredJson);
          arango.applyIndexes(indexSet);
        }
      }

    } catch (IOException | ParserConfigurationException | TransformerException | XPathExpressionException
            | JSONException | DOMException | SAXException e) {
      throw new MetaStoreException(e);
    }

  }
}
