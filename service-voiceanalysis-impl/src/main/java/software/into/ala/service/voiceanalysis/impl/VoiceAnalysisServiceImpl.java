package software.into.ala.service.voiceanalysis.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.into.ala.dao.AnalysisDao;
import software.into.ala.dao.FileDao;
import software.into.ala.dao.TranscriptDao;
import software.into.ala.dao.dto.AnalysisDTO;
import software.into.ala.dao.dto.FileDTO;
import software.into.ala.dao.dto.FileFormat;
import software.into.ala.dao.dto.FileProcessingStatus;
import software.into.ala.dao.dto.TranscriptDTO;
import software.into.ala.service.messaging.MessagingService;
import software.into.ala.service.messaging.dto.FileMessageDTO;
import software.into.ala.service.voiceanalysis.VoiceAnalysisService;

@Component
public class VoiceAnalysisServiceImpl implements VoiceAnalysisService {
	private static final Logger LOG = LoggerFactory.getLogger(VoiceAnalysisServiceImpl.class);

	@Reference
	private FileDao fileDao;

	@Reference
	private TranscriptDao transcriptDao;

	@Reference
	private AnalysisDao analysisDao;

	@Reference
	private MessagingService messagingService;

	@Override
	public FileDTO handleSubmitForAnalysis(String fileStorageLocation, FileItemIterator fileItemsIter)
			throws Exception {

		FileDTO fileDTO = new FileDTO();

		try {

			fileDTO = processSubmitForAnalysisRequest(fileStorageLocation, fileItemsIter);

			validateSubmitForAnalysisFields(fileDTO);

			fileDTO.status = FileProcessingStatus.transcript_requested;

			String fileId = fileDao.save(fileDTO);
			fileDTO.fileId = fileId;
			LOG.info("File created with ID: " + fileId);

			fileDTO = fileDao.findById(fileId);

			FileMessageDTO fmDTO = messagingService.convertToFileMessageDTO(fileDTO);

			messagingService.transcriptRequestedEvent(fmDTO);

			return fileDTO;

		} catch (Throwable t) {
			cleanupOnFail(fileDTO, fileStorageLocation);

			LOG.error(t.getMessage());
			throw new Exception(t);
		}
	}

	@Override
	public FileDTO handleRetrieveFile(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		return fileDao.findById(fileId);
	}

	@Override
	public TranscriptDTO handleRetrieveTranscript(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		return transcriptDao.select(fileId);
	}

	@Override
	public AnalysisDTO handleRetrieveAnalysis(String fileId) {
		Objects.requireNonNull(fileId, "File ID must be specified!");

		return analysisDao.select(fileId);
	}

	private FileDTO processSubmitForAnalysisRequest(String fileStorageLocation, FileItemIterator fileItemsIter)
			throws Exception {

		FileDTO fileDTO = new FileDTO();

		try {

			while (fileItemsIter.hasNext()) {
				FileItemStream item = fileItemsIter.next();

				if (item.isFormField()) {
					processFormField(item, fileDTO);

				} else if ("file".equalsIgnoreCase(item.getFieldName())) {
					processFileField(item, fileStorageLocation, fileDTO);

				}
			}

		} catch (IllegalStateException | FileUploadException | IOException e) {
			LOG.error(e.getMessage());
			throw new Exception(e);
		}

		return fileDTO;
	}

	private void processFormField(FileItemStream item, FileDTO fileDTO) throws IOException {
		String fieldName = item.getFieldName();
		String value = Streams.asString(item.openStream());

		LOG.debug("Processing form field '" + fieldName + "' with value '" + value + "'");

		if (Objects.nonNull(fieldName) && Objects.nonNull(value)) {

			switch (fieldName) {
			case "fileFormat":
				fileDTO.fileFormat = FileFormat.valueOfMimeType(value.trim());
				break;
			case "language":
				fileDTO.language = value.trim();
				break;
			case "description":
				fileDTO.description = value.trim();
				break;
			}
		}
	}

	private void processFileField(FileItemStream item, String fileStorageLocation, FileDTO fileDTO) throws IOException {
		String fieldName = item.getFieldName();
		String fileName = item.getName();
		String contentType = item.getContentType();

		boolean isContentTypeValid = isContentTypeValid(contentType);
		if (!isContentTypeValid) {
			throw new IllegalStateException("Unsupported Media Type!");
		}

		LOG.debug("Processing file field '" + fieldName + "' with file name '" + fileName + "' and content type '"
				+ contentType + "'");

		Path storageDirPath = Files.createTempDirectory(Paths.get(fileStorageLocation), "uploaded_");

		fileDTO.fileDir = String.valueOf(storageDirPath.getFileName());

		Path filePath = Paths.get(storageDirPath.toString(), fileName);

		long bytes = Files.copy(item.openStream(), Paths.get(storageDirPath.toString(), fileName));

		if (bytes > 0) {
			fileDTO.fileName = fileName;
			LOG.debug("File stored at: " + filePath.toString());
		}
	}

	private void validateSubmitForAnalysisFields(FileDTO fileDTO) {
		Objects.requireNonNull(fileDTO.fileName, "File name must be specified!");
		Objects.requireNonNull(fileDTO.fileDir, "File directory must be specified!");
		Objects.requireNonNull(fileDTO.fileFormat, "File format must be specified!");
		Objects.requireNonNull(fileDTO.language, "Language must be specified!");
	}

	private void cleanupOnFail(FileDTO fileDTO, String fileStorageLocation) {
		if (Objects.nonNull(fileDTO.fileDir)) {
			try {
				Path dirFullPath = Paths.get(fileStorageLocation, fileDTO.fileDir);
				if (Files.exists(dirFullPath) && Files.isDirectory(dirFullPath)) {

					try (DirectoryStream<Path> entries = Files.newDirectoryStream(dirFullPath)) {
						for (Path entry : entries) {
							Files.delete(entry);
						}
					}

					Files.delete(dirFullPath);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
	}

	private boolean isContentTypeValid(String contentType) {
		return FileFormat.hasMimeType(contentType);
	}
}
