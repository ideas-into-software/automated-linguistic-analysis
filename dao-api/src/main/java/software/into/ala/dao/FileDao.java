package software.into.ala.dao;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;

@ProviderType
public interface FileDao {

	List<FileDTO> findAll();

	FileDTO findById(String fileId);

	String save(FileDTO data);

	void update(FileDTO data);

	void delete(String fileId);

	boolean exists(String fileId);

	FileDTO updateStatus(String fileId, FileProcessingStatus status);
}
