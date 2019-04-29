package software.into.ala.service.status.impl;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.dao.FileDao;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.service.messaging.MessagingService;
import software.into.ala.service.messaging.dto.FileMessageDTO;
import software.into.ala.service.status.StatusUpdatesService;

@Component(scope = ServiceScope.PROTOTYPE)
public class StatusUpdatesServiceImpl implements StatusUpdatesService {
	private static final Logger LOG = LoggerFactory.getLogger(StatusUpdatesServiceImpl.class);

	private PushStreamProvider pushStreamProvider;
	private SimplePushEventSource<FileMessageDTO> simplePushEventSource;
	private ExecutorService fileProcessingStatusConsumerExec;

	@Reference
	private MessagingService messagingService;

	@Reference
	private FileDao fileDao;

	@Reference(name = "camelContext", service = CamelContext.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, bind = "setCamelContext", unbind = "unsetCamelContext")
	private CamelContext camelContext;

	@Activate
	protected void activate() {
		this.pushStreamProvider = new PushStreamProvider();
		this.simplePushEventSource = pushStreamProvider.createSimpleEventSource(FileMessageDTO.class);
		this.fileProcessingStatusConsumerExec = Executors.newSingleThreadExecutor();
	}

	@Deactivate
	protected void deactivate() {
		this.simplePushEventSource.close();
		if (!fileProcessingStatusConsumerExec.isShutdown()) {
			this.fileProcessingStatusConsumerExec.shutdown();
		}
	}

	@Override
	public PushStream<String> getStatusUpdates(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		LOG.debug("Starting processing status consumer for file ID: " + fileId);
		fileProcessingStatusConsumerExec.execute(new StatusUpdatesConsumer(fileId));

		LOG.debug("Creating processing status stream for file ID: " + fileId);
		return pushStreamProvider.createStream(simplePushEventSource).map(fmDTO -> fmDTO.status);
	}

	@Override
	public boolean hasStatusUpdates(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		if (fileDao.exists(fileId)) {
			return !isProcessingFinished(fileDao.findById(fileId));
		}

		return false;
	}

	private class StatusUpdatesConsumer implements Runnable {
		private String fileId;

		public StatusUpdatesConsumer(String fileId) {
			Objects.requireNonNull(fileId, "File ID must be specified!");

			LOG.debug("Creating new instance for file ID: " + fileId);

			this.fileId = fileId;
		}

		@Override
		public void run() {

			try {

				ConsumerTemplate consumerTemplate = getCamelContext().createConsumerTemplate();

				Endpoint endpoint = messagingService.registerStatusUpdatesEndpoint(fileId);

				FileMessageDTO fmDTO = consumerTemplate.receiveBody(endpoint, FileMessageDTO.class);
				LOG.debug("Received message: " + fmDTO);

				while (fmDTO != null) {

					LOG.debug("Publishing message: " + fmDTO);

					simplePushEventSource.publish(fmDTO);

					if (isProcessingFinished(fmDTO)) {
						LOG.debug("Processing is finished!");
						break;
					}

					LOG.debug("Awaiting new message...");

					fmDTO = consumerTemplate.receiveBody(endpoint, FileMessageDTO.class);

					LOG.debug("Received new message: " + fmDTO);
				}

				LOG.debug("Cleaning up..");
				consumerTemplate.cleanUp();

				LOG.debug("Closing stream..");
				simplePushEventSource.endOfStream();

			} catch (Exception e) {
				LOG.error(e.getMessage());
				simplePushEventSource.error(e);
			}
		}
	}

	private boolean isProcessingFinished(FileMessageDTO fmDTO) {
		Objects.requireNonNull(fmDTO, "File must be specified!");

		boolean isProcessingFinished = Objects.nonNull(fmDTO.status)
				? (FileProcessingStatus.analysis_ready == FileProcessingStatus.valueOf(fmDTO.status)
						|| FileProcessingStatus.analysis_failed == FileProcessingStatus.valueOf(fmDTO.status))
				: false;

		return isProcessingFinished;
	}

	private boolean isProcessingFinished(FileDTO fDTO) {
		Objects.requireNonNull(fDTO, "File must be specified!");

		boolean isProcessingFinished = Objects.nonNull(fDTO.status)
				? (FileProcessingStatus.analysis_ready == fDTO.status
						|| FileProcessingStatus.analysis_failed == fDTO.status)
				: false;

		return isProcessingFinished;
	}

	void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	void unsetCamelContext(CamelContext camelContext) {
		this.camelContext = null;
	}

	private CamelContext getCamelContext() {
		return camelContext;
	}
}
