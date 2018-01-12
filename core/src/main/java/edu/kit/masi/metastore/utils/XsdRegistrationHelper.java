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

import org.fzk.tools.xml.JaxenUtil;
import org.jdom.Document;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for registering XSD files.
 *
 * @author hartmann-v
 */
public class XsdRegistrationHelper {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XsdRegistrationHelper.class);

  /**
   * Extract the target namespace of an XSD file.
   *
   * @param fileContent Content of XSD file as string.
   * @return Target namespace of XSD file.
   */
  public static String getNamespaceFromXsd(String fileContent) {
    String namespace = "no namespace defined!";
    try {
      Document document = JaxenUtil.getDocument(fileContent);
      Namespace[] namespaces = {Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema")};
      namespace = JaxenUtil.getAttributeValue(document, "/xs:schema/@targetNamespace", namespaces);
    } catch (Exception ex) {
      LOGGER.error(null, ex);
    }
    return namespace;
  }

}
