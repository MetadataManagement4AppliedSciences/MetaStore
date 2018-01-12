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
package edu.kit.masi.metastore.utils;

import edu.kit.dama.authorization.entities.impl.AuthorizationContext;
import edu.kit.dama.authorization.exceptions.UnauthorizedAccessAttemptException;
import edu.kit.dama.mdm.base.MetaDataSchema;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hartmann-v
 */
public class KitDataManagerUtil {

  /**
   * Logger for debugging purposes.
   */
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KitDataManagerUtil.class);

  /**
   * Get linked namespace for given prefix.
   *
   * @param pPrefix Prefix of namespace
   * @return Namespace linked with prefix.
   * @throws MetaStoreException An error occurred.
   */
  public static String getNamespaceFromPrefix(String pPrefix) throws MetaStoreException {
    String namespace = null;
    IMetaDataManager metaDataManager = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
    try {
      metaDataManager.setAuthorizationContext(AuthorizationContext.factorySystemContext());
      List<MetaDataSchema> find = metaDataManager.find(MetaDataSchema.class);
      for (MetaDataSchema schema : find) {
        if (schema.getSchemaIdentifier().equalsIgnoreCase(pPrefix)) {
          namespace = schema.getNamespace();
          break;
        }
      }
      if (namespace == null) {
        throw new MetaStoreException("Prefix '" + pPrefix + "' not registered yet!", StatusCode.BAD_REQUEST.getStatusCode());
      }
    } catch (UnauthorizedAccessAttemptException ex) {
      LOGGER.error("Error reading Metadata schemas from database", ex);
      throw new MetaStoreException("Unauthorized access", ex);
    }
    metaDataManager.close();

    return namespace;
  }

  /**
   * Check if namespace already registered to KIT Data Manager.
   *
   * @param pNamespace Namespace of the schema
   * @return Prefix of namespace or null if not registered yet.
   * @throws MetaStoreException An error occurred.
   */
  public static String getPrefixForNamespace(String pNamespace) throws MetaStoreException {
    String prefix = null;
    LOGGER.debug("Get prefix for namespace '{}'", pNamespace);
    IMetaDataManager metaDataManager = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
    try {
      metaDataManager.setAuthorizationContext(AuthorizationContext.factorySystemContext());
      List<MetaDataSchema> find = metaDataManager.find(MetaDataSchema.class);
      for (MetaDataSchema schema : find) {
        if (schema.getNamespace().equalsIgnoreCase(pNamespace)) {
          prefix = schema.getSchemaIdentifier();
          break;
        }
      }
      LOGGER.debug("No matching namespace found!");
    } catch (UnauthorizedAccessAttemptException ex) {
      LOGGER.error("Error reading Metadata schemas from database", ex);
      throw new MetaStoreException("Unauthorized access", ex);
    }
    metaDataManager.close();

    return prefix;
  }

  /**
   * Get all registered prefixes.
   *
   * @return List with all registered prefixes.
   * @throws MetaStoreException An error occurred.
   */
  public static List<String> getAllPrefixes() throws MetaStoreException {
    List<String> prefixes = new ArrayList<>();
    IMetaDataManager metaDataManager = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
    try {
      metaDataManager.setAuthorizationContext(AuthorizationContext.factorySystemContext());
      List<MetaDataSchema> find = metaDataManager.find(MetaDataSchema.class);
      for (MetaDataSchema schema : find) {
        prefixes.add(schema.getSchemaIdentifier());
        LOGGER.debug(schema.getSchemaIdentifier() + ": " + schema.getNamespace());
      }
    } catch (UnauthorizedAccessAttemptException ex) {
      LOGGER.error("Error reading Metadata schemas from database", ex);
      throw new MetaStoreException("Unauthorized access", ex);
    }
    metaDataManager.close();

    return prefixes;
  }

  /**
   * Register a new schema.
   *
   * @param pPrefix Prefix of the new schema.
   * @param pNamespace Namespace of the new schema.
   * @throws MetaStoreException An error occurred.
   */
  public static void registerNewSchema(String pPrefix, String pNamespace) throws MetaStoreException {
    IMetaDataManager metaDataManager = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
    try {
      metaDataManager.setAuthorizationContext(AuthorizationContext.factorySystemContext());
      MetaDataSchema mds = new MetaDataSchema(pPrefix);
      mds.setNamespace(pNamespace);
      metaDataManager.save(mds);
    } catch (UnauthorizedAccessAttemptException ex) {
      LOGGER.error("Error writing new schema for prefix '" + pPrefix + "' and namespace '" + pNamespace + "'", ex);
      throw new MetaStoreException("Unauthorized access", ex);
    }
    metaDataManager.close();

    return;
  }

}
