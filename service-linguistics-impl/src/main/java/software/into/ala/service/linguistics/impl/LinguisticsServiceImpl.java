package software.into.ala.service.linguistics.impl;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;

import software.into.ala.dao.AnalysisDao;
import software.into.ala.dao.FileDao;
import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.service.linguistics.LinguisticsService;
import software.into.ala.service.messaging.MessagingService;
import software.into.ala.service.messaging.dto.FileMessageDTO;

@SuppressWarnings("deprecation")
@Component(immediate = true, configurationPid = {
		"software.into.ala.service.linguistics" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = LinguisticsServiceImpl.Configuration.class)
public class LinguisticsServiceImpl implements LinguisticsService {
	private static final Logger LOG = LoggerFactory.getLogger(LinguisticsServiceImpl.class);

	private Configuration configuration;

	private IamOptions iAmOptions;

	private ExecutorService analysesExec;

	@Reference
	private MessagingService messagingService;

	@Reference
	private FileDao fileDao;

	@Reference
	private AnalysisDao analysisDao;

	@ObjectClassDefinition
	@interface Configuration {

		@AttributeDefinition(required = true)
		String endpoint();

		@AttributeDefinition(required = true)
		String apiKey();

		@AttributeDefinition(required = true)
		String apiVersion();
	}

	@Activate
	protected void activate(Configuration configuration) {
		this.configuration = configuration;

		Objects.requireNonNull(this.configuration.endpoint(), "Endpoint is required!");
		Objects.requireNonNull(this.configuration.apiKey(), "API key is required!");
		Objects.requireNonNull(this.configuration.apiVersion(), "API version is required!");

		this.iAmOptions = new IamOptions.Builder().apiKey(this.configuration.apiKey()).build();

		this.analysesExec = Executors.newCachedThreadPool();

		messagingService.addServiceInvocationRoute(FileProcessingStatus.transcript_ready,
				"software.into.ala.service.linguistics.LinguisticsService",
				"analyse(software.into.ala.service.messaging.dto.FileMessageDTO)");

		messagingService.addServiceInvocationRoute(FileProcessingStatus.analysis_requested,
				"software.into.ala.service.linguistics.LinguisticsService",
				"analyse(software.into.ala.service.messaging.dto.FileMessageDTO)");
	}

	@Override
	public void analyse(FileMessageDTO message) {
		Objects.requireNonNull(message, "File must be specified!");
		Objects.requireNonNull(message.transcript.content, "Transcript must be specified!");

		LOG.debug("Called ::analyse with file ID: " + message.fileId);

		if (analysisDao.hasAnalysis(message.fileId)) {
			LOG.warn("Analysis for file ID: " + message.fileId + " already exists!");
			analysisAlreadyExists(message.fileId);
			return;
		}

		Promise<ToneAnalysis> results = analyseAsync(message);
		results.onSuccess(r -> processResult(message.fileId, r));
		results.onFailure(r -> handleFailure(message.fileId, r));
	}

	private class AnalysisWorker implements Runnable {
		private Configuration config;
		private IamOptions options;
		private String fileId;
		private String transcript;
		private Deferred<ToneAnalysis> deferred;

		public AnalysisWorker(Configuration config, IamOptions options, String fileId, String transcript,
				Deferred<ToneAnalysis> deferred) {

			LOG.debug("Creating new instance for file ID: " + fileId);

			this.config = config;
			this.options = options;
			this.fileId = fileId;
			this.transcript = transcript;
			this.deferred = deferred;
		}

		@Override
		public void run() {

			try {

				FileMessageDTO updated = updateStatus(fileId, FileProcessingStatus.analysis_pending);
				messagingService.analysisPendingEvent(updated);

				ToneAnalyzer toneAnalyzer = new ToneAnalyzer(this.config.apiVersion(), this.options);
				toneAnalyzer.setEndPoint(this.config.endpoint());

				ToneOptions toneOptions = new ToneOptions.Builder().text(transcript).build();

				ToneAnalysis results = toneAnalyzer.tone(toneOptions).execute();

				deferred.resolve(results);

			} catch (Exception e) {
				LOG.error(e.getMessage());

				deferred.fail(e);
			}
		}
	}

	private Promise<ToneAnalysis> analyseAsync(FileMessageDTO message) {
		final Deferred<ToneAnalysis> deferred = new Deferred<ToneAnalysis>();

		this.analysesExec.execute(new AnalysisWorker(this.configuration, this.iAmOptions, message.fileId,
				message.transcript.content, deferred));

		return deferred.getPromise();
	}

	private void processResult(String fileId, ToneAnalysis results) throws Exception {
		LOG.debug("Received linguistic analysis for file ID: " + fileId);

		AnalysisDTO aDTO = new AnalysisDTO();
		aDTO.content = results.toString();
		aDTO.fileId = fileId;

		LOG.debug("Saving linguistic analysis for file ID: " + fileId);
		analysisDao.save(fileId, aDTO);

		FileMessageDTO updated = updateStatus(fileId, FileProcessingStatus.analysis_ready);

		messagingService.analysisReadyEvent(updated);
	}

	private void handleFailure(String fileId, Throwable t) throws Exception {
		updateStatus(fileId, FileProcessingStatus.analysis_failed);
		messagingService.analysisFailedEvent(fileId, t.getMessage());
	}

	private void analysisAlreadyExists(String fileId) {
		FileDTO fDTO = fileDao.updateStatus(fileId, FileProcessingStatus.analysis_ready);
		FileMessageDTO fmDTO = messagingService.convertToFileMessageDTO(fDTO);
		messagingService.analysisReadyEvent(fmDTO);
	}

	private FileMessageDTO updateStatus(String fileId, FileProcessingStatus fps) {
		FileDTO fDTO = fileDao.updateStatus(fileId, fps);

		return messagingService.convertToFileMessageDTO(fDTO);
	}
}
