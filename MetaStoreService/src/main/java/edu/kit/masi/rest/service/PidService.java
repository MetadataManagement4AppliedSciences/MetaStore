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
import edu.kit.dama.authorization.entities.Role;
import edu.kit.dama.util.DataManagerSettings;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.exception.StatusCode;
import edu.kit.masi.pid.PidController;
import edu.kit.masi.rest.security.impl.SecurityHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XSD registration to meta store.
 *
 * @author hartmann-v
 */
@Path("/pid")
@Api(value = "PID REST API", description = "Endpoint for User specific operations regarding PID", tags = "PID")
public class PidService extends BaseService {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PidService.class);

  /**
   * Context used for authentication and authorization.
   */
  @Context
  HttpContext context;

  /**
   * Create PID. Create a PID for given service.
   *
   * @param pGroupId The group id the PID belongs to [default: USERS]
   * @param pDigitalObjectId The digital object ID which should be available via
   * PID.
   * @param pService Name of the service.
   * @return JSONObject holding PID or error message.
   */
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Create a PID with given service.", notes = "Returns the PID.")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully created PID")
    ,
    @ApiResponse(code = 400, message = "Bad request - service not allowed.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response createPid(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Digital Object ID", name = "digitalObjectId", required = true) @FormParam(value = "digitalObjectId") String pDigitalObjectId,
          @ApiParam(value = "Service which should be used to create PID", name = "service", required = true) @FormParam(value = "service") String pService) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("createPid: groupID = {} & service = {} & digital object ID", pGroupId, pService);
    }

    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();
    try {
      if (pService == null) {
        throw new MetaStoreException("Please provide a service!", StatusCode.BAD_REQUEST.getStatusCode());
      }
      PidController pc = new PidController();
      String pid = pc.createPid(context, pGroupId, pService, pDigitalObjectId);
      String message = pc.linkDigitalObjectToPid(context, pGroupId, pid, pDigitalObjectId);
      JSONObject jo = new JSONObject();
      jo.put("PID", pid);
      sb.append(jo.toString());
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while creating PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while creating PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get a list of all available PID services.
   *
   * @param pGroupId The group id the registration belongs to [default: USERS]
   * @return JSON array containing all available PID services.
   */
  @GET
  @Path("/services")
  @Produces("application/json")
  @ApiOperation(value = "Get a list of all available PID services", notes = "Returns only the service names. Different service names may"
          + "be used for different setups of same service.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Success")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response listAllRegisteredPidServices(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least guest)", required = false, defaultValue = "USERS") @QueryParam("groupId") String pGroupId) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("listAllRegisteredPidServices: groupID = {} ", pGroupId);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    try {
      List<String> listAllPrefixes = new PidController().listAllServices(context, pGroupId);
      JSONArray array = new JSONArray();
      array = array.put(listAllPrefixes);

      sb.append(array.toString());
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(sb.toString());
      }
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while listing all PID services.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while listing all PID services.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get all attributes of an existing PID.
   *
   * @param pPid PID.
   * @return JSONObject holding all attributes.
   */
  @GET
  @Produces({"application/json", "text/html"})
  @ApiOperation(value = "Get all attributes of PID or landing page depending on format of return value.", notes = "In case of attributes: JSonArray with key:value")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Success")
    ,
    @ApiResponse(code = 400, message = "Bad request.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response getAllAttributes(
          @ApiParam(value = "PID (URL encoded)", name = "pid", required = true) @QueryParam(value = "pid") String pPid) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("getAllAttributes: pid = {}", pPid);
    }
    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();
    MediaType acceptableMediaType = getAcceptableMediaType(context, MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_HTML_TYPE);
    if (acceptableMediaType == MediaType.TEXT_HTML_TYPE) {
      sb = showLandingPage(pPid);
    } else {
      JSONArray jArray = new JSONArray();
      try {
        PidController pc = new PidController();
        Map<String, List<String>> attributesOfPid = pc.getAttributesOfPid(pPid);
        for (String key : attributesOfPid.keySet()) {
          for (String value : attributesOfPid.get(key)) {
            JSONObject jo = new JSONObject();
            jo.put(key, value);
            jArray.put(jo);
          }
        }
        sb.append(jArray.toString());
      } catch (MetaStoreException ex) {
        LOGGER.error("Error while accessing all attributes of PID.", ex);
        sb.append(exceptionToJson(ex));
        statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
      } catch (Exception ex) {
        LOGGER.error("Uncatched error while accessing all attributes of PID.", ex);
        sb.append(exceptionToJson(ex));
        statusCode = Response.Status.INTERNAL_SERVER_ERROR;
      }
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get attribute of an existing PID.
   *
   * @param pPid PID.
   * @param pAttributeLabel label of the attribute.
   * @return JSONObject holding attribute.
   */
  @GET
  @Path("/attribute/{attributeLabel}")
  @Produces("application/json")
  @ApiOperation(value = "Get attribute with given label of given PID.", notes = " attribute with"
          + "same label may exist.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successfully added attribute")
    ,
    @ApiResponse(code = 400, message = "Bad request - The chosen service will not support this feature.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response getAttribute(
          @ApiParam(value = "PID (URL encoded)", name = "pid", required = true) @QueryParam(value = "pid") String pPid,
          @ApiParam(value = "Label of the attribute", name = "attributeLabel", required = true) @PathParam(value = "attributeLabel") String pAttributeLabel) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("getAttribute: pid = {}, key = {}", pPid, pAttributeLabel);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();
    JSONArray jArray = new JSONArray();
    try {
      PidController pc = new PidController();
      List<String> attributeOfPid = pc.getAttributeOfPid(pPid, pAttributeLabel);
      for (String value : attributeOfPid) {
        JSONObject jo = new JSONObject();
        jo.put(pAttributeLabel, value);
        jArray.put(jo);
      }

      sb.append(jArray.toString());
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while accessing attribute of PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while accessing attribute of PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Add a new attribute to an existing PID.
   *
   * @param pGroupId The group id the PID belongs to [default: USERS]
   * @param pPid PID.
   * @param pAttributeLabel label of the attribute.
   * @param pValue value of the attribute.
   * @return JSONObject holding status message.
   */
  @POST
  @Path("/attribute/{attributeLabel}")
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Add an attribute to given PID.", notes = "If attribute already exists a new attribute with"
          + "same label will be added.")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully added attribute")
    ,
    @ApiResponse(code = 400, message = "Bad request - The chosen service will not support this feature.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response addAttributeToPid(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "PID", name = "pid", required = true) @FormParam(value = "pid") String pPid,
          @ApiParam(value = "Label of the attribute", name = "attributeLabel", required = true) @PathParam(value = "attributeLabel") String pAttributeLabel,
          @ApiParam(value = "Value of the attribute", name = "value", required = true) @FormParam(value = "value") String pValue) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("addAttributeToPid: groupID = {}, pid = {}, key = {}, value = {}", pGroupId, pPid, pAttributeLabel, pValue);
    }

    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();
    try {
      SecurityHelper.checkForAuthorization(context, pGroupId, Role.MANAGER);
      PidController pc = new PidController();
      String message = pc.addAttributeToPid(context, pGroupId, pPid, pAttributeLabel, pValue);
      JSONObject jo = new JSONObject();
      jo.put("message", message);
      sb.append(jo.toString());
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while adding attribute to PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while adding attribute to PID.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Update an existing attribute of an existing PID.
   *
   * @param pGroupId The group id the PID belongs to [default: USERS]
   * @param pPid PID.
   * @param pAttributeLabel label of the attribute.
   * @param pOldValue old value of the attribute.
   * @param pNewValue value of the attribute.
   * @return JSONObject holding status message.
   */
  @PUT
  @Path("/attribute/{attributeLabel}")
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Update existing attribute with a new value to given PID.", notes = " attribute with"
          + "same label will be added.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successfully added attribute")
    ,
    @ApiResponse(code = 400, message = "Bad request - The chosen service will not support this feature.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")})
  public Response updateAttributeOfPid(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "PID (URL encoded)", name = "pid", required = true) @FormParam(value = "pid") String pPid,
          @ApiParam(value = "Label of the attribute", name = "attributeLabel", required = true) @PathParam(value = "attributeLabel") String pAttributeLabel,
          @ApiParam(value = "Old value of the attribute", name = "oldValue", required = true) @FormParam(value = "oldValue") String pOldValue,
          @ApiParam(value = "New Value of the attribute", name = "newValue", required = true) @FormParam(value = "newValue") String pNewValue) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("updateAttributeOfPid: groupID = {}, pid = {}, key = {}, value = {}", pGroupId, pPid, pAttributeLabel, pNewValue);
    }
    Response.Status statucCodeNotImplemmented = Response.Status.SERVICE_UNAVAILABLE;
    return Response.status(statucCodeNotImplemmented).entity("Method not implemented yet!").build();
//    Response.Status statusCode = Response.Status.OK;
//    StringBuilder sb = new StringBuilder();
//    try {
//      SecurityHelper.checkForAuthorization(context, pGroupId, Role.MANAGER);
//      PidController pc = new PidController();
//      String message = pc.updateAttributeToPid(context, pGroupId, pPid, pAttributeLabel, pOldValue, pNewValue);
//      JSONObject jo = new JSONObject();
//      jo.put("message", message);
//      sb.append(jo.toString());
//    } catch (MetaStoreException ex) {
//      LOGGER.error("Error while updating attribute of PID.", ex);
//      sb.append(exceptionToJson(ex));
//      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
//    }
//    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get a list of all available PID services.
   *
   * @param pPid The group id the registration belongs to [default: USERS]
   * @return JSON array containing all available PID services.
   */
  public StringBuilder showLandingPage(String pPid) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("showLandingPage: PID = {} ", pPid);
    }

    Response.Status statusCode = Response.Status.OK;
    PidController pc = new PidController();

    String stringProperty = DataManagerSettings.getSingleton().getStringProperty("general.baseUrl", "unknown");
    StringBuilder sb = new StringBuilder("<html><head><meta http-equiv=\"refresh\" content=\"0; url=");
    try {
      stringProperty = stringProperty + "?landing&oid=";
      sb.append(stringProperty).append(pc.getDigitalObjectId4pid(pPid));
      sb.append("\" /></head></html>");
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while generating landing page.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while generating landing page.", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return sb;
  }

}
