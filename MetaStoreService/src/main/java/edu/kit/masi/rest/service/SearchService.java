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
package edu.kit.masi.rest.service;

import com.sun.jersey.api.core.HttpContext;
import edu.kit.dama.util.Constants;
import edu.kit.masi.metastore.control.RestMetaStoreController;
import edu.kit.masi.metastore.exception.MetaStoreException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing search on metadata stored inside meta store.
 *
 * @author hartmann-v
 */
@Path("/xml/search")
@Api(value = "Metastore REST API", description = "Endpoint for User specific search operations", tags = "Search")
public class SearchService extends BaseService {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

//  SecurityContext securityContext;
  @Context
  HttpContext context;

  /**
   * Search for term(s) in all mets documents. Result will be filtered by
   * authorization of KIT Data Manager.
   *
   * @param pSearchTerms Search term(s) (Only one search term allowed yet)
   * @param pIndexes Group(s) used for searching. (Not supported yet)
   * @param pTypes Types (prefix of namespaces) used for searching. (Not
   * supported yet)
   * @param pGroupId The group id the search belongs to [default: WORLD]
   * @param pMaxNoOfHits Maximum number of hits.
   * @param pShortList Show only Digital Object Identifiers.
   * @return Response holding status and array of found METS documents.
   */
  @GET
  @Produces("application/json")
  @ApiOperation(value = "Full text search on metadata.", notes = "Returns the complete documents.", tags = "Search")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successfully retrieval of search results")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "No search engine found")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response getSearchResult(
          @ApiParam(value = "For non public search provide group connected to user.", required = false, defaultValue = "WORLD") @QueryParam("groupId") String pGroupId,
          @ApiParam(value = "Search term(s)", allowMultiple = true, required = true) @QueryParam("term") List<String> pSearchTerms,
          @ApiParam(value = "Which indices should be searched (linked with groupIds). (Not supported yet!)", allowMultiple = true, required = false, defaultValue = "_all") @QueryParam("index") List<String> pIndexes,
          @ApiParam(value = "Which types should be searched (linked with metadata schema). (Not supported yet!)", allowMultiple = true, required = false, defaultValue = "_all") @QueryParam("type") List<String> pTypes,
          @ApiParam(value = "Maximum number of hits.", required = false, defaultValue = "20") @QueryParam("size") int pMaxNoOfHits,
          @ApiParam(value = "Short - Show only Digital Object IDs.", required = false, defaultValue = "false") @QueryParam("short") boolean pShort) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("getSearchResult: groupID = {} & searchTerms = {} & indexes = {} & types = {} & maxNoOfHits = {} & short = {}", pGroupId, pSearchTerms, pIndexes, pTypes, pMaxNoOfHits, pShort);
    }

    Response.Status statusCode = Response.Status.OK;
    String jsonString;

    if (pGroupId == null) {
      pGroupId = Constants.WORLD_GROUP_ID; // Set to default value.
    }
    if (pMaxNoOfHits <= 0) {
      pMaxNoOfHits = 20; // Set to default value.
    }
    try {
      RestMetaStoreController msc = new RestMetaStoreController();
      jsonString = msc.searchForMetsDocuments(context, pGroupId, pIndexes, pTypes, pSearchTerms, pMaxNoOfHits, pShort);

    } catch (MetaStoreException ex) {
      LOGGER.error("Error while searching.", ex);
      jsonString = exceptionToJson(ex);
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while searching.", ex);
      jsonString = exceptionToJson(ex);
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }

    return Response.status(statusCode).entity(jsonString).build();
  }
}
