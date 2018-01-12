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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All services for storing and reading xml from meta store.
 *
 * @author hartmann-v
 */
@Path("/xml")
@Api(value = "Metastore REST API", description = "Endpoint for User specific operations registration", tags = "Metadata")
public class MetadataService extends BaseService {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataService.class);

  /**
   * Context used for authentication and authorization.
   */
  @Context
  HttpContext context;

  /**
   * Store and index XML.
   *
   * @param pFileContent XML as a string.
   * @param pDigitalObjectId Unique ID of the document. In most cases this
   * should be identical to the digital data object id.
   * @param pGroupId The group id the ingest belongs to [default: USERS]
   * @return Response holding status and message.
   */
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Store, validate and index XML file to metastore. Only METS file are allowed.", notes = "METS XML will be splitted in its sections and stored as separate parts.")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully stored and indexed.")
    ,
    @ApiResponse(code = 400, message = "Bad request. At least one Parameter is invalid.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 409, message = "ID already exists. Use 'put' for update metadata.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response storeAndIndexMetsXml(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least member)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Unique ID for the metadata (should be identical to id of linked Digital Data Object).", required = true) @FormParam(value = "oid") String pDigitalObjectId,
          @ApiParam(value = "Content of XML file to be stored", type = "File", name = "xmlFile", required = true) @FormParam(value = "xmlFile") String pFileContent) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("storeMets: groupID = {} & digitalObjectId = {} & Metsfile = {}", pGroupId, pDigitalObjectId, pFileContent);
    }

    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();

    try {
      String storeMetsDocument = new RestMetaStoreController().storeMetsDocument(context, pGroupId, pDigitalObjectId, pFileContent);

      sb.append(messageToJson(storeMetsDocument));
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while storing mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while storing mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Update METS file.
   *
   * @param pGroupId The group id the ingest belongs to [default: USERS]
   * @param pDigitalObjectId Unique ID of the document. In most cases this
   * should be identical to the digital data object id.
   * @param pFileContent XML as a string.
   * @return Response holding status and message.
   */
  @PUT
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Update, validate and index XML file to metastore. Only METS file are allowed. (Not available yet!)", notes = "METS xml will be splitted in its sections and stored as separate parts.")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully updated and indexed.")
    ,
    @ApiResponse(code = 400, message = "Bad request. At least one Parameter is invalid.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 409, message = "ID already exists. Use 'put' for update metadata.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response updateAndIndexMetsXml(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least member)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Unique ID for the metadata (should be identical to id of linked Digital Data Object).", required = true) @FormParam(value = "oid") String pDigitalObjectId,
          @ApiParam(value = "Content of XML file to be stored", type = "File", name = "xmlFile", required = true) @FormParam(value = "xmlFile") String pFileContent) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("update METS: groupID = {} & digitalObjectId = {} & Metsfile = {}", pGroupId, pDigitalObjectId, pFileContent);
    }

    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();

    try {
      String storeMetsDocument = new RestMetaStoreController().updateMetsDocument(context, pGroupId, pDigitalObjectId, pFileContent);

      sb.append(messageToJson(storeMetsDocument));
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while updating mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while updating mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get METS file as XML
   *
   * @param pGroupId The group id the access belongs to [default: USERS]
   * @param pDigitalObjectId ID of the metadata (may be the id of the digital
   * object)
   * @return Response holding status and content of METS document.
   */
  @GET
  @Path("/")
  @Produces({"application/xml", "application/json"})
  @ApiOperation(value = "Get single METS file.", notes = "METS file will be compiled from most current sections."
          + " If there are multiple sections with the same type an error will occur.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful.")
    ,
    @ApiResponse(code = 400, message = "Bad request. At least one Parameter is invalid.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "ID doesn't exist.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response getMetsDocument(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @QueryParam("groupId") String pGroupId,
          @ApiParam(value = "Unique ID for the metadata (should be identical to id of linked Digital Data Object).", required = true) @QueryParam(value = "oid") String pDigitalObjectId) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("get METS file: groupID = {} & digitalObjectId = {} ", pGroupId, pDigitalObjectId);
    }

    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    MediaType returnType = getAcceptableMediaType(context, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE);
    try {
      String metsDocument = new RestMetaStoreController().getMetsDocument(context, pGroupId, pDigitalObjectId, returnType);

      sb.append(metsDocument);
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while accessing mets file", ex);
      exceptionToFormattedString(ex, returnType);
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while accessing mets file", ex);
      exceptionToFormattedString(ex, returnType);
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Get nested section as JSON/XML.
   *
   * @param pGroupId The group id the access belongs to [default: USERS]
   * @param pDigitalObjectId ID of the metadata (may be the id of the digital
   * object)
   * @param prefixOfNamespace Prefix of the namespace provided during
   * registration.
   * @param pSectionId ID of the section (optional parameter if multiple
   * sections with same namespace exists.
   * @return Response holding status and the content of the partial metadata.
   */
  @GET
  @Path("/{prefix}")
  @Produces({"application/xml", "application/json"})
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Get single section of METS file.", notes = "Section will be selected by prefix of the metadata registered in KIT Data Manager."
          + " If there are multiple sections with the same type an error will occur.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful.")
    ,
    @ApiResponse(code = 400, message = "Bad request. At least one Parameter is invalid.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "ID doesn't exist.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response getPartialXML(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @QueryParam("groupId") String pGroupId,
          @ApiParam(value = "Unique ID for the metadata (should be identical to id of linked Digital Data Object).", required = true) @QueryParam(value = "oid") String pDigitalObjectId,
          @ApiParam(value = "Prefix of namespace of selected section", required = true) @PathParam("prefix") String prefixOfNamespace,
          @ApiParam(value = "Section ID for the metadata (mandatory if multiple sections with same type exists).", required = false) @QueryParam(value = "sectionId") String pSectionId) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("get Section XML: groupID = {} & digitalObjectId = {} & prefix = {} & section ID = {}", pGroupId, pDigitalObjectId, prefixOfNamespace, pSectionId);
    }
 
    Response.Status statusCode = Response.Status.OK;
    StringBuilder sb = new StringBuilder();

    MediaType returnType = getAcceptableMediaType(context, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE);
    try {
      String metsDocument = new RestMetaStoreController().getPartialMetsDocument(context, pGroupId, pDigitalObjectId, prefixOfNamespace, pSectionId, returnType);

      sb.append(metsDocument);
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while accessing section of mets file", ex);
      exceptionToFormattedString(ex, returnType);
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while accessing section of mets file", ex);
      exceptionToFormattedString(ex, returnType);
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
    return Response.status(statusCode).entity(sb.toString()).build();
  }

  /**
   * Update nested XML section.
   *
   * @param pGroupId The group id the access belongs to [default: USERS]
   * @param pDigitalObjectId ID of the metadata (may be the id of the digital
   * object)
   * @param pSectionId ID of the section (optional parameter if multiple
   * sections with same namespace exists.
   * @param pFileContent New content of the section.
   * @return Response holding status and message.
   */
  @PUT
  @Path("/{prefix}")
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //MediaType.MULTIPART_FORM_DATA)//
  @ApiOperation(value = "Update single section of METS file.", notes = "Section will be selected by prefix and sectionID (optional) of the metadata registered in KIT Data Manager."
          + " If there are multiple sections with the same type an error will occur.")
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Successfully updated.")
    ,
    @ApiResponse(code = 400, message = "Bad request. At least one Parameter is invalid.")
    ,
    @ApiResponse(code = 401, message = "Unauthorized access.")
    ,
    @ApiResponse(code = 404, message = "ID doesn't exist.")
    ,
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response updatePartialXML(
          @ApiParam(value = "GroupId the user belongs to. (User has to be at least manager)", required = false, defaultValue = "USERS") @FormParam("groupId") String pGroupId,
          @ApiParam(value = "Unique ID for the metadata (should be identical to id of linked Digital Data Object).", required = true) @FormParam(value = "oid") String pDigitalObjectId,
          @ApiParam(value = "Section ID for the metadata (mandatory if multiple sections with same type exists).", required = false) @FormParam(value = "sectionId") String pSectionId,
          @ApiParam(value = "Content of XML file to be stored", type = "File", name = "xmlFile", required = true) @FormParam(value = "xmlFile") String pFileContent) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("update section XML: groupID = {} & digitalObjectId = {} & section ID = {} & section file = {}", pGroupId, pDigitalObjectId, pSectionId, pFileContent);
    }

    Response.Status statusCode = Response.Status.CREATED;
    StringBuilder sb = new StringBuilder();

    try {
      String metsDocument = new RestMetaStoreController().updatePartialMetsDocument(context, pGroupId, pDigitalObjectId, pSectionId, pFileContent);

      sb.append(messageToJson(metsDocument));
    } catch (MetaStoreException ex) {
      LOGGER.error("Error while updating section of mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.fromStatusCode(ex.getHttpStatus());
    } catch (Exception ex) {
      LOGGER.error("Uncatched error while updating section of mets file", ex);
      sb.append(exceptionToJson(ex));
      statusCode = Response.Status.INTERNAL_SERVER_ERROR;
    }

    return Response.status(statusCode).entity(sb.toString()).build();
  }

}
