package software.into.ala.dao;

import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.dao.dto.TranscriptDTO;

@ProviderType
public interface TranscriptDao {

	TranscriptDTO select(String fileId);

	void save(String fileId, TranscriptDTO data);

	void update(String fileId, TranscriptDTO data);

	void delete(String fileId);

	boolean hasTranscript(String fileId);
}