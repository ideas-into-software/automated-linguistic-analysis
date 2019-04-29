package software.into.ala.service.messaging.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.service.messaging.MessagingService;
import software.into.ala.service.messaging.dto.AnalysisMessageDTO;
import software.into.ala.service.messaging.dto.FileMessageDTO;
import software.into.ala.service.messaging.dto.TranscriptMessageDTO;
import software.into.ala.service.messaging.impl.internal.SimpleRoute;

@Component(immediate = true, configurationPid = {
		"software.into.ala.service.messaging" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MessagingServiceImpl.Config.class)
public class MessagingServiceImpl implements MessagingService {
	private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceImpl.class);

	private static final String SCHEME_RABBITMQ = "rabbitmq";
	private static final String SCHEME_BEAN = "bean";

	private Config configuration;

	private List<RouteBuilder> routesToAdd;

	@Reference(name = "camelContext", service = CamelContext.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, bind = "setCamelContext", unbind = "unsetCamelContext")
	private CamelContext camelContext;

	@Activate
	protected void activate(Config config) {
		this.configuration = config;
		this.routesToAdd = new CopyOnWriteArrayList<RouteBuilder>();
	}

	@ObjectClassDefinition
	@interface Config {

		@AttributeDefinition(required = true)
		String host() default "localhost";

		@AttributeDefinition(required = true)
		int port() default 5672;

		@AttributeDefinition(required = false)
		String username() default "guest";

		@AttributeDefinition(required = false)
		String password() default "guest";

		@AttributeDefinition(required = false)
		String exchangeName() default "alaXchange";

		@AttributeDefinition(required = false)
		String exchangeType() default "topic";

		@AttributeDefinition(required = false)
		boolean durable() default true;

		@AttributeDefinition(required = false)
		boolean autoDelete() default true;

		@AttributeDefinition(required = false)
		boolean autoAck() default true;

		@AttributeDefinition(required = false)
		int threadPoolSize() default 5;

		@AttributeDefinition(required = false)
		int requestTimeout() default 2000;

		@AttributeDefinition(required = false)
		boolean transferException() default true;
	}

	@Override
	public void transcriptRequestedEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::transcriptRequestedEvent with file ID: " + message.fileId);

		publishProcessingEvent(message, FileProcessingStatus.transcript_requested);

		publishStatusUpdateEvent(message, FileProcessingStatus.transcript_requested);
	}

	@Override
	public void transcriptPendingEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::transcriptRequestedEvent with file ID: " + message.fileId);

		publishStatusUpdateEvent(message, FileProcessingStatus.transcript_pending);
	}

	@Override
	public void transcriptReadyEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::transcriptReadyEvent with file ID: " + message.fileId);

		publishProcessingEvent(message, FileProcessingStatus.transcript_ready);

		publishStatusUpdateEvent(message, FileProcessingStatus.transcript_ready);
	}

	@Override
	public void transcriptFailedEvent(String fileId, String error) {
		Objects.requireNonNull(fileId, "File ID must be specified!");
		Objects.requireNonNull(error, "Error must be specified!");

		LOG.debug("Called ::transcriptFailedEvent with file ID: " + fileId);

		publishStatusUpdateEvent(fileId, error, FileProcessingStatus.transcript_failed);
	}

	@Override
	public void analysisRequestedEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::analysisRequestedEvent with file ID: " + message.fileId);

		publishProcessingEvent(message, FileProcessingStatus.analysis_requested);

		publishStatusUpdateEvent(message, FileProcessingStatus.analysis_requested);
	}

	@Override
	public void analysisPendingEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::analysisPendingEvent with file ID: " + message.fileId);

		publishStatusUpdateEvent(message, FileProcessingStatus.analysis_pending);
	}

	@Override
	public void analysisReadyEvent(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::analysisReadyEvent with file ID: " + message.fileId);

		publishStatusUpdateEvent(message, FileProcessingStatus.analysis_ready);
	}

	@Override
	public void analysisFailedEvent(String fileId, String error) {
		Objects.requireNonNull(fileId, "File ID must be specified!");
		Objects.requireNonNull(error, "Error must be specified!");

		LOG.debug("Called ::analysisFailedEvent with file ID: " + fileId);

		publishStatusUpdateEvent(fileId, error, FileProcessingStatus.analysis_failed);
	}

	@Override
	public void addServiceInvocationRoute(FileProcessingStatus status, String serviceName, String methodName) {
		Objects.requireNonNull(status, "Status must be specified!");
		Objects.requireNonNull(serviceName, "Service name must be specified!");
		Objects.requireNonNull(methodName, "Method name must be specified!");

		String fromUri = buildRabbitMQEndpointUri(status);
		String toUri = buildServiceMethodInvocationEndpointUri(serviceName, methodName);

		SimpleRoute route = new SimpleRoute(fromUri, toUri);

		if (hasCamelContext()) {
			LOG.debug("Camel context available, adding routes immediately...");
			addRoute(route);
		} else {
			LOG.debug("Camel context not available yet, will add routes later...");
			this.routesToAdd.add(route);
		}
	}

	@Override
	public Endpoint registerStatusUpdatesEndpoint(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		String key = buildStatusUpdateKey(fileId);

		String uri = buildRabbitMQEndpointUri(key);
		LOG.debug("URI: " + uri);

		Endpoint endpoint = getCamelContext().getEndpoint(uri);
		LOG.debug("Endpoint: " + endpoint);

		return endpoint;
	}

	@Override
	public FileMessageDTO convertToFileMessageDTO(FileDTO fileDTO) {
		Objects.requireNonNull(fileDTO, "File DTO must be specified!");

		FileMessageDTO fmDTO = new FileMessageDTO();
		if (Objects.nonNull(fileDTO)) {
			fmDTO.fileId = fileDTO.fileId;
			fmDTO.fileName = fileDTO.fileName;
			fmDTO.fileDir = fileDTO.fileDir;
			fmDTO.fileFormat = Objects.nonNull(fileDTO.fileFormat) ? fileDTO.fileFormat.name() : null;
			fmDTO.language = fileDTO.language;
			fmDTO.description = fileDTO.description;
			fmDTO.status = Objects.nonNull(fileDTO.status) ? fileDTO.status.name() : null;
			fmDTO.created = fileDTO.created;
			fmDTO.transcript = Objects.nonNull(fileDTO.transcript) ? convertToTranscriptMessageDTO(fileDTO.transcript)
					: null;
			fmDTO.analysis = Objects.nonNull(fileDTO.analysis) ? convertToAnalysisMessageDTO(fileDTO.analysis) : null;
		}

		return fmDTO;
	}

	@Override
	public TranscriptMessageDTO convertToTranscriptMessageDTO(TranscriptDTO transcriptDTO) {
		Objects.requireNonNull(transcriptDTO, "Transcript DTO must be specified!");

		TranscriptMessageDTO tmDTO = new TranscriptMessageDTO();
		if (Objects.nonNull(transcriptDTO)) {
			tmDTO.transcriptId = transcriptDTO.transcriptId;
			tmDTO.content = transcriptDTO.content;
			tmDTO.fileId = transcriptDTO.fileId;
		}

		return tmDTO;
	}

	@Override
	public AnalysisMessageDTO convertToAnalysisMessageDTO(AnalysisDTO analysisDTO) {
		Objects.requireNonNull(analysisDTO, "Analysis DTO must be specified!");

		AnalysisMessageDTO amDTO = new AnalysisMessageDTO();
		if (Objects.nonNull(analysisDTO)) {
			amDTO.analysisId = analysisDTO.analysisId;
			amDTO.content = analysisDTO.content;
			amDTO.fileId = analysisDTO.fileId;
		}

		return amDTO;
	}

	private void publishProcessingEvent(FileMessageDTO message, FileProcessingStatus status) {
		Objects.requireNonNull(message, "File must be specified!");
		Objects.requireNonNull(status, "Status must be specified!");

		String uri = buildRabbitMQEndpointUri(status, message.fileId);
		LOG.debug("URI: " + uri);

		Endpoint endpoint = getCamelContext().getEndpoint(uri);
		LOG.debug("Endpoint: " + endpoint);

		getCamelContext().createProducerTemplate().sendBody(endpoint, message);
	}

	@SuppressWarnings("unused")
	private void publishFailedEvent(String fileId, String error, FileProcessingStatus status) {
		Objects.requireNonNull(fileId, "File ID must be specified!");
		Objects.requireNonNull(error, "Error must be specified!");
		Objects.requireNonNull(status, "Status must be specified!");

		String uri = buildRabbitMQEndpointUri(status, fileId);
		LOG.debug("URI: " + uri);

		Endpoint endpoint = getCamelContext().getEndpoint(uri);
		LOG.debug("Endpoint: " + endpoint);

		getCamelContext().createProducerTemplate().sendBody(endpoint, error);
	}

	private void publishStatusUpdateEvent(FileMessageDTO message, FileProcessingStatus status) {
		Objects.requireNonNull(message, "File must be specified!");
		Objects.requireNonNull(status, "Status must be specified!");

		String key = buildStatusUpdateKey(message.fileId);
		LOG.debug("Key: " + key);

		String uri = buildRabbitMQEndpointUri(key);
		LOG.debug("URI: " + uri);

		Endpoint endpoint = getCamelContext().getEndpoint(uri);
		LOG.debug("Endpoint: " + endpoint);

		getCamelContext().createProducerTemplate().sendBody(endpoint, message);
	}

	private void publishStatusUpdateEvent(String fileId, String error, FileProcessingStatus status) {
		Objects.requireNonNull(fileId, "File ID must be specified!");
		Objects.requireNonNull(error, "Error must be specified!");
		Objects.requireNonNull(status, "Status must be specified!");

		String key = buildStatusUpdateKey(fileId);
		LOG.debug("Key: " + key);

		String uri = buildRabbitMQEndpointUri(key);
		LOG.debug("URI: " + uri);

		Endpoint endpoint = getCamelContext().getEndpoint(uri);
		LOG.debug("Endpoint: " + endpoint);

		getCamelContext().createProducerTemplate().sendBody(endpoint, error);
	}

	private String buildKey(FileProcessingStatus status, String fileId) {
		Objects.requireNonNull(status, "Status must be specified!");
		Objects.requireNonNull(fileId, "File ID must be specified!");

		StringBuilder key = new StringBuilder();

		key.append(status.getKey());

		return key.toString();
	}

	private String buildStatusUpdateKey(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		StringBuilder key = new StringBuilder();

		key.append("status").append(".").append(normalizeFileId(fileId));

		return key.toString();
	}

	private String normalizeFileId(String fileId) {
		return fileId.replace("-", "");
	}

	private String buildRabbitMQEndpointUri(FileProcessingStatus status, String fileId) {
		Objects.requireNonNull(status, "Status must be specified!");
		Objects.requireNonNull(fileId, "File ID must be specified!");

		String key = buildKey(status, fileId);

		return buildRabbitMQEndpointUri(key);
	}

	private String buildRabbitMQEndpointUri(FileProcessingStatus status) {
		Objects.requireNonNull(status, "Status must be specified!");

		StringBuilder key = new StringBuilder();

		key.append(status.getKey());

		return buildRabbitMQEndpointUri(key.toString());
	}

	private String buildRabbitMQEndpointUri(String key) {
		Objects.requireNonNull(key, "Key must be specified!");

		StringBuilder endpointUri = new StringBuilder();

		endpointUri.append(SCHEME_RABBITMQ).append("://").append(configuration.host()).append(":")
				.append(configuration.port()).append("/").append(configuration.exchangeName()).append("?")
				.append("exchangeType=").append(configuration.exchangeType()).append("&").append("queue=").append(key)
				.append("&").append("durable=").append(configuration.durable()).append("&").append("autoDelete=")
				.append(configuration.autoDelete()).append("&").append("routingKey=").append(key).append("&")
				.append("username=").append(configuration.username()).append("&").append("password=")
				.append(configuration.password()).append("&").append("autoAck=").append(configuration.autoAck())
				.append("&").append("threadPoolSize=").append(configuration.threadPoolSize()).append("&")
				.append("transferException=").append(configuration.transferException()).append("&")
				.append("requestTimeout=").append(configuration.requestTimeout());

		return endpointUri.toString();
	}

	private String buildServiceMethodInvocationEndpointUri(String service, String method) {
		Objects.requireNonNull(service, "Service must be specified!");
		Objects.requireNonNull(method, "Method must be specified!");

		StringBuilder endpointUri = new StringBuilder();

		endpointUri.append(SCHEME_BEAN).append(":").append(service).append("?method=").append(method);

		return endpointUri.toString();
	}

	private void addRoute(RouteBuilder route) {
		try {
			getCamelContext().addRoutes(route);
		} catch (Exception e) {
			LOG.error("Error adding route: " + e.getMessage());
		}
	}

	private void addRoutes() {
		if (!this.routesToAdd.isEmpty()) {
			LOG.debug("Camel context available now, adding routes..");

			this.routesToAdd.forEach(r -> addRoute(r));
			this.routesToAdd.clear();
		}
	}

	void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;

		addRoutes();
	}

	void unsetCamelContext(CamelContext camelContext) {
		this.camelContext = null;
	}

	private CamelContext getCamelContext() {
		return camelContext;
	}

	private boolean hasCamelContext() {
		return Objects.nonNull(camelContext);
	}
}
