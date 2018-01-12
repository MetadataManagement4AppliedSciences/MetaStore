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
package edu.kit.masi.metastore.model;

import org.w3c.dom.Node;

/**
 * Document holding xmlData of METS document.
 * fileSec and structMap have to be handled in a proprietary way.
 *
 * @author hartmann-v
 */
public class SectionDocument {
  /**
   * Unique ID of 'parent' xml.
   */
  private String digitalObjectId;
  /**
   * Id of the section.
   */
  private String sectionId;
  /**
   * Root node holding the xml data of this section.
   */
  private Node rootNode;

  /**
   * Constructor.
   *
   * @param pDigitalObjectId ID of the mets file.
   * @param sectionId Section ID
   * @param rootNode Root node of the section.
   */
  public SectionDocument(String pDigitalObjectId, String sectionId, Node rootNode) {
    this.digitalObjectId = pDigitalObjectId;
    this.sectionId = sectionId;
    this.rootNode = rootNode;
  }

  /**
   * Get ID of corresponding mets file.
   *
   * @return the digitalObjectId
   */
  public String getDigitalObjectId() {
    return digitalObjectId;
  }

  /**
   * Get section ID.
   *
   * @return the sectionId
   */
  public String getSectionId() {
    return sectionId;
  }

  /**
   * Get root node of section.
   *
   * @return the rootNode
   */
  public Node getRootNode() {
    return rootNode;
  }
  /**
   * Get namespace URI of section document.
   * @return  get namespace.
   */
  public String getType() {
    return rootNode.getNamespaceURI();
  }
}
