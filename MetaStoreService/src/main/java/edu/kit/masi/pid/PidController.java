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
package edu.kit.masi.pid;

import com.sun.jersey.api.core.HttpContext;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.entities.Role;
import edu.kit.dama.authorization.entities.impl.AuthorizationContext;
import edu.kit.dama.authorization.exceptions.UnauthorizedAccessAttemptException;
import edu.kit.dama.commons.types.DigitalObjectId;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.pid.db.PidToDigitalObject;
import edu.kit.masi.plugin.pid.IPidPlugin;
import edu.kit.masi.plugin.pid.impl.PidPluginFactory;
import edu.kit.masi.rest.security.impl.SecurityHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller managing all pid services including authorization. PID is always a
 * complete URL. e.g.: https:/hdl.handle.net/11022/1234-5678-...
 *
 * @author hartmann-v
 */
public class PidController {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PidController.class);

  /**
   * Default constructor.
   */
  public PidController() {
  }

  // <editor-fold defaultstate="collapsed" desc="PID services">
  /**
   * List all available PID services. Minimum role is GUEST!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @return List with all available services.
   * @throws MetaStoreException An error occurred.
   */
  public List<String> listAllServices(HttpContext pContext, String pGroupId) throws MetaStoreException {

    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);
    List<String> serviceNames = PidPluginFactory.getPluginNames();
    if (serviceNames.isEmpty()) {
      throw new MetaStoreException("No services available yet. Please add at least one service!", StatusCode.NOT_FOUND.getStatusCode());
    }

    return serviceNames;
  }

  /**
   * Create a PID. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pService Name of the service which should be used.
   * @param pDigitalObjectId ID of the digital object.
   * @return new PID.
   * @throws MetaStoreException An error occurred.
   */
  public String createPid(HttpContext pContext, String pGroupId, String pService, String pDigitalObjectId) throws MetaStoreException {
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MANAGER);
    existDigitalObjectId(pDigitalObjectId);
    IPidPlugin pidPlugin = PidPluginFactory.getPidPlugin(pService);
    if (pidPlugin == null) {
      throw new MetaStoreException("Service not found!", Response.Status.BAD_REQUEST.getStatusCode());
    }
    String createUniqueIdentifier = pidPlugin.createUniqueIdentifier(pDigitalObjectId);
    LOGGER.debug("New PID: '{}'", createUniqueIdentifier);
    return createUniqueIdentifier;
  }

  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Manage attributes of PID.">
  /**
   * Get all attributes of PID. Minimum role is GUEST!
   *
   * @param pPid The PID.
   * @return Map with all attributes and its values.
   * @throws MetaStoreException An error occurred.
   */
  public Map<String, List<String>> getAttributesOfPid(String pPid) throws MetaStoreException {
    LOGGER.debug("getAttributesOfPid '{}'", pPid);
    Map<String, List<String>> returnValue = new HashMap<>();
    IPidPlugin pidPlugin = PidPluginFactory.getPidPluginByPid(pPid);
    if (pidPlugin == null) {
      throw new MetaStoreException("Plugin not found!", Response.Status.BAD_REQUEST.getStatusCode());
    }
    try {
      List<String> attributeKeys = pidPlugin.getAttributeKeys(pPid);
      for (String attribute : attributeKeys) {
        List<String> attributeValues = pidPlugin.getAttributeValues(pPid, attribute);
        returnValue.put(attribute, attributeValues);
      }
    } catch (Exception ex) {
      LOGGER.error("Error getting attributes of PID '{}'", pPid);
      throw new MetaStoreException("Error getting attribute of PID", ex);
    }
    return returnValue;
  }

  /**
   * Get attribute of PID. One attribute may have multiple values.
   *
   * @param pPid The PID.
   * @param pAttributeLabel attribute which should be read.
   * @return List holding all values of this attribute.
   * @throws MetaStoreException An error occurred.
   */
  public List<String> getAttributeOfPid(String pPid, String pAttributeLabel) throws MetaStoreException {
    LOGGER.debug("getAttributesOfPid '{}'", pPid);
    List<String> returnValue;
    IPidPlugin pidPlugin = PidPluginFactory.getPidPluginByPid(pPid);
    if (pidPlugin == null) {
      throw new MetaStoreException("Plugin not found!", Response.Status.BAD_REQUEST.getStatusCode());
    }
    try {
      returnValue = pidPlugin.getAttributeValues(pPid, pAttributeLabel);
    } catch (Exception ex) {
      LOGGER.error("Error getting attributes of PID '{}'", pPid);
      throw new MetaStoreException("Error getting attribute of PID", ex);
    }
    return returnValue;
  }

  /**
   * Add attribute to PID. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pPid The PID.
   * @param pAttribute The label of the attribute.
   * @param pNewValue The value of the attribute.
   * @return SUCCESS or FAILURE
   * @throws MetaStoreException An error occurred.
   */
  public String addAttributeToPid(HttpContext pContext, String pGroupId, String pPid, String pAttribute, String pNewValue) throws MetaStoreException {
    LOGGER.debug("addAttributeToPid to '{}': {} = {}", pPid, pAttribute, pNewValue);
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MANAGER);
    IPidPlugin pidPlugin = PidPluginFactory.getPidPluginByPid(pPid);
    if (pidPlugin == null) {
      throw new MetaStoreException("Plugin not found!", Response.Status.BAD_REQUEST.getStatusCode());
    }
    try {
      pidPlugin.addAttribute(pPid, pAttribute, pNewValue);
    } catch (Exception ex) {
      LOGGER.error("Error adding attribute '{}' to PID '{}'", pAttribute, pPid);
      throw new MetaStoreException("Error adding attribute to PID", ex);
    }
    return "SUCCESS";
  }

  /**
   * Update attribute of PID. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pPid The PID.
   * @param pAttribute The label of the attribute.
   * @param pOldValue The value of the attribute.
   * @param pNewValue The value of the attribute.
   * @return SUCCESS or FAILURE
   * @throws MetaStoreException An error occurred.
   */
  public String updateAttributeToPid(HttpContext pContext, String pGroupId, String pPid, String pAttribute, String pOldValue, String pNewValue) throws MetaStoreException {
    LOGGER.debug("updateAttributeToPid to '{}': {} = {} -> {}", pPid, pAttribute, pOldValue, pNewValue);
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MANAGER);
    try {
      IPidPlugin pidPlugin = PidPluginFactory.getPidPluginByPid(pPid);
      if (pidPlugin == null) {
        throw new MetaStoreException("Plugin not found!", Response.Status.BAD_REQUEST.getStatusCode());
      }
      pidPlugin.updateAttribute(pPid, pAttribute, pOldValue, pNewValue);
    } catch (Exception ex) {
      LOGGER.error("Error adding attribute '{}' to PID '{}'", pAttribute, pPid);
      throw new MetaStoreException("Error updating attribute of PID", ex);
    }
    return "SUCCESS";
  }
  // </editor-fold>

  /**
   * Link digital object to PID. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pPid The PID.
   * @param pDigitalObjectId The ID of the digital object.
   * @return SUCCESS or FAILURE
   * @throws MetaStoreException An error occurred.
   */
  public String linkDigitalObjectToPid(HttpContext pContext, String pGroupId, String pPid, String pDigitalObjectId) throws MetaStoreException {
    LOGGER.debug("linkDigitalObjectToPid: {} -> {}", pPid, pDigitalObjectId);
    IAuthorizationContext checkForAuthorization = SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MANAGER);
    IMetaDataManager metaDataManager = null;
    try {
      // Check if PID exists 
      // Not possible due to missing service.
      // Maybe PID is generated outside.
      // Check if DigitalObjectId exists
      metaDataManager = getMetaDataManager(checkForAuthorization);
      existDigitalObjectId(metaDataManager, pDigitalObjectId);
      // Make DigitalObjectId public available
      // not neccessary at all
      // Set Digital Object to READ ONLY
      // Create and store link PID <-> DigitalObjectId
      LOGGER.trace("Store link between PID '{}' and digital object '{}'", pPid, pDigitalObjectId);
      PidToDigitalObject ptdo = new PidToDigitalObject(pPid, pDigitalObjectId);
      metaDataManager.save(ptdo);
    } catch (Exception ex) {
      LOGGER.error("Error linking digital object '{}' to PID '{}'", pDigitalObjectId, pPid);
      throw new MetaStoreException("Error linking digital object to PID", ex);
    } finally {
      if (metaDataManager != null) {
        metaDataManager.close();
      }
    }
    return "SUCCESS";
  }

  /**
   * Get DigitalObjectID for PID
   *
   * @param pPid PID
   * @return Digital object ID 
   * @throws MetaStoreException If an error occurred.
   */
  public String getDigitalObjectId4pid(String pPid) throws MetaStoreException {
    String returnValue = "No digital object found!";

    LOGGER.debug("getDigitalObjectId4pid: {} ", pPid);

    IMetaDataManager metaDataManager = getMetaDataManager(AuthorizationContext.factorySystemContext());
    PidToDigitalObject ptdo = new PidToDigitalObject(pPid);
    PidToDigitalObject find;
    try {
      find = metaDataManager.find(PidToDigitalObject.class, pPid);
    } catch (UnauthorizedAccessAttemptException ex) {
      // This should never happen as the SystemContext is used.
      LOGGER.error("Access not allowed!?", ex);
      throw new MetaStoreException("Error accessing database.");
    }
    if (find == null) {
      LOGGER.info("No digital object ID for given PID '{}'", pPid);
    } else {
      returnValue = find.getDigitalObjectIdentifier();
    }
    return returnValue;
  }

  /**
   * Check if digitalobjectid exists.
   *
   * @param pDigitalObjectId ID of digital object
   * @return true if digital object ID exists.
   * @throws MetaStoreException An error occurred or digital object ID doesn't
   * exist.
   */
  public boolean existDigitalObjectId(String pDigitalObjectId) throws MetaStoreException {
    IMetaDataManager metaDataManager = getMetaDataManager(AuthorizationContext.factorySystemContext());
    existDigitalObjectId(metaDataManager, pDigitalObjectId);
    return true;
  }

  /**
   * Check if digitalobjectid exists and user is authorized to access.
   *
   * @param pMetaDataManager Instance for accessing database.
   * @param pDigitalObjectId ID of digital object
   * @return true if digital object ID exists.
   * @throws MetaStoreException An error occurred.
   */
  private boolean existDigitalObjectId(IMetaDataManager pMetaDataManager, String pDigitalObjectId) throws MetaStoreException {
    try {
      DigitalObject dobj = new DigitalObject();
      dobj.setDigitalObjectId(new DigitalObjectId(pDigitalObjectId));
      List<DigitalObject> find = pMetaDataManager.find(dobj, dobj);
      LOGGER.trace("Find {} digital objects with id {}", find.size(), pDigitalObjectId);
      if (find.size() != 1) {
        throw new MetaStoreException("DigitalObjectId is not defined!", StatusCode.BAD_REQUEST.getStatusCode());
      }
    } catch (UnauthorizedAccessAttemptException ex) {
      LOGGER.error(null, ex);
      throw new MetaStoreException(ex, Response.Status.UNAUTHORIZED.getStatusCode());
    }
    return true;
  }

  /**
   * Create metadata manager for accessing database.
   *
   * @param pac Authorization context.
   * @return instance of metadata manager.
   */
  private IMetaDataManager getMetaDataManager(IAuthorizationContext pac) {
    MetaDataManagement mdm = MetaDataManagement.getMetaDataManagement();
    IMetaDataManager metaDataManager = mdm.getMetaDataManager();
    metaDataManager.setAuthorizationContext(pac);

    return metaDataManager;
  }
}
