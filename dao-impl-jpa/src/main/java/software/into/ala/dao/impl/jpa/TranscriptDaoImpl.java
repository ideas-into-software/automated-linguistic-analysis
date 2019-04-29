package software.into.ala.dao.impl.jpa;

import java.sql.SQLException;
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

import software.into.ala.dao.TranscriptDao;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.dao.impl.jpa.entities.FileEntity;
import software.into.ala.dao.impl.jpa.entities.TranscriptEntity;

@Component
public class TranscriptDaoImpl implements TranscriptDao {
	private static final Logger LOG = LoggerFactory.getLogger(TranscriptDaoImpl.class);

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
	public TranscriptDTO select(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.notSupported(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<TranscriptEntity> query = builder.createQuery(TranscriptEntity.class);

			Root<TranscriptEntity> from = query.from(TranscriptEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			TranscriptEntity tEntity = entityManager.createQuery(query).getSingleResult();

			return Objects.nonNull(tEntity) ? tEntity.toDTO() : null;
		});
	}

	@Override
	public void save(String fileId, TranscriptDTO data) {
		Objects.requireNonNull(fileId, "File ID is required!");
		Objects.requireNonNull(data, "Transcript DTO is required!");

		if (!hasTranscript(fileId)) {

			transactionControl.required(() -> {
				FileEntity file = entityManager.find(FileEntity.class, fileId);

				Objects.requireNonNull(file, "There is no file with id " + fileId);

				entityManager.merge(TranscriptEntity.fromDTO(file, data));

				LOG.debug("Saved file transcript : {}", data);

				return null;
			});

		} else {

			update(fileId, data);
		}
	}

	@Override
	public void update(String fileId, TranscriptDTO data) {
		Objects.requireNonNull(fileId, "File ID is required!");
		Objects.requireNonNull(data, "Transcript DTO is required!");

		transactionControl.required(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<TranscriptEntity> query = builder.createQuery(TranscriptEntity.class);

			Root<TranscriptEntity> from = query.from(TranscriptEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			TranscriptEntity transcript = entityManager.createQuery(query).getSingleResult();

			Objects.requireNonNull(transcript, "There is no transcript for file id " + fileId);

			transcript.setContent(data.content);

			entityManager.merge(transcript);

			LOG.debug("Updated file transcript : {}", data);

			return null;
		});
	}

	@Override
	public void delete(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		transactionControl.required(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaDelete<TranscriptEntity> query = builder.createCriteriaDelete(TranscriptEntity.class);

			Root<TranscriptEntity> from = query.from(TranscriptEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			entityManager.createQuery(query).executeUpdate();

			LOG.debug("Deleted file transcript : {}", fileId);

			return null;
		});
	}

	@Override
	public boolean hasTranscript(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.notSupported(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Long> query = builder.createQuery(Long.class);

			Root<TranscriptEntity> from = query.from(TranscriptEntity.class);

			query.select(builder.count(from));

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			return (entityManager.createQuery(query).getSingleResult() > 0) ? true : false;
		});
	}
}
