package software.into.ala.rest.status;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.osgi.util.pushstream.PushStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.service.status.StatusUpdatesService;

@Component(service = StatusUpdatesRestController.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@Path("status")
@JSONRequired
public class StatusUpdatesRestController {
	private static final Logger LOG = LoggerFactory.getLogger(StatusUpdatesRestController.class);

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private StatusUpdatesService statusUpdateService;

	@Context
	private Sse sse;

	@Context
	private SseEventSink sink;

	@GET
	@Path("{fileId}")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void getStatusUpdates(@Context SseEventSink sink, @PathParam("fileId") String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		LOG.debug("Called ::getStatusUpdates with file ID: " + fileId);

		this.sink = sink;

		if (!statusUpdateService.hasStatusUpdates(fileId)) {

			String errorMsg = "No status updates for file ID: " + fileId + "!";

			LOG.error(errorMsg);

			failure(new IllegalArgumentException(errorMsg));

		} else {

			PushStream<String> fPushStream = statusUpdateService.getStatusUpdates(fileId);
			fPushStream.forEach(this::deliverEvent).onFailure(this::failure).onResolve(this::resolved);
		}
	}

	private void deliverEvent(String eventData) {
		LOG.debug("Delivering: " + eventData);

		OutboundSseEvent event = this.sse.newEvent("status", eventData);
		this.sink.send(event);
	}

	private void failure(Throwable t) {
		LOG.debug("Error: " + t.getMessage());

		OutboundSseEvent event = this.sse.newEvent("error", t.getMessage());
		this.sink.send(event);
	}

	private void resolved() {
		LOG.debug("End of stream");

		OutboundSseEvent event = this.sse.newEvent("end", "End of Stream");
		this.sink.send(event);
	}
}
