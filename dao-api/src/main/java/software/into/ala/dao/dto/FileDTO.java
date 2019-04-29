package software.into.ala.dao.dto;

import org.osgi.dto.DTO;

public class FileDTO extends DTO {
	public String fileId;
	public String fileName;
	public String fileDir;
	public FileFormat fileFormat;
	public String language;
	public String description;
	public FileProcessingStatus status;
	public String created;
	public TranscriptDTO transcript;
	public AnalysisDTO analysis;
}
