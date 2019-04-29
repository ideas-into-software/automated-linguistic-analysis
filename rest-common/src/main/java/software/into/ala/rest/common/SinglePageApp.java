package software.into.ala.rest.common;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;

@Component(service = SinglePageApp.class, immediate = true)
@HttpWhiteboardResource(pattern = "/spa/*", prefix = "static")
public class SinglePageApp {
}
