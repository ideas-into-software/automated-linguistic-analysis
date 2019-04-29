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

import software.into.ala.dao.AnalysisDao;
import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.impl.jpa.entities.AnalysisEntity;
import software.into.ala.dao.impl.jpa.entities.FileEntity;

@Component
public class AnalysisDaoImpl implements AnalysisDao {
	private static final Logger LOG = LoggerFactory.getLogger(AnalysisDaoImpl.class);

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
	public AnalysisDTO select(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.notSupported(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<AnalysisEntity> query = builder.createQuery(AnalysisEntity.class);

			Root<AnalysisEntity> from = query.from(AnalysisEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			AnalysisEntity aEntity = entityManager.createQuery(query).getSingleResult();

			return Objects.nonNull(aEntity) ? aEntity.toDTO() : null;
		});
	}

	@Override
	public void save(String fileId, AnalysisDTO data) {
		Objects.requireNonNull(fileId, "File ID is required!");
		Objects.requireNonNull(data, "Analysis DTO is required!");

		if (!hasAnalysis(fileId)) {

			transactionControl.required(() -> {
				FileEntity file = entityManager.find(FileEntity.class, fileId);

				Objects.requireNonNull(file, "There is no file with id " + fileId);

				entityManager.merge(AnalysisEntity.fromDTO(file, data));

				LOG.debug("Saved file analysis : {}", data);

				return null;
			});

		} else {

			update(fileId, data);
		}
	}

	@Override
	public void update(String fileId, AnalysisDTO data) {
		Objects.requireNonNull(fileId, "File ID is required!");
		Objects.requireNonNull(data, "Analysis DTO is required!");

		transactionControl.required(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<AnalysisEntity> query = builder.createQuery(AnalysisEntity.class);

			Root<AnalysisEntity> from = query.from(AnalysisEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			AnalysisEntity analysis = entityManager.createQuery(query).getSingleResult();

			Objects.requireNonNull(analysis, "There is no analysis for file id " + fileId);

			analysis.setContent(data.content);

			entityManager.merge(analysis);

			LOG.debug("Updated file transcript : {}", data);

			return null;
		});
	}

	@Override
	public void delete(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		transactionControl.required(() -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaDelete<AnalysisEntity> query = builder.createCriteriaDelete(AnalysisEntity.class);

			Root<AnalysisEntity> from = query.from(AnalysisEntity.class);

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			entityManager.createQuery(query).executeUpdate();

			LOG.debug("Deleted File Analysis : {}", fileId);

			return null;
		});
	}

	@Override
	public boolean hasAnalysis(String fileId) {
		Objects.requireNonNull(fileId, "File ID is required!");

		return transactionControl.notSupported(() -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Long> query = builder.createQuery(Long.class);

			Root<AnalysisEntity> from = query.from(AnalysisEntity.class);

			query.select(builder.count(from));

			query.where(builder.equal(from.get("file").get("fileId"), fileId));

			return (entityManager.createQuery(query).getSingleResult() > 0) ? true : false;
		});
	}
}
