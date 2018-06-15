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
package edu.kit.masi.metastore.plugin.impl;

import edu.kit.masi.metastore.control.MetaStoreController;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.model.ReturnType;
import edu.kit.masi.plugin.AbstractServicePlugin;
import edu.kit.masi.plugin.metastore.IMetaStorePlugin;
import org.apache.commons.configuration.Configuration;

/**
 * Plugin for accessing the metastore.
 * 
 * @author hartmann-v
 */
public class MetaStorePlugin extends AbstractServicePlugin implements IMetaStorePlugin {
  /**
   * Version string of this implementation.
   */
  private static final String VERSION = "1.0";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void configureService(Configuration pConfig) {
    // no configuration neccessary
  }

  @Override
  public String registerXsdDocument(String pXsdDocument, String pPrefix) throws MetaStoreException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getMetsDocument(String pDigitalObjectId, ReturnType returnType) throws MetaStoreException {
    return new MetaStoreController().getMetsDocument(pDigitalObjectId, returnType);
  }

  @Override
  public String storeMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException {
    return new MetaStoreController().storeMetsDocument(pMetsDocument, pDigitalObjectId);
  }

  @Override
  public String updateMetsDocument(String pMetsDocument, String pDigitalObjectId) throws MetaStoreException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getPartialMetsDocument(String pType, String pDigitalObjectId, ReturnType returnType) throws MetaStoreException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String updatePartialMetsDocument(String pSectionDocument, String pDigitalObjectId, String pSectionId) throws MetaStoreException {
    return new MetaStoreController().updatePartialMetsDocument(pSectionDocument, pDigitalObjectId, pSectionId);
  }

  @Override
  public boolean validateDocument(String pXmlDocument) throws MetaStoreException {
    return new MetaStoreController().validateDocument(pXmlDocument);
  }

  @Override
  public String searchForMetsDocuments(String searchTerm, int maxNoOfHits, ReturnType returnType) throws MetaStoreException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getXsdAsString(String pNamespace) throws MetaStoreException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
