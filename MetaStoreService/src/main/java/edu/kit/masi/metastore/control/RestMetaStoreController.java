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
package edu.kit.masi.metastore.control;

import com.sun.jersey.api.core.HttpContext;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.entities.Role;
import edu.kit.masi.metastore.db.ArangoDB;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.metastore.model.ReturnType;
import edu.kit.masi.metastore.utils.KitDataManagerUtil;
import edu.kit.masi.metastore.utils.MetsUtility;
import edu.kit.masi.metastore.utils.XsdUtil;
import edu.kit.masi.plugin.search.ISearchPlugin;
import edu.kit.masi.plugin.search.impl.SearchPluginFactory;
import edu.kit.masi.rest.security.impl.SecurityHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller managing all services including authorization.
 *
 * @author hartmann-v
 */
public class RestMetaStoreController {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RestMetaStoreController.class);

  /**
   * Connection to database.
   */
  private final ArangoDB arango = MetaStoreController.getArangoDb();
  /**
   * Utilities handling METS file.
   */
  private final MetsUtility metsUtility = new MetsUtility(arango);
  /**
   * Controller for the metastore.
   */
  private final MetaStoreController metaStoreController = new MetaStoreController();

  /**
   * Default constructor.
   */
  public RestMetaStoreController() {
  }

  // <editor-fold defaultstate="collapsed" desc="XSD services">
  /**
   * Register XSD file to meta store. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pPrefix Prefix of the namespace (has to be unique)
   * @param pXsdDocument Content of XSD file.
   * @return Target namespace of the registered XSD.
   * @throws MetaStoreException An error occurred.
   */
  public String registerXsdDocument(HttpContext pContext, String pGroupId, String pPrefix, String pXsdDocument) throws MetaStoreException {

    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MANAGER);

    boolean prefixExists = KitDataManagerUtil.getAllPrefixes().contains(pPrefix);
    boolean registerNewSchema = true;
    String targetNamespace = null;
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

    String returnValue = metaStoreController.registerXsdDocument(pXsdDocument, newPrefix);

    if (registerNewSchema) {
      LOGGER.debug("Register prefix '{}' with namespace '{}' at KIT Data Manager.", newPrefix, targetNamespace);
      KitDataManagerUtil.registerNewSchema(newPrefix, targetNamespace);
    }

    return returnValue;
  }

  /**
   * Get XSD file from meta store.
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pPrefix Prefix of the namespace (has to be unique)
   * @return Registered XSD.
   * @throws MetaStoreException An error occurred.
   */
  public String getXsdDocument(HttpContext pContext, String pGroupId, String pPrefix) throws MetaStoreException {
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);

    // KIT DataManager: get Namespace for prefix
    String namespace = KitDataManagerUtil.getNamespaceFromPrefix(pPrefix);
    LOGGER.debug("Try to fetch XSD for namespace '{}'", namespace);
    return metaStoreController.getXsdAsString(namespace);
  }

  /**
   * List all prefixes of all registered XSD files at meta store.
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @return JSON array of prefixes of all registered XSD files.
   * @throws MetaStoreException An error occurred.
   */
  public String listAllPrefixes(HttpContext pContext, String pGroupId) throws MetaStoreException {
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);

    // KIT DataManager: get Namespace for prefix
    List<String> allPrefixes = KitDataManagerUtil.getAllPrefixes();
    JSONArray array = new JSONArray();
    array = array.put(allPrefixes);

    return array.toString();
  }
  // </editor-fold>

  /**
   * Get METS file from meta store. Minimum role is GUEST!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pDigitalObjectId The id of the METS document.
   * @param returnType JSON or XML
   * @return Mets document as JSON/XML
   * @throws MetaStoreException An error occurred.
   */
  public String getMetsDocument(HttpContext pContext, String pGroupId, String pDigitalObjectId, MediaType returnType) throws MetaStoreException {
    //TODO:
    IAuthorizationContext checkForAuthorization = SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);
    List<String> objectIds = new ArrayList<>();
    objectIds.add(pDigitalObjectId);
    SecurityHelper.filter(checkForAuthorization, Role.GUEST, objectIds);
    if (objectIds.isEmpty()) {
      throw new MetaStoreException("Unauthorized access!", Response.Status.UNAUTHORIZED.getStatusCode());
    }

    return metaStoreController.getMetsDocument(pDigitalObjectId, getReturnType(returnType));
  }

  /**
   * Store METS file in meta store. Minimum role is MEMBER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pDigitalObjectId The id of the METS document.
   * @param pMetsDocument Content of METS file.
   * @return Message that document was successfully stored.
   * @throws MetaStoreException An error occurred.
   */
  public String storeMetsDocument(HttpContext pContext, String pGroupId, String pDigitalObjectId, String pMetsDocument) throws MetaStoreException {
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MEMBER);

    return metaStoreController.storeMetsDocument(pMetsDocument, pDigitalObjectId);
  }

  /**
   * Update existing METS file in meta store. Minimum role is MANAGER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pDigitalObjectId The id of the METS document.
   * @param pMetsDocument Content of METS file.
   * @return Message for successful update.
   * @throws MetaStoreException An error occurred.
   */
  public String updateMetsDocument(HttpContext pContext, String pGroupId, String pDigitalObjectId, String pMetsDocument) throws MetaStoreException {
//    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MEMBER);
    // validate METS document
    // validate sections
    // store and index METS document
    // store and index sections.
    throw new MetaStoreException("Update METS not implemented yet!", Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
//    return "METS document updated successfully.";
  }

  /**
   * Get existing section of METS file in meta store. Minimum role is GUEST!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pDigitalObjectId The id of the METS document.
   * @param pPrefix Type (prefix of namespace) of the section.
   * @param pSectionId id of the section (may be null if namespace is unique)
   * @param returnType JSON or XML
   * @return Selected section of METS document.
   * @throws MetaStoreException An error occurred.
   */
  public String getPartialMetsDocument(HttpContext pContext, String pGroupId, String pDigitalObjectId, String pPrefix, String pSectionId, MediaType returnType) throws MetaStoreException {
    IAuthorizationContext checkForAuthorization = SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);

    List<String> objectIds = new ArrayList<>();
    objectIds.add(pDigitalObjectId);
    SecurityHelper.filter(checkForAuthorization, Role.GUEST, objectIds);
    if (objectIds.isEmpty()) {
      throw new MetaStoreException("Unauthorized access!", Response.Status.UNAUTHORIZED.getStatusCode());
    }
    // KIT DataManager: get Namespace for prefix
    String namespace = KitDataManagerUtil.getNamespaceFromPrefix(pPrefix);
    String partialMetsDocument = metaStoreController.getPartialMetsDocument(namespace, pDigitalObjectId, getReturnType(returnType));
    return partialMetsDocument;
  }

  /**
   * Update existing section of METS file in meta store. Minimum role is MEMBER!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pDigitalObjectId The id of the METS document.
   * @param pSectionId id of the section (may be null if namespace is unique)
   * @param pSectionDocument Content of METS file.
   * @return Message that section was updated successfully.
   * @throws MetaStoreException An error occurred.
   */
  public String updatePartialMetsDocument(HttpContext pContext, String pGroupId, String pDigitalObjectId, String pSectionId, String pSectionDocument) throws MetaStoreException {
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.MEMBER);
    if (pSectionId == null) {
      pSectionId = "";
    }
    String updatePartialMetsDocument = metaStoreController.updatePartialMetsDocument(pSectionDocument, pDigitalObjectId, pSectionId);
    return updatePartialMetsDocument;
  }

  /**
   * Validate XML against registered XSD. Minimum role is GUEST!
   *
   * @param pContext HTTP context of call.
   * @param pGroupId The group id the ingest belongs to
   * @param pXmlDocument Content of METS file.
   * @return true if valid.
   * @throws MetaStoreException An error occurred.
   */
  public boolean validateDocument(HttpContext pContext, String pGroupId, String pXmlDocument) throws MetaStoreException {
    boolean isValid = false;
    SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);
    try {
      isValid = metaStoreController.validateDocument(pXmlDocument);
      // </editor-fold>
    } catch (MetaStoreException mse) {
      throw mse;
    } catch (Exception ex) {
      LOGGER.error("Error parsing xml!", ex);
      throw new MetaStoreException("XML is not valid! - " + ex.getMessage(), ex);
    }

    return isValid;
  }

  // <editor-fold defaultstate="collapsed" desc="Search service">
  /**
   * Search for all mets documents matching the given term.
   *
   * @param pContext Context of user.
   * @param pGroupId GoupId of user,
   * @param pIndexes All provided indexes. (Index identical to group)
   * @param pPrefixes All provided prefixes.
   * @param pSearchTerms search terms.
   * @param maxNoOfHits maximum number of hits.
   * @param pShort Return only Digital object IDs instead of document.
   * @return All fitting mets documents as JSON array.
   * @throws MetaStoreException An error occurred.
   */
  public String searchForMetsDocuments(HttpContext pContext, String pGroupId, List<String> pIndexes, List<String> pPrefixes, List<String> pSearchTerms, int maxNoOfHits, boolean pShort) throws MetaStoreException {
    IAuthorizationContext authorizationContext = SecurityHelper.checkForAuthorization(pContext, pGroupId, Role.GUEST);

    StringBuilder returnValue = new StringBuilder();
    ReturnType returnType = ReturnType.JSON;  // may be adaptable later.
    ISearchPlugin searchPlugin = SearchPluginFactory.getSearchPlugin();
    Set<String> allDigitalObjectIds = null;
    if (searchPlugin == null) {
      allDigitalObjectIds = metsUtility.searchFullTextWithId(pSearchTerms.get(0));
    } else {
      String[] allTerms = pSearchTerms.toArray(new String[pSearchTerms.size()]);
      String[] types = pPrefixes.toArray(new String[pPrefixes.size()]);
      String[] searchResults = searchPlugin.searchForMets(ISearchPlugin.Combination.CONJUNCTION, types, allTerms);
      allDigitalObjectIds = new HashSet<>(Arrays.asList(searchResults));
    }
    LOGGER.debug("Found {} hits", allDigitalObjectIds.size());
    if (allDigitalObjectIds.size() > 0) {
      // filter Digital Object IDs
      SecurityHelper.filter(authorizationContext, Role.GUEST, allDigitalObjectIds);
      LOGGER.debug("Found {} hits after filtering!", allDigitalObjectIds.size());
      if (allDigitalObjectIds.size() > 0) {
        // filter Digital Object IDs
        int noOfDocuments = 1;
        if (returnType.equals(ReturnType.JSON)) {
          JSONArray array = new JSONArray();
          for (String digitalObjectIds : allDigitalObjectIds) {
            // JSONObject jsonObject =
            // arango.getJsonObject(MetaStoreUtility.getHashValue(digitalObjectIds));
            // returnValue.put(jsonObject);
            JSONObject item;
            if (pShort) {
              item = new JSONObject();
              item.put("digitalObjectId", digitalObjectIds);
            } else {
              item = new JSONObject(metsUtility.getMetsDocument(digitalObjectIds, returnType));
            }
            LOGGER.debug("Add document #{} of {}!", noOfDocuments, maxNoOfHits);
            array.put(item);
            noOfDocuments++;
            if (noOfDocuments > maxNoOfHits) {
              break;
            }
          }
          return array.toString();
        } else {
          returnValue.append("<array>\n");
          for (String digitalObjectIds : allDigitalObjectIds) {
            if (pShort) {
             returnValue.append(String.format("<digitalObjectId>%s</digitalObjectId>", digitalObjectIds));
            } else {
            returnValue.append(
                    metaStoreController.getMetsDocument(digitalObjectIds, returnType)
                            .replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "") + "\n");
            }
            noOfDocuments++;
            if (noOfDocuments > maxNoOfHits) {
              break;
            }
          }
          returnValue.append("</array>");
        }
      }
    }
    return returnValue.toString();
  }
  // </editor-fold>

  /**
   * Transform return type to model specific return type.
   *
   * @param pReturnType Return type provided by service.
   * @return return type.
   */
  private ReturnType getReturnType(MediaType pReturnType) {
    ReturnType returnType = ReturnType.JSON;
    if (pReturnType == MediaType.APPLICATION_JSON_TYPE) {
      returnType = ReturnType.JSON;
    }
    if (pReturnType == MediaType.APPLICATION_XML_TYPE) {
      returnType = ReturnType.XML;
    }
    return returnType;
  }
}
