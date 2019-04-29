package software.into.ala.service.messaging.dto;

import org.osgi.dto.DTO;

public class TranscriptMessageDTO extends DTO implements MessageDTO {
	private static final long serialVersionUID = -561264373745770520L;

	public long transcriptId;
	public String content;
	public String fileId;
}
