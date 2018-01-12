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
package edu.kit.masi.plugin.metastore.impl;

import edu.kit.dama.util.DataManagerSettings;
import java.util.ServiceLoader;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.kit.masi.plugin.metastore.IMetaStorePlugin;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hartmann-v
 */
public class MetaStorePluginFactory {

  /**
   * Logger.
   */
  protected static final Logger LOGGER = LoggerFactory.getLogger(MetaStorePluginFactory.class);
  /**
   * Map managing all plugins. Each plugin should be initialized only once.
   */
  private static final Map<String, IMetaStorePlugin> allPlugins = new HashMap<>();

  /**
   * Get MetaStorePlugin configured in datamanager.xml. If no MetaStorePlugin is
   * defined inside datamanager.xml there should only one implementation of
   * IMetaStorePlugin interface available. If there are multiple a warning is
   * printed and the last implementation in classpath will be returned.
   *
   * @return instance of a MetaStorePlugin
   */
  public static IMetaStorePlugin getMetaStorePlugin() {
    IMetaStorePlugin selectedPlugin = null;
    // Read default plugin if defined in datamanager.xml
    String implementationName = DataManagerSettings.getSingleton().getStringProperty(IMetaStorePlugin.NAME, null);
    String implementationVersion = DataManagerSettings.getSingleton().getStringProperty(IMetaStorePlugin.VERSION, null);
    Configuration subConfiguration = DataManagerSettings.getSingleton().getSubConfiguration(IMetaStorePlugin.CONFIGURATION);

    LOGGER.info("Looking for implementation of service 'IMetaStorePlugin'!");
    for (IMetaStorePlugin plugin : ServiceLoader.load(IMetaStorePlugin.class)) {
      String name = plugin.getName();
      String version = plugin.getVersion();
      LOGGER.debug("Found implementation: " + name + " - Version: " + version);
      if (name.equals(implementationName) && version.equals(implementationVersion)) {
        selectedPlugin = plugin;
        break;
      }
      if ((implementationName == null) && (implementationVersion == null)) {
        if (selectedPlugin != null) {
          LOGGER.error("Multiple implementations of service 'IMetaStorePlugin' found but no configuration found in 'datamanager.xml'!?");
        }
        selectedPlugin = plugin;
      }
    }
    // use selected plugin...
    return getPlugin(selectedPlugin, subConfiguration);
  }

  /**
   * Get plugin as a singleton.
   *
   * @param pPlugin plugin which should be checked
   * @param pConfig configuration if plugin is not already initialized.
   * @return plugin.
   */
  private static synchronized IMetaStorePlugin getPlugin(IMetaStorePlugin pPlugin, Configuration pConfig) {
    IMetaStorePlugin selectedPlugin = null;
    if (pPlugin != null) {
      LOGGER.info("Selected implementation: " + pPlugin.getName() + " - Version: " + pPlugin.getVersion());
      String key = pPlugin.getName() + "_" + pPlugin.getVersion();
      if (!allPlugins.containsKey(key)) {
        pPlugin.configureService(pConfig);
        allPlugins.put(key, pPlugin);
      }
      selectedPlugin = allPlugins.get(key);
    } else {
      LOGGER.info("No plugin found!");
    }

    return selectedPlugin;
  }
}
