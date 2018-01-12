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

import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.model.ReturnType;

/**
 * Controller managing all services.
 *
 * @author hartmann-v
 */
public interface IMetaStoreController {

  /**
   * Register XSD file to meta store. Minimum role is MANAGER!
   *
   * @param pXsdDocument Content of XSD file.
   * @param pPrefix Prefix of the namespace (has to be unique)
   * @return Target namespace of the registered XSD.
   * @throws MetaStoreException An error occurred.
   */
  public String registerXsdDocument(String pXsdDocument, String pPrefix) throws MetaStoreException;
  /**
   * Get METS file from meta store. Minimum role is GUEST!
   *
   * @param pDigitalObjectId The id of the METS document.
   * @param returnType xml or json.
   * @return Mets document as JSON
   * @throws MetaStoreException An error occurred.
   */
  public String getMetsDocument(String pDigitalObjectId, ReturnType returnType) throws MetaStoreException;
  /**
   * Store METS file in meta store. Minimum role is MEMBER!
   *
   * @param pMetsDocument Content of METS file.
   * @param pDigitalObjectId The id of the METS document.
   * @return Message that document was successfully stored.
   * @throws MetaStoreException An error occurred.
   */
  public String storeMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException;

  /**
   * Update existing METS file in meta store. Minimum role is MEMBER!
   *
   * @param pMetsDocument Content of METS file.
   * @param pDigitalObjectId The id of the METS document.
   * @return Message for successful update.
   * @throws MetaStoreException An error occurred.
   */
  public String updateMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException;

  /**
   * Get existing section of METS file in meta store. Minimum role is GUEST!
   *
   * @param pType Type (namespace) of the section.
   * @param pDigitalObjectId The id of the METS document.
   * @param returnType xml or json.
   * @return Selected section of METS document.
   * @throws MetaStoreException An error occurred.
   */
  public String getPartialMetsDocument(String pType, String pDigitalObjectId, ReturnType returnType)
          throws MetaStoreException;
  /**
   * Update existing section of METS file in meta store. Minimum role is
   * MEMBER!
   *
   * @param pSectionDocument Content of new nested section for METS file.
   * @param pDigitalObjectId The id of the METS document.
   * @param pSectionId id of the section holding the nested section
   * @return Message that section was updated successfully.
   * @throws MetaStoreException An error occurred.
   */
  public String updatePartialMetsDocument(String pSectionDocument, String pDigitalObjectId, String pSectionId)
          throws MetaStoreException;

  /**
   * Validate XML against registered XSD. Minimum role is GUEST!
   *
   * @param pXmlDocument Content of METS file.
   * @return true if valid.
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateDocument(String pXmlDocument) throws MetaStoreException;
  /**
   * Search for all mets documents matching the given term.
   *
   * @param searchTerm search term.
   * @param maxNoOfHits maximum number of hits.
   * @param returnType xml or json.
   * @return All fitting mets documents as JSON array.
   * @throws MetaStoreException An error occurred.
   */
  public String searchForMetsDocuments(String searchTerm, int maxNoOfHits, ReturnType returnType)
          throws MetaStoreException;
  /**
   * Get registered XSD by namespace.
   *
   * @param pNamespace target namespace of XSD.
   * @return XSD file as string.
   * @throws MetaStoreException An error occurred.
   */
  public String getXsdAsString(String pNamespace) throws MetaStoreException;
}
