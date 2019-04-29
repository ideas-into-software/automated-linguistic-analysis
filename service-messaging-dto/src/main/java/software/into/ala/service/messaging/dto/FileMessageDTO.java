package software.into.ala.service.messaging.dto;

import org.osgi.dto.DTO;

public class FileMessageDTO extends DTO implements MessageDTO {
	private static final long serialVersionUID = -165456219042742647L;

	public String fileId;
	public String fileName;
	public String fileDir;
	public String fileFormat;
	public String language;
	public String description;
	public String status;
	public String created;
	public TranscriptMessageDTO transcript;
	public AnalysisMessageDTO analysis;
}
