package software.into.ala.service.messaging.dto;

import org.osgi.dto.DTO;

public class AnalysisMessageDTO extends DTO implements MessageDTO {
	private static final long serialVersionUID = 1100542778771948011L;

	public long analysisId;
	public String content;
	public String fileId;
}
