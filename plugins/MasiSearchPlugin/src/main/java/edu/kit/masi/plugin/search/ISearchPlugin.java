/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.masi.plugin.search;

import edu.kit.masi.plugin.IServicePlugin;

/**
 * Interface to separate code from implementation.
 * As this interface extends IServicePlugin new Plugins
 * can be supported 'on the fly' without recompiling 
 * code. 
 * All search methods should always only return IDs
 * of the matching documents. 
 * Filtering will be done later. 
 * (In case of proprietary queries it may also be done in beforehand.)
 * @see MasiServicePlugin
 * @see edu.kit.masi.plugin.IServicePlugin
 * @author hartmann-v
 */
public interface ISearchPlugin extends IServicePlugin {
  
  /**
   * Base path for service declaration and configuration.
   */
  String MODULE_NAME = "module.searchPlugin";
  /**
   * XPath for name of service inside datamanager.xml.
   */
  String NAME = MODULE_NAME + ".name";
  /**
   * XPath for version of service inside datamanager.xml.
   */
  String VERSION = MODULE_NAME + ".version";

  /**
   * How to combine multiple terms.
   */
  static enum Combination {
    DISJUNCTION,
    CONJUNCTION;
  };
  
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
   * Full text search for given search terms.
   * Disjunction: At least one of the search terms has to fit.
   * Conjunction: All search terms have to fit.
   * @param pCombination dis- or conjunction
   * @param pValues Search terms.
   * @return IDs of all fitting documents.
   */
  String[] searchForMets(Combination pCombination, String[] pValues);
   /**
   * Full text search for given search terms.
   * Disjunction: At least one of the search terms has to fit.
   * Conjunction: All search terms have to fit.
   * @param pCombination dis- or conjunction
   * @param types Restrict to given types of documents 
   * @param pValues Search terms.
   * @return IDs of all fitting documents.
   */
  String[] searchForMets(Combination pCombination, String[] types, String[] pValues);
  /**
   * Proprietary search using query language of the used search engine.
   * @param query Query string.
   * @return IDs of all fitting documents.
   */
  String[] search(String query);
  /**
   * Proprietary search using query language of the used search engine.
   * @param types Restrict to given types of documents 
   * @param query Query string.
   * @return IDs of all fitting documents.
   */
  String[] search(String[] types, String query);
  
}
