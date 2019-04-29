package software.into.ala.service.messaging;

import org.apache.camel.Endpoint;
import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.service.messaging.dto.AnalysisMessageDTO;
import software.into.ala.service.messaging.dto.FileMessageDTO;
import software.into.ala.service.messaging.dto.TranscriptMessageDTO;

@ProviderType
public interface MessagingService {

	void transcriptRequestedEvent(FileMessageDTO message);

	void transcriptPendingEvent(FileMessageDTO message);

	void transcriptReadyEvent(FileMessageDTO message);

	void transcriptFailedEvent(String fileId, String error);

	void analysisRequestedEvent(FileMessageDTO message);

	void analysisPendingEvent(FileMessageDTO message);

	void analysisReadyEvent(FileMessageDTO message);

	void analysisFailedEvent(String fileId, String error);

	void addServiceInvocationRoute(FileProcessingStatus status, String serviceName, String methodName);

	Endpoint registerStatusUpdatesEndpoint(String fileId);

	FileMessageDTO convertToFileMessageDTO(FileDTO fileDTO);

	TranscriptMessageDTO convertToTranscriptMessageDTO(TranscriptDTO transcriptDTO);

	AnalysisMessageDTO convertToAnalysisMessageDTO(AnalysisDTO analysisDTO);
}
