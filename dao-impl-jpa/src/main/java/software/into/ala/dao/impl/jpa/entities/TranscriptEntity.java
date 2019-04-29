package software.into.ala.dao.impl.jpa.entities;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import software.into.ala.dao.dto.TranscriptDTO;

@Entity
@Table(name = "transcripts")
public class TranscriptEntity {

	@OneToOne
	@JoinColumn(name = "file_id", foreignKey = @ForeignKey(name = "file"))
	private FileEntity file;

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = IDENTITY)
	private Long transcriptId;

	@Column(name = "content")
	private String content;

	public TranscriptDTO toDTO() {
		TranscriptDTO dto = new TranscriptDTO();
		dto.fileId = file.getFileId();
		dto.transcriptId = transcriptId;
		dto.content = content;

		return dto;
	}

	public static TranscriptEntity fromDTO(FileEntity file, TranscriptDTO dto) {
		Objects.requireNonNull(file, "File Entity is required!");
		Objects.requireNonNull(dto, "Transcript DTO is required!");

		TranscriptEntity entity = new TranscriptEntity();
		entity.file = file;
		entity.transcriptId = dto.transcriptId;
		entity.content = dto.content;

		return entity;
	}

	public void setContent(String content) {
		Objects.requireNonNull(content, "Content is required!");

		this.content = content;
	}

	public void setFile(FileEntity file) {
		this.file = file;
	}
}
