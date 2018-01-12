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
package edu.kit.masi.plugin.metastore;

import edu.kit.masi.metastore.control.IMetaStoreController;
import edu.kit.masi.plugin.IServicePlugin;

/**
 * Interface for storing METS in MetaStore.
 * Planned implementations (2017/05):
 * MetaStore
 * @author hartmann-v
 */
public interface IMetaStorePlugin extends IServicePlugin, IMetaStoreController {
  
  /**
   * Base path for service declaration and configuration.
   */
  String MODULE_NAME = "module.metastore";
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

}
