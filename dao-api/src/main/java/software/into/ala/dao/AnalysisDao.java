package software.into.ala.dao;

import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.dao.dto.AnalysisDTO;

@ProviderType
public interface AnalysisDao {

	AnalysisDTO select(String fileId);

	void save(String fileId, AnalysisDTO data);

	void update(String fileId, AnalysisDTO data);

	void delete(String fileId);

	boolean hasAnalysis(String fileId);
}
