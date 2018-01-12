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
package edu.kit.masi.metastore.init;

import edu.kit.masi.metastore.control.MetaStoreController;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.metastore.utils.KitDataManagerUtil;
import edu.kit.masi.metastore.utils.XsdUtil;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializing MetaStore via ServletContext. Parameterization will be done via
 * context parameter 'initMetaStore'. The parameter has to have the following
 * format: prefix1:filenameOfXSD1,prefix2:filenameOfXSD2,... The XSD files
 * should be located in WEB-INF/classes/xsd
 *
 * @author hartmann-v
 */
public class InitializeMetaStore implements ServletContextListener {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(InitializeMetaStore.class);

  /** 
   * Init parameter.
   */
  private static final String CONFIG_PARAM = "initMetaStore";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    String initParameter = sce.getServletContext().getInitParameter(CONFIG_PARAM);
    String[] prefixXsdPairs = initParameter.split(",");
    for (String pair : prefixXsdPairs) {
      String[] split = pair.split(":");
      String prefix = split[0].trim();
      String filename = "xsd/" + split[1].trim();
      testAndRegisterXsd(prefix, filename);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // do nothing
  }

  /**
   * Test settings for given prefix and filename.
   * If prefix/namespace is not already defined register 
   * prefix with namespace. In case of error please look at the
   * log file.
   * @param pPrefix Prefix of the namespace.
   * @param pFilename Filename holding XSD file.
   */
  private void testAndRegisterXsd(String pPrefix, String pFilename) {
    String returnValue = "Success";
    try {
      MetaStoreController metaStoreController = new MetaStoreController();
      boolean prefixExists = KitDataManagerUtil.getAllPrefixes().contains(pPrefix);
      boolean registerNewSchema = true;
      String targetNamespace;
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(pFilename);
      String pXsdDocument = IOUtils.toString(inputStream);
      // 1. Prefix not defined
      // 1.1 Check if namespace is registered and get prefix
      // 2. Prefix defined
      // 2.1 Check if namespace is registered and get prefix
      // 2.2 Prefix should be identical
      XsdUtil xsdUtil = new XsdUtil();
      xsdUtil.getNamespaceAndVersionFromXsd(pXsdDocument);
      targetNamespace = xsdUtil.getNamespace();
      String newPrefix = KitDataManagerUtil.getPrefixForNamespace(targetNamespace);
      if (newPrefix == null) {
        if (pPrefix == null) {
          throw new MetaStoreException("Please provide a prefix for Namespace '" + targetNamespace + "'!", StatusCode.BAD_REQUEST.getStatusCode());
        } else {
          if (prefixExists) {
            //Prefix already in use
            throw new MetaStoreException("Prefix already in use for namespace '" + targetNamespace + "'", StatusCode.BAD_REQUEST.getStatusCode());
          }
          newPrefix = pPrefix;
        }
      } else {
        // namespace already registered with prefix newPrefix
        if (!newPrefix.equalsIgnoreCase(pPrefix)) {
          if (pPrefix != null) {
            throw new MetaStoreException("Namespace '" + targetNamespace + "' already registered with prefix '" + newPrefix + "'!", StatusCode.BAD_REQUEST.getStatusCode());
          }
        }
        // Prefix already registered
        LOGGER.debug("Prefix '{}' and matching namespace '{}' are already registered at KIT Data Manager.", newPrefix, targetNamespace);
        registerNewSchema = false;

      }

      LOGGER.debug("Store XSD for namespace '{}' in MetaStore.", targetNamespace);

      returnValue = metaStoreController.registerXsdDocument(pXsdDocument, newPrefix);

      if (registerNewSchema) {
        LOGGER.debug("Register prefix '{}' with namespace '{}' at KIT Data Manager.", newPrefix, targetNamespace);
        KitDataManagerUtil.registerNewSchema(newPrefix, targetNamespace);
      }
      LOGGER.info(returnValue);
    } catch (MetaStoreException mse) {
      LOGGER.error("Error registering XSD!", mse);
    } catch (IOException ioe) {
      LOGGER.error("Error reading file '" + pFilename + "'!", ioe);
    }
  }

}
