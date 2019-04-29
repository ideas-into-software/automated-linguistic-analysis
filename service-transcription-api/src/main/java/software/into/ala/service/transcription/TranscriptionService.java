package software.into.ala.service.transcription;

import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.service.messaging.dto.FileMessageDTO;

@ProviderType
public interface TranscriptionService {

	void transcribe(FileMessageDTO message);
}
