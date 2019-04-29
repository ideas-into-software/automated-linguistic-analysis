package software.into.ala.rest.voiceanalysis;

import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.service.voiceanalysis.VoiceAnalysisService;

@Component(service = VoiceAnalysisRestController.class, configurationPid = {
		"org.apache.aries.jax.rs.whiteboard.default" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@JaxrsResource
@Path("voiceanalysis")
@Produces(MediaType.APPLICATION_JSON)
@JSONRequired
public class VoiceAnalysisRestController {
	private static final Logger LOG = LoggerFactory.getLogger(VoiceAnalysisRestController.class);

	@Reference
	private VoiceAnalysisService analysisService;

	private Map<String, Object> configuration;

	@Activate
	protected void activate(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response submitForAnalysis(@Context HttpServletRequest request) {
		LOG.debug("Called ::submitForAnalysis");

		String fileStorageLocation = getFileStorageLocation();
		if (Objects.isNull(fileStorageLocation)) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		if (!isRequestMultipart(request)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		if (!isRequestSizeValid(request)) {
			return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build();
		}

		FileDTO fileDTO = new FileDTO();

		try {

			ServletFileUpload fileUpload = new ServletFileUpload();

			fileDTO = analysisService.handleSubmitForAnalysis(fileStorageLocation, fileUpload.getItemIterator(request));

			return Response.status(Response.Status.CREATED).entity(fileDTO).build();

		} catch (Throwable t) {
			LOG.error(t.getMessage());

			if (t instanceof IllegalStateException) {
				return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(t.getMessage()).build();
			} else if (t instanceof NullPointerException) {
				return Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getMessage()).build();
			}
		}
	}

	@GET
	@Path("{fileId}")
	public Response retrieveFile(@PathParam("fileId") String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		LOG.debug("Called ::retrieveFile with fileId: " + fileId);

		FileDTO file = analysisService.handleRetrieveFile(fileId);
		if (file != null) {
			LOG.info("Found file with ID: " + file);

			return Response.status(Response.Status.OK).entity(file).build();

		} else {
			LOG.info("Could not find analysis with ID: " + fileId);

			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("{fileId}/transcript")
	public Response retrieveTranscript(@PathParam("fileId") String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		LOG.debug("Called ::retrieveTranscript with fileId: " + fileId);

		TranscriptDTO transcript = analysisService.handleRetrieveTranscript(fileId);
		if (transcript != null) {
			LOG.info("Found transcript for file ID: " + fileId);

			return Response.status(Response.Status.OK).entity(transcript).build();

		} else {
			LOG.info("Could not find transcript for file ID: " + fileId);

			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("{fileId}/analysis")
	public Response retrieveAnalysis(@PathParam("fileId") String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		LOG.debug("Called ::retrieveAnalysis with fileId: " + fileId);

		AnalysisDTO analysis = analysisService.handleRetrieveAnalysis(fileId);
		if (analysis != null) {
			LOG.info("Found analysis for file ID: " + fileId);

			return Response.status(Response.Status.OK).entity(analysis).build();

		} else {
			LOG.info("Could not find analysis for file ID: " + fileId);

			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	private boolean isRequestSizeValid(@Context HttpServletRequest request) {
		boolean isRequestSizeValid = false;

		long rContentLength = request.getContentLengthLong();
		LOG.debug("Request content type length is: " + rContentLength);

		long maxRequestSize = getMaxRequestSize();
		LOG.debug("Max request size is: " + maxRequestSize);

		if (rContentLength <= maxRequestSize) {
			isRequestSizeValid = true;
		}

		return isRequestSizeValid;
	}

	private boolean isRequestMultipart(@Context HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request);
	}

	private String getFileStorageLocation() {
		return (String) configuration.get("osgi.http.whiteboard.servlet.multipart.location");
	}

	private long getMaxRequestSize() {
		return ((Long) configuration.get("osgi.http.whiteboard.servlet.multipart.maxRequestSize")).longValue();
	}
}
