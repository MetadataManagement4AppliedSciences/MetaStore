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
import edu.kit.masi.metastore.control.RestMetaStoreController;
import edu.kit.masi.metastore.exception.MetaStoreException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Services for validating XML.
 * @author hartmann-v
 */
@Path("/xml/validate")
@Api(value = "Metastore REST API", description = "Endpoint for User specific operations registration", tags = "Validation")
public class ValidationService extends BaseService {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationService.class);

  /**
   * Context used for authentication and authorization.
   */
  @Context
  HttpContext context;
  /**
   * Validate XML against registered XSD file.
   * @param pGroupId The group id the registration belongs to [default: USERS]
   * @param pFileContent Content of the XML file.
   * @return true if valid, error status otherwise.
   */
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Validate XML against registered XSD.", notes = "Also fails if linked namespace is not registered.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Valid XML")
    ,
    @ApiResponse(code = 400, message = "Invalid XML")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response validateXml(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Content of XML file to be stored", type = "File", name = "xmlFile", required = true) @FormParam(value = "xmlFile") String pFileContent) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("ValidateXML: groupID = {} & content = {}", pGroupId, pFileContent);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    try {
      boolean isValid = new RestMetaStoreController().validateDocument(context, pGroupId, pFileContent);
      if (!isValid) {
        throw new MetaStoreException("XML is not valid!", Response.Status.BAD_REQUEST.getStatusCode());
      }
      sb.append(messageToJson("Document is valid!"));
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while validating XML!", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while validating XML!", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }

    return Response.status(statusCode).entity(sb.toString()).build();
  }
}
