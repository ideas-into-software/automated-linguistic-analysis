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

import software.into.ala.dao.dto.AnalysisDTO;

@Entity
@Table(name = "analyses")
public class AnalysisEntity {

	@OneToOne
	@JoinColumn(name = "file_id", foreignKey = @ForeignKey(name = "file"))
	private FileEntity file;

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = IDENTITY)
	private Long analysisId;

	@Column(name = "content")
	private String content;

	public AnalysisDTO toDTO() {
		AnalysisDTO dto = new AnalysisDTO();
		dto.fileId = file.getFileId();
		dto.analysisId = analysisId;
		dto.content = content;

		return dto;
	}

	public static AnalysisEntity fromDTO(FileEntity file, AnalysisDTO dto) {
		Objects.requireNonNull(file, "File Entity is required!");
		Objects.requireNonNull(dto, "Analysis DTO is required!");

		AnalysisEntity entity = new AnalysisEntity();
		entity.file = file;
		entity.analysisId = dto.analysisId;
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
