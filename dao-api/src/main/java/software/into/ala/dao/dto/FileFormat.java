package software.into.ala.dao.dto;

public enum FileFormat {
	flac("audio/flac"), mp3("audio/mp3"), ogg("audio/ogg");

	private String mime;

	private FileFormat(String mime) {
		this.mime = mime;
	}

	public String getMime() {
		return mime;
	}

	public static FileFormat valueOfMimeType(String mime) {
		for (FileFormat format : FileFormat.values()) {
			if ((format.getMime()).equalsIgnoreCase(mime)) {
				return format;
			}
		}
		return null;
	}

	public static boolean hasMimeType(String mime) {
		for (FileFormat format : FileFormat.values()) {
			if ((format.getMime()).equalsIgnoreCase(mime)) {
				return true;
			}
		}
		return false;
	}
}