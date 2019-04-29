package software.into.ala.service.linguistics;

import org.osgi.annotation.versioning.ProviderType;

import software.into.ala.service.messaging.dto.FileMessageDTO;

@ProviderType
public interface LinguisticsService {

	void analyse(FileMessageDTO message);
}
