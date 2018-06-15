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
package edu.kit.masi.rest.security.impl;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.oauth.server.OAuthServerRequest;
import com.sun.jersey.oauth.signature.OAuthParameters;
import edu.kit.dama.authorization.entities.GroupId;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.entities.Role;
import edu.kit.dama.authorization.entities.SecurableResourceId;
import edu.kit.dama.authorization.services.base.PlainAuthorizerLocal;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.rest.util.RestUtils;
import edu.kit.masi.metastore.exception.MetaStoreException;
import java.util.ArrayList;
import java.util.Collection;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class containing methods useful for authorization.
 *
 * @author hartmann-v
 */
public class SecurityHelper {

  /**
   * String for identifying securable resources.
   */
  private static final String DOMAIN = DigitalObject.class.getCanonicalName();
  /**
   * Logger for logging output.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityHelper.class);

  /**
   * Method used to test if oAuth credentials are accessible. For productive use
   * use RestUtils.authorize(HttpContext hc, GroupId pGroupId) instead.
   *
   * @param hc context of REST call.
   * @return String containing key and token.
   */
  public static final String getContextFromHeader(HttpContext hc) {
    LOGGER.debug("Starting OAuth authentication.");
    OAuthServerRequest request = new OAuthServerRequest(hc.getRequest());
    // get incoming OAuth parameters
    OAuthParameters params = new OAuthParameters();
    params.readRequest(request);

    LOGGER.debug("Obtaining consumer credentials.");
    //obtain consumer key and secret
    String consumerKey = params.getConsumerKey();
    String token = params.getToken();

    return consumerKey + " --> " + token;
  }

  /**
   * Check for minimum privileges.
   *
   * @param pContext Context of user.
   * @param pGroupId GoupId of user,
   * @param pRoleRequired Minimum role.
   * @return Context of the user.
   * @throws MetaStoreException An error occurred.
   */
  public static IAuthorizationContext checkForAuthorization(HttpContext pContext, String pGroupId, Role pRoleRequired) throws MetaStoreException {
    IAuthorizationContext authorize = RestUtils.authorize(pContext, new GroupId(pGroupId));
    // Only managers should have access to registration.
    if (!authorize.getRoleRestriction().atLeast(pRoleRequired)) {
      throw new MetaStoreException("Authorization failed!", Response.Status.UNAUTHORIZED.getStatusCode());
    }
    return authorize;
  }

  /**
   * Filter out all resources not accessible by the context at least with
   * roleRequired privilege. In this call, the provided list of resource ids is
   * directly modified.
   *
   * @param pAuthContext Context of user who wants to read the resources.
   * @param pRoleRequired Minimum role required for the access.
   * @param pResourceIds Collection holding all ids. 'Forbidden' ids are
   * filtered out at the end.
   */
  public static void filter(IAuthorizationContext pAuthContext,
          Role pRoleRequired,
          Collection<String> pResourceIds) {
    Collection<SecurableResourceId> allResources = new ArrayList<>();
    Collection<String> filteredResoures = new ArrayList<>();
    for (String resourceId : pResourceIds) {
      allResources.add(new SecurableResourceId(DOMAIN, resourceId));
      // Split filter in smaller parts!
      if (allResources.size() >= 1000) {
        Collection<SecurableResourceId> result = new ArrayList<>();
        PlainAuthorizerLocal.filterOnAccessAllowed(pAuthContext, pRoleRequired, allResources, result);
        for (SecurableResourceId resourceIdFiltered : result) {
          filteredResoures.add(resourceIdFiltered.getDomainUniqueId());
        }
        allResources.clear();

      }
    }
    if (!allResources.isEmpty()) {
      Collection<SecurableResourceId> result = new ArrayList<>();
      PlainAuthorizerLocal.filterOnAccessAllowed(pAuthContext, pRoleRequired, allResources, result);
      for (SecurableResourceId resourceIdFiltered : result) {
        filteredResoures.add(resourceIdFiltered.getDomainUniqueId());
      }

    }
    pResourceIds.clear();
    for (String resourceIdString : filteredResoures) {
      pResourceIds.add(resourceIdString);
    }
  }
}
