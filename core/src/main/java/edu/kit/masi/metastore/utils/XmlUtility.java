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

import edu.kit.masi.metastore.exception.MetaStoreException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Utility class handling all xml specific things.
 * @author hartmann-v
 */
public class XmlUtility {
/**
   * Logger.
   */
	public static final Logger LOGGER = LoggerFactory.getLogger(XmlUtility.class);

  /** 
   * Transform Stream into string.
   * @param pInputStream stream from source.
   * @return String containing stream content.
   * @throws MetaStoreException An error occurred.
   */
  public static String inputStreamToString(InputStream pInputStream) throws MetaStoreException {
    MetaStoreUtility.LOGGER.info("----Incoming XSD");
    try {
      StringWriter writer = new StringWriter();
      IOUtils.copy(pInputStream, writer, "UTF-8");
      return writer.toString();
    } catch (IOException e) {
      e.printStackTrace();
      throw new MetaStoreException(e.getMessage());
    }
  }

  /**
   * Validate xml document with provided xsd document.
   * @param xmlString xml document as string.
   * @param xsdString xsd document as string.
   * @return valid or not.
   * @throws MetaStoreException  An error occurred.
   */
  public static boolean validate(String xmlString, String xsdString) throws MetaStoreException {
    boolean isValid = false;
    try {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Source xsd = new StreamSource(new StringReader(xsdString));
      Schema schema = factory.newSchema(xsd);
      Validator validator = schema.newValidator();
      Source xml = new StreamSource(new StringReader(xmlString));
      validator.validate(xml);
      LOGGER.debug("validate internal xml file maybe using jaxb or predefined using DOM parser");
      isValid = true;
    } catch (IOException | SAXException e) {
      LOGGER.error("Error while validating", e);
      LOGGER.error("Document: " + xmlString);
      throw new MetaStoreException("Document is not valid! - " + e.getMessage());
    }
    return isValid;
  }

  /**
   * Transform string to W3C document.
   * @param xmlString XML as string.
   * @return Document.
   * @throws MetaStoreException  An error occurred.
   */
  public static Document strToXmlDocument(String xmlString) throws MetaStoreException {
    try {
      ByteArrayInputStream input = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(true);
      DocumentBuilder dBuilder;
      dBuilder = dbFactory.newDocumentBuilder();
      return dBuilder.parse(input);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new MetaStoreException("Error while parsing String: " + xmlString, e);
    }
  }
  /**
   * Write xml document as string.
   * @param doc2 XML document.
   * @return String representation of XML document.
   * @throws MetaStoreException An error occurred.
   */
  public static String xmlToString(Document doc2) throws MetaStoreException {
    return xmlToString(doc2.getDocumentElement());
  }

  /**
   * Write xml node as string.
   * @param node XML node.
   * @return String representation of XML document.
   * @throws MetaStoreException An error occurred.
   */
  public static String xmlToString(Node node) throws MetaStoreException {
    try {
      Source source = new DOMSource(node);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);
      return stringWriter.getBuffer().toString();
    } catch (TransformerException e) {
      throw new MetaStoreException("Error tranforming node to string! - " + e.getMessage(), e);
    }
  }
  
}
