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
package edu.kit.masi.plugin.search.impl;

import edu.kit.dama.util.DataManagerSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.kit.masi.plugin.search.ISearchPlugin;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class looking for plugins implementing ISearchPlugin interface.
 *
 * @author hartmann-v
 */
public class SearchPluginFactory {

  /**
   * Logger.
   */
  protected static final Logger LOGGER = LoggerFactory.getLogger(SearchPluginFactory.class);
  /**
   * Map managing all plugins. Each plugin should be initialized only once.
   */
  private static final Map<String, ISearchPlugin> allPlugins = new HashMap<>();

  static {
    LOGGER.info("SearchPluginFactory initialized!");
  }

  /**
   * Get search plugin configured in datamanager.xml. If no search plugin is defined
   * inside datamanager.xml there should only one implementation of ISearchPlugin
   * interface available. If there are multiple a warning is printed and the
   * last implementation in classpath will be returned.
   *
   * @return instance of a SearchPlugin
   */
  public static ISearchPlugin getSearchPlugin() {
    ISearchPlugin selectedPlugin = null;
    // Read default plugin if defined in datamanager.xml
    String implementationName = DataManagerSettings.getSingleton().getStringProperty(ISearchPlugin.NAME, null);
    String implementationVersion = DataManagerSettings.getSingleton().getStringProperty(ISearchPlugin.VERSION, null);

    LOGGER.info("Looking for implementation of service 'ISearchPlugin'by configuration: '{}'/'{}'!", implementationName, implementationVersion);
    for (ISearchPlugin plugin : ServiceLoader.load(ISearchPlugin.class)) {
      String name = plugin.getName();
      String version = plugin.getVersion();
      LOGGER.debug("Found implementation: " + name + " - Version: " + version);
      if (name.equals(implementationName) && version.equals(implementationVersion)) {
        selectedPlugin = plugin;
        break;
      }
      if ((implementationName == null) && (implementationVersion == null)) {
        if (selectedPlugin != null) {
          LOGGER.error("Multiple implementations of plugin 'ISearchPlugin' found but no configuration found in 'datamanager.xml'!?");
        }
        selectedPlugin = plugin;
      }
    }

    // use selected plugin...
    return getPlugin(selectedPlugin);
  }

  /**
   * Get search plugin configured in datamanager.xml defined by its service name.
   *
   * @param pServiceName service name
   * @return instance of a search Plugin
   */
  public static ISearchPlugin getSearchPlugin(String pServiceName) {
    ISearchPlugin selectedPlugin = null;
    if (pServiceName == null) {
      return getSearchPlugin();
    }
    LOGGER.info("Looking for implementation of service 'ISearchPlugin' defined by service name '{}'.", pServiceName);
    for (ISearchPlugin plugin : ServiceLoader.load(ISearchPlugin.class)) {
      String name = plugin.getName();
      String version = plugin.getVersion();
      String serviceName = plugin.getServiceName();
      LOGGER.debug("Found implementation: '{}' - version: {} --> '{}'", name, version, serviceName);
      if (pServiceName.equals(serviceName)) {
        selectedPlugin = plugin;
        break;
      }
    }
    
    // use selected plugin...
    return getPlugin(selectedPlugin);
  }

  /**
   * Get a list of all Search plugins.
   *
   * @return List with all service names of found plugins.
   */
  public static List<String> getPluginNames() {
    List<String> allServices = new ArrayList<>();

    LOGGER.info("Looking for all implementations.");
    for (ISearchPlugin plugin : ServiceLoader.load(ISearchPlugin.class)) {
      String name = plugin.getName();
      String version = plugin.getVersion();
      String serviceName = plugin.getServiceName();
      LOGGER.debug("Found implementation: '{}' - version: {} --> '{}'", name, version, serviceName);
      if (serviceName != null) {
        allServices.add(serviceName);
      }
    }

    return allServices;
  }

  /**
   * Get plugin as a singleton.
   *
   * @param pPlugin plugin which should be checked
   * @return plugin.
   */
  private static synchronized ISearchPlugin getPlugin(ISearchPlugin pPlugin) {
    ISearchPlugin selectedPlugin = null;
    if (pPlugin != null) {
      LOGGER.info("Selected implementation: " + pPlugin.getName() + " - Version: " + pPlugin.getVersion());
      String key = pPlugin.getName() + "_" + pPlugin.getVersion();
      if (!allPlugins.containsKey(key)) {
        Configuration subConfiguration = DataManagerSettings.getSingleton().getSubConfiguration(pPlugin.getConfigurationPath());
        pPlugin.configureService(subConfiguration);
        allPlugins.put(key, pPlugin);
      }
      selectedPlugin = allPlugins.get(key);
    } else {
      LOGGER.info("No search plugin found!");
    }

    return selectedPlugin;
  }
}
