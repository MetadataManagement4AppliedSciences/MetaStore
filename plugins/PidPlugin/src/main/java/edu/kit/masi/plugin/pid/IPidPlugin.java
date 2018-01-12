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
package edu.kit.masi.plugin.pid;

import edu.kit.masi.plugin.IServicePlugin;
import java.util.List;

/**
 * Interface for creating new PID.
 * Planned implementation (2017/03):
 * EPIC PID from handle
 * @author hartmann-v
 */
public interface IPidPlugin extends IServicePlugin {
  
  /**
   * Base path for service declaration and configuration.
   */
  String MODULE_NAME = "module.pidPlugin";
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
   * Get the prefix of the PID.
   * This is the prefix of the URL for the PID.
   * e.g.: https://hdl.handle.net/ for EPIC
   * This may be used to find the according service.
   * @return prefix of PID.
   */
  String getPrefixPidUrl();
  
  /** 
   * Create unique identifier for digital object.
   * @param digitalObjectIdentifier identifier of the digital object.
   * @return unique identifier.
   */
  String createUniqueIdentifier(String digitalObjectIdentifier);
  
  /**
   * XPath for configuration of service with service name inside datamanager.xml.
   * @return XPath for configuration depends on service name.
   */
  default String getConfigurationPath() {
    return MODULE_NAME + "." + getServiceName() + ".configuration";
  }
  
  /**
   * Get all attributes values of PID with given key.
   *
   * @param pPid PID as URL
   * @param pKey Key of the attribute.
   * @return All attributes of given PID.
   */
  public List<String> getAttributeValues(String pPid, String pKey);
   
  /**
   * Get keys of all attributes of PID.
   *
   * @param pPid PID as URL
   * @return All attributes of given PID.
   */
  public List<String> getAttributeKeys(String pPid);
  
  /**
   * Create a new attribute.
   * @param pPid PID
   * @param pAttribute attribute of the PID
   * @param pValue old value of the attribute
   */
  void addAttribute(String pPid, String pAttribute, String pValue);
  
  /**
   * Update an existing attribute.
   * @param pPid PID
   * @param pAttribute attribute of the PID
   * @param pOldValue old value of the attribute
   * @param pNewValue new value of the attribute
   */
  void updateAttribute(String pPid, String pAttribute, String pOldValue, String pNewValue);
}
