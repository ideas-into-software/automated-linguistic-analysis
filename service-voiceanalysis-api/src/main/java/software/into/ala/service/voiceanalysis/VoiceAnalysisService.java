package software.into.ala.service.voiceanalysis;

import org.apache.commons.fileupload.FileItemIterator;
import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.TranscriptDTO;

@ProviderType
public interface VoiceAnalysisService {

	FileDTO handleSubmitForAnalysis(String fileStorageLocation, FileItemIterator fileItemsIter) throws Exception;

	FileDTO handleRetrieveFile(String fileId);

	TranscriptDTO handleRetrieveTranscript(String fileId);

	AnalysisDTO handleRetrieveAnalysis(String fileId);
}
