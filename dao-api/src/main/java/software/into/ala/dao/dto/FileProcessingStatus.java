package software.into.ala.dao.dto;

public enum FileProcessingStatus {
	transcript_requested("transcription.transcript.requested", "Transcript was requested"),
	transcript_pending("transcription.transcript.pending", "Transcript is pending"),
	transcript_ready("transcription.transcript.ready", "Transcript is ready!"),
	transcript_failed("transcription.transcript.failed", "Transcript failed!"),
	analysis_requested("linguistics.analysis.requested", "Analysis was requested"),
	analysis_pending("linguistics.analysis.pending", "Analysis is pending"),
	analysis_ready("linguistics.analysis.ready", "Analysis is ready!"),
	analysis_failed("linguistics.analysis.failed", "Analysis failed!");

	private String key;
	private String label;

	private FileProcessingStatus(String key, String label) {
		this.key = key;
		this.label = label;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}
}