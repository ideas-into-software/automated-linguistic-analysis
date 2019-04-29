package software.into.ala.service.transcription.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResult;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;

import software.into.ala.dao.FileDao;
import software.into.ala.dao.TranscriptDao;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileFormat;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.service.messaging.MessagingService;
import software.into.ala.service.messaging.dto.FileMessageDTO;
import software.into.ala.service.transcription.TranscriptionService;

@SuppressWarnings("deprecation")
@Component(immediate = true, configurationPid = {
		"software.into.ala.service.transcription" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = TranscriptionServiceImpl.Configuration.class)
public class TranscriptionServiceImpl implements TranscriptionService {
	private static final Logger LOG = LoggerFactory.getLogger(TranscriptionServiceImpl.class);

	private Configuration configuration;

	private IamOptions iAmOptions;

	private ExecutorService transcriptionsExec;

	@Reference
	private MessagingService messagingService;

	@Reference
	private FileDao fileDao;

	@Reference
	private TranscriptDao transcriptDao;

	@ObjectClassDefinition
	@interface Configuration {

		@AttributeDefinition(required = true)
		String endpoint();

		@AttributeDefinition(required = true)
		String apiKey();

		@AttributeDefinition(required = true)
		String fileStorageLocation();
	}

	@Activate
	protected void activate(Configuration configuration) {
		this.configuration = configuration;

		Objects.requireNonNull(this.configuration.endpoint(), "Endpoint is required!");
		Objects.requireNonNull(this.configuration.apiKey(), "API key is required!");
		Objects.requireNonNull(this.configuration.fileStorageLocation(), "File storage location is required!");

		this.iAmOptions = new IamOptions.Builder().apiKey(this.configuration.apiKey()).build();

		this.transcriptionsExec = Executors.newCachedThreadPool();

		messagingService.addServiceInvocationRoute(FileProcessingStatus.transcript_requested,
				"software.into.ala.service.transcription.TranscriptionService",
				"transcribe(software.into.ala.service.messaging.dto.FileMessageDTO)");

	}

	@Override
	public void transcribe(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");

		LOG.debug("Called ::transcribe with file ID: " + message.fileId);

		if (transcriptDao.hasTranscript(message.fileId)) {
			LOG.warn("Transcript for file ID: " + message.fileId + " already exists!");
			transcriptAlreadyExists(message.fileId);
			return;
		}

		Promise<SpeechRecognitionResults> results = transcribeAsync(message.fileId);
		results.onSuccess(r -> processResult(message.fileId, r));
		results.onFailure(r -> handleFailure(message.fileId, r));
	}

	private class TranscriptionWorker implements Runnable {
		private Configuration config;
		private IamOptions options;
		private String fileId;
		private Deferred<SpeechRecognitionResults> deferred;

		public TranscriptionWorker(Configuration config, IamOptions options, String fileId,
				Deferred<SpeechRecognitionResults> deferred) {

			LOG.debug("Creating new instance for file ID: " + fileId);

			this.config = config;
			this.options = options;
			this.fileId = fileId;
			this.deferred = deferred;
		}

		@Override
		public void run() {

			try {

				FileMessageDTO updated = updateStatus(fileId, FileProcessingStatus.transcript_pending);
				messagingService.transcriptPendingEvent(updated);

				SpeechToText speechToText = new SpeechToText(this.options);
				speechToText.setEndPoint(this.config.endpoint());

				FileFormat fileFormat = FileFormat.valueOf(updated.fileFormat);

				Path filePath = getFilePath(this.config.fileStorageLocation(), updated.fileDir, updated.fileName);

				LOG.debug("Processing file: " + filePath.toString());

				try (InputStream fileIs = Files.newInputStream(filePath)) {

					RecognizeOptions recognizeOptions = new RecognizeOptions.Builder().audio(fileIs)
							.contentType(fileFormat.getMime()).timestamps(false).build();

					SpeechRecognitionResults results = speechToText.recognize(recognizeOptions).execute();

					deferred.resolve(results);

				} catch (Throwable t) {
					LOG.error(t.getMessage());

					deferred.fail(t);
				}

			} catch (Exception e) {
				LOG.error(e.getMessage());

				deferred.fail(e);
			}
		}
	}

	private Promise<SpeechRecognitionResults> transcribeAsync(String fileId) {
		final Deferred<SpeechRecognitionResults> deferred = new Deferred<SpeechRecognitionResults>();

		this.transcriptionsExec.execute(new TranscriptionWorker(this.configuration, this.iAmOptions, fileId, deferred));

		return deferred.getPromise();
	}

	private void processResult(String fileId, SpeechRecognitionResults results) throws Exception {
		LOG.debug("Received transcript for file ID: " + fileId);

		List<SpeechRecognitionResult> resultsList = results.getResults();

		String transcripts = transcriptsAsString(resultsList);

		TranscriptDTO tDTO = new TranscriptDTO();
		tDTO.content = transcripts;
		tDTO.fileId = fileId;

		LOG.debug("Saving transcript for file ID: " + fileId);
		transcriptDao.save(fileId, tDTO);

		FileMessageDTO updated = updateStatus(fileId, FileProcessingStatus.transcript_ready);

		messagingService.transcriptReadyEvent(updated);
	}

	private void handleFailure(String fileId, Throwable t) throws Exception {
		updateStatus(fileId, FileProcessingStatus.transcript_failed);
		messagingService.transcriptFailedEvent(fileId, t.getMessage());
	}

	private void transcriptAlreadyExists(String fileId) {
		FileDTO fDTO = fileDao.updateStatus(fileId, FileProcessingStatus.transcript_ready);
		FileMessageDTO fmDTO = messagingService.convertToFileMessageDTO(fDTO);
		messagingService.transcriptReadyEvent(fmDTO);
	}

	private FileMessageDTO updateStatus(String fileId, FileProcessingStatus fps) {
		FileDTO fDTO = fileDao.updateStatus(fileId, fps);

		return messagingService.convertToFileMessageDTO(fDTO);
	}

	private String transcriptsAsString(List<SpeechRecognitionResult> results) {
		return results.stream().flatMap(r -> r.getAlternatives().stream()).map(a -> a.getTranscript())
				.collect(Collectors.joining("; "));
	}

	private Path getFilePath(String storageLocation, String fileDir, String fileName) {
		return Paths.get(storageLocation, fileDir, fileName);
	}
}
