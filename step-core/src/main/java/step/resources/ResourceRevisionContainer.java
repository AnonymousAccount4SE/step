package step.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class ResourceRevisionContainer {

	protected final Resource resource;
	protected final ResourceRevision resourceRevision;
	protected final OutputStream outputStream;
	private final ResourceManagerImpl resourceManagerImpl;

	protected ResourceRevisionContainer(Resource resource, ResourceRevision resourceRevision, OutputStream outputStream, ResourceManagerImpl resourceManagerImpl) {
		super();
		this.resource = resource;
		this.resourceRevision = resourceRevision;
		this.outputStream = outputStream;
		this.resourceManagerImpl = resourceManagerImpl;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}
	
	public Resource getResource() {
		return resource;
	}

	public ResourceRevision getResourceRevision() {
		return resourceRevision;
	}

	public void save(boolean checkForDuplicates, Map<String, String> additionalAttributes) throws IOException, SimilarResourceExistingException {
		try {
			outputStream.close();
		} catch (IOException e) {

		}
		resourceManagerImpl.closeResourceContainer(resource, resourceRevision, checkForDuplicates, additionalAttributes);
	}
	
	public void save(Map<String, String> additionalAttributes) throws IOException {
		try {
			save(false, additionalAttributes);
		} catch (SimilarResourceExistingException e) {
			throw new RuntimeException("This should never happen");
		}
	}

	public void save() throws IOException {
		save(null);
	}
}
