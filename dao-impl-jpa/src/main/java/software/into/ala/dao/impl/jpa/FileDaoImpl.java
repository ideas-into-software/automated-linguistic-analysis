package software.into.ala.dao.impl.jpa;

import static java.util.stream.Collectors.toList;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.dao.FileDao;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.impl.jpa.entities.FileEntity;

@Component
public class FileDaoImpl implements FileDao {
	private static final Logger LOG = LoggerFactory.getLogger(FileDaoImpl.class);

	@Reference
	TransactionControl transactionControl;

	@Reference(name = "provider")
	JPAEntityManagerProvider jpaEntityManagerProvider;

	EntityManager entityManager;

	@Activate
	void activate(Map<String, Object> props) throws SQLException {
		entityManager = jpaEntityManagerProvider.getResource(transactionControl);
	}

	@Override
	public List<FileDTO> findAll() {
		return transactionControl.notSupported(() -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<FileEntity> query = builder.createQuery(FileEntity.class);

			query.from(FileEntity.class);

			return entityManager.createQuery(query).getResultList().stream().map(FileEntity::toDTO).collect(toList());
		});
	}

	@Override
	public FileDTO findById(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.supports(() -> {
			FileEntity file = entityManager.find(FileEntity.class, fileId);

			return Objects.nonNull(file) ? file.toDTO() : null;
		});
	}

	@Override
	public String save(FileDTO data) {
		Objects.requireNonNull(data, "File DTO is required!");

		return transactionControl.required(() -> {
			FileEntity entity = FileEntity.fromDTO(data);

			if (Objects.isNull(entity.getFileId())) {
				entityManager.persist(entity);
			} else {
				entityManager.merge(entity);
			}

			LOG.debug("Saved File with ID : {}", entity.getFileId());

			return entity.getFileId();
		});
	}

	@Override
	public void update(FileDTO data) {
		Objects.requireNonNull(data, "File DTO is required!");

		transactionControl.required(() -> {
			FileEntity entity = FileEntity.fromDTO(data);

			Objects.requireNonNull(entity.getFileId(), "File ID is required!");

			entityManager.merge(entity);

			LOG.debug("Updated File : {}", data);

			return null;
		});
	}

	@Override
	public void delete(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		transactionControl.required(() -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaDelete<FileEntity> query = builder.createCriteriaDelete(FileEntity.class);

			Root<FileEntity> from = query.from(FileEntity.class);

			query.where(builder.equal(from.get("fileId"), fileId));

			entityManager.createQuery(query).executeUpdate();

			LOG.debug("Deleted File with ID : {}", fileId);

			return null;
		});
	}

	@Override
	public FileDTO updateStatus(String fileId, FileProcessingStatus status) {
		Objects.requireNonNull(fileId, "File ID is required!");
		Objects.requireNonNull(status, "File processing status is required!");

		return transactionControl.required(() -> {
			FileEntity file = entityManager.find(FileEntity.class, fileId);

			Objects.requireNonNull(file, "There is no file with id " + fileId);

			file.setStatus(status);

			return entityManager.merge(file).toDTO();
		});
	}

	@Override
	public boolean exists(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.notSupported(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Long> query = builder.createQuery(Long.class);

			Root<FileEntity> from = query.from(FileEntity.class);

			query.select(builder.count(from));

			query.where(builder.equal(from.get("fileId"), fileId));

			return (entityManager.createQuery(query).getSingleResult() > 0) ? true : false;
		});

	}
}
