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
package edu.kit.masi.plugin.index;

import edu.kit.masi.plugin.IServicePlugin;
import java.util.List;

/**
 * Interface for indexing JSON document.
 * Planned implementation (2017/10):
 * elasticsearch
 * @author hartmann-v
 */
public interface IIndexPlugin extends IServicePlugin {
  
  /**
   * Base path for service declaration and configuration.
   */
  String MODULE_NAME = "module.indexPlugin";
  /**
   * XPath for name of service inside datamanager.xml.
   */
  String NAME = MODULE_NAME + ".name";
  /**
   * XPath for version of service inside datamanager.xml.
   */
  String VERSION = MODULE_NAME + ".version";
  /**
   * XPath for configuration of service inside datamanager.xml.
   */
  String CONFIGURATION = MODULE_NAME + ".configuration";
  
  /** 
   * Get name of the service.
   * @return service name (should be unique)
   */
  String getServiceName();
  
  /**
   * XPath for configuration of service with service name inside datamanager.xml.
   * @return XPath for configuration depends on service name.
   */
  default String getConfigurationPath() {
    return MODULE_NAME + "." + getServiceName() + ".configuration";
  }
  
  /**
   * Index JSON document for search.
   *
   * @param pJsonDoc JSON document holding metadata.
   * @param pDocumentId Id of the document. (Needed for updates of document.)
   * @param pType Type of the document.
   * @return True if indexing succeeds.
   */
  public boolean indexJsonDocument(String pJsonDoc, String pDocumentId, String pType);
   
}
