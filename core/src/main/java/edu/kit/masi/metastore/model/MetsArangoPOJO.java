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

/**
 * Data object holding all properties of a METS file.
 * @author hartmann-v
 */
public class MetsArangoPOJO {
  /** 
   * Holding the complete METS file.
   */
	private String xmlData;
  /** 
   * ID of the document.
   */
	private String mainXmlHandler;
  /** 
   * Section of the document.
   * Name of the node!?
   */
	private String sections;
  /**
   * Section ID of the object.
   */
	private String id;
	/**
   * Get section name.
   * @return section name.
   */
	public String getSections() {
		return sections;
	}
  /**
   * Set section name.
   * @param sections  section name.
   */
	public void setSections(String sections) {
		this.sections = sections;
	}
  /**
   * Get ID.
   * @return ID 
   */
	public String getId() {
		return id;
	}
  /**
   * Set ID.
   * @param id ID
   */
	public void setId(String id) {
		this.id = id;
	}
  /**
   * Get ID of the document.
   * @return Document ID
   */
	public String getMainXmlHandler() {
		return mainXmlHandler;
	}
  /**
   * Set ID of the document.
   * @param mainXmlHandler Document ID
   */
	public void setMainXmlHandler(String mainXmlHandler) {
		this.mainXmlHandler = mainXmlHandler;
	}
  /**
   * Get content of section.
   * @return  Content of section.
   */
	public String getXmlData() {
		return xmlData;
	}
  /**
   * Set content of section.
   * @param xmlData Content of section.
   */
	public void setXmlData(String xmlData) {
		this.xmlData = xmlData;
	}

}
