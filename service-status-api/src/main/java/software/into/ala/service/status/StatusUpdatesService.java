package software.into.ala.service.status;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.pushstream.PushStream;

@ProviderType
public interface StatusUpdatesService {

	boolean hasStatusUpdates(String fileId);

	PushStream<String> getStatusUpdates(String fileId);
}
