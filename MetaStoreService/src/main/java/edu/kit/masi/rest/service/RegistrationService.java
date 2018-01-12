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
import edu.kit.masi.metastore.exception.StatusCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XSD registration to meta store.
 *
 * @author hartmann-v
 */
@Path("/xsd")
@Api(value = "Metastore REST API", description = "Endpoint for User specific operations registration", tags = "XML Schema Definition (XSD)")
public class RegistrationService extends BaseService {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

  /**
   * Context used for authentication and authorization.
   */
  @Context
  HttpContext context;

  /**
   * Register XSD file. All used schemas has to be registered in beforehand.
   * Otherwise the ingest of metadata will fail. The user has to be at least
   * manager.
   *
   * @param pGroupId The group id the registration belongs to [default: USERS]
   * @param pPrefix Unique prefix linked with the XSD file.
   * @param pFileContent Content of XSD file.
   * @return Response holding status and message.
   */
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Register XSD file", notes = "Returns the namespace of registered xsd file")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully registered schema")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "Can't register schema due to errors in schema.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response registerFile(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Unique prefix linked to the schema of XSD file.", name = "prefix", required = true) @FormParam(value = "prefix") String pPrefix,
          @ApiParam(value = "Content of XSD file to be registered.", type = "File", name = "file", required = true) @FormParam(value = "file") String pFileContent) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("registerFile: groupID = {} & prefix = {} & content = {}", pGroupId, pPrefix, pFileContent);
    }
    
    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();

    try {
    if (pFileContent ==  null) {
      throw new MetaStoreException("Please provide input of XSD file!", StatusCode.BAD_REQUEST.getStatusCode());
    }
  
      String registerXsdDocument = new RestMetaStoreController().registerXsdDocument(context, pGroupId, pPrefix, pFileContent);
      sb.append(messageToJson(registerXsdDocument));
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while registering xsd.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched Error while registering xsd.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get registered XSD file for given prefix.
   *
   * @param pGroupId The group id the registration belongs to [default: USERS]
   * @param pPrefix Unique prefix linked with the XSD file.
   * @return XSD file
   */
  @GET
  @Path("/{prefix}")
  @Produces("application/xml")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Register XSD file", notes = "Returns the xsd file")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Get registered schema")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "Prefix not registered.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response getXsdFile(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least guest)", required = false, defaultValue = "USERS") @QueryParam("groupId") String pGroupId,
          @ApiParam(value = "Unique prefix linked to the schema of XSD file.", name = "prefix", required = true) @PathParam(value = "prefix") String pPrefix) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("getXsdFile: groupID = {} & prefix {}", pGroupId, pPrefix);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    try {
      String xsdDocument = new RestMetaStoreController().getXsdDocument(context, pGroupId, pPrefix);
      sb.append(xsdDocument);
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while accessing xsd.", ex);
      sb.append(exceptionToXml(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while accessing registering xsd.", ex);
      sb.append(exceptionToXml(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get all prefixes of registered XSD files.
   *
   * @param pGroupId The group id the registration belongs to [default: USERS]
   * @return JSON array containing all registered prefixes.
   */
  @GET
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Get all prefixes of registered XSD files", notes = "Returns only the prefixes.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Success")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response listAllRegisteredPrefixes(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least guest)", required = false, defaultValue = "USERS") @QueryParam("groupId") String pGroupId) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("listAllRegisteredPrefixes: groupID = {} ", pGroupId);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    try {
      String listAllPrefixes = new RestMetaStoreController().listAllPrefixes(context, pGroupId);
      sb.append(listAllPrefixes);
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while listing all prefixes of registered XSDs.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while listing all prefixes of registered XSDs.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }
}
