package software.into.ala.dao.impl.jpa.entities;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileFormat;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.impl.jpa.converters.TimestampLocalDateTimeConverter;

@Entity
@Table(name = "files")
public class FileEntity {

	@Id
	@Column(name = "id", unique = true)
	@GeneratedValue(strategy = IDENTITY)
	private String fileId;

	@Column(name = "name")
	private String fileName;

	@Column(name = "dir")
	private String fileDir;

	@Enumerated(EnumType.STRING)
	@Column(name = "format")
	private FileFormat fileFormat;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private FileProcessingStatus status;

	@Column(name = "language")
	private String language;

	@Column(name = "description")
	private String description;

	@Column(name = "created", insertable = false)
	@Convert(converter = TimestampLocalDateTimeConverter.class)
	private LocalDateTime created;

	@OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
	private TranscriptEntity transcript;

	@OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
	private AnalysisEntity analysis;

	public String getFileId() {
		return fileId;
	}

	public FileDTO toDTO() {
		FileDTO dto = new FileDTO();
		dto.fileId = fileId;
		dto.fileName = fileName;
		dto.fileDir = fileDir;
		dto.fileFormat = fileFormat;
		dto.language = language;
		dto.description = description;
		dto.status = status;
		dto.created = Objects.nonNull(created) ? created.toString() : null;
		dto.transcript = Objects.nonNull(transcript) ? transcript.toDTO() : null;
		dto.analysis = Objects.nonNull(analysis) ? analysis.toDTO() : null;

		return dto;
	}

	public static FileEntity fromDTO(FileDTO dto) {
		Objects.requireNonNull(dto, "File DTO is required!");

		FileEntity entity = new FileEntity();
		entity.fileId = Objects.nonNull(dto.fileId) ? dto.fileId : null;
		entity.fileName = dto.fileName;
		entity.fileDir = dto.fileDir;
		entity.fileFormat = dto.fileFormat;
		entity.language = dto.language;
		entity.description = dto.description;
		entity.status = dto.status;
		entity.created = Objects.nonNull(dto.created) ? LocalDateTime.parse(dto.created) : null;
		entity.transcript = Objects.nonNull(dto.transcript) ? TranscriptEntity.fromDTO(entity, dto.transcript) : null;
		entity.analysis = Objects.nonNull(dto.analysis) ? AnalysisEntity.fromDTO(entity, dto.analysis) : null;

		return entity;
	}

	public TranscriptEntity getTranscript() {
		return transcript;
	}

	public void setTranscript(TranscriptEntity transcript) {
		this.transcript = transcript;
		transcript.setFile(this);
	}

	public AnalysisEntity getAnalysis() {
		return analysis;
	}

	public void setAnalysis(AnalysisEntity analysis) {
		this.analysis = analysis;
		analysis.setFile(this);
	}

	public void setStatus(FileProcessingStatus status) {
		this.status = status;
	}
}
