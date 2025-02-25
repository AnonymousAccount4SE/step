package step.controller.services.entities;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import step.controller.services.async.AsyncTask;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.entities.Entity;
import step.framework.server.Session;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableResponse;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.TableServiceException;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;

import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractEntityServices<T extends AbstractIdentifiableObject> extends AbstractStepServices {

    private final String entityName;
    private Accessor<T> accessor;
    private TableService tableService;
    /**
     * Associates {@link Session} to threads. This is used by requests that are executed
     * outside the Jetty scope like for {@link AsyncTaskManager}
     */
    private static final ThreadLocal<Session<User>> sessions = new ThreadLocal<>();
    private AsyncTaskManager asyncTaskManager;

    public AbstractEntityServices(String entityName) {
        this.entityName = entityName;
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        Entity<T, Accessor<T>> entityType = (Entity<T, Accessor<T>>) context.getEntityManager().getEntityByName(entityName);
        accessor = entityType.getAccessor();
        tableService = context.require(TableService.class);
        asyncTaskManager = context.require(AsyncTaskManager.class);
    }

    /**
     * Set the current {@link Session} for the current thread. This is useful for request that are processed
     * outside the Jetty scope like for {@link AsyncTaskManager}
     *
     * @param session the current {@link Session}
     */
    protected static void setCurrentSession(Session<User> session) {
        sessions.set(session);
    }

    @Override
    protected Session<User> getSession() {
        Session<User> userSession = sessions.get();
        if (userSession != null) {
            return userSession;
        } else {
            return super.getSession();
        }
    }

    @Operation(operationId = "get{Entity}ById", description = "Retrieves an entity by its Id")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public T get(@PathParam("id") String id) {
        return accessor.get(id);
    }

    @Operation(operationId = "find{Entity}sByAttributes", description = "Returns the list of entities matching the provided attributes")
    @POST
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<T> findManyByAttributes(Map<String, String> attributes) {
        Spliterator<T> manyByAttributes = accessor.findManyByAttributes(attributes);
        return StreamSupport.stream(manyByAttributes, false).collect(Collectors.toList());
    }

    @Operation(operationId = "delete{Entity}", description = "Deletes the entity with the given Id")
    @DELETE
    @Path("/{id}")
    @Secured(right = "{entity}-delete")
    public void delete(@PathParam("id") String id) {
        accessor.remove(new ObjectId(id));
    }

    @Operation(operationId = "clone{Entity}s", description = "Clones the entities according to the provided parameters")
    @POST
    @Path("/bulk/clone")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public AsyncTaskStatus<TableBulkOperationReport> cloneEntities(TableBulkOperationRequest request) {
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(entityName, request, this::clone, getSession()));
    }

    @Operation(operationId = "save{Entity}", description = "Saves the provided entity")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public T save(T entity) {
        entity = beforeSave(entity);
        return accessor.save(entity);
    }

    @Operation(operationId = "clone{Entity}", description = "Clones the entity with the given Id")
    @GET
    @Path("/{id}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public T clone(@PathParam("id") String id) {
        T entity = get(id);
        if (entity == null) {
            throw new ControllerServiceException("The entity with Id " + id + " doesn't exist");
        }
        T clonedEntity = cloneEntity(entity);

        if (clonedEntity instanceof AbstractOrganizableObject) {
            AbstractOrganizableObject organizableObject = (AbstractOrganizableObject) clonedEntity;
            // Append _Copy to new plan name
            String name = organizableObject.getAttribute(AbstractOrganizableObject.NAME);
            String newName = name + "_Copy";
            organizableObject.addAttribute(AbstractOrganizableObject.NAME, newName);
        }
        // Save the cloned plan
        save(clonedEntity);
        return clonedEntity;
    }

    protected T cloneEntity(T entity) {
        entity.setId(new ObjectId());
        return entity;
    }

    protected T beforeSave(T entity) {
        return entity;
    }

    @Operation(operationId = "delete{Entity}s", description = "Deletes the entities according to the provided parameters")
    @POST
    @Path("/bulk/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDelete(TableBulkOperationRequest request) {
        Consumer<String> consumer = t -> {
            try {
                delete(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(entityName, request, consumer, getSession()));
    }

    protected <R> AsyncTaskStatus<R> scheduleAsyncTaskWithinSessionContext(AsyncTask<R> asyncTask) {
        Session<User> session = getSession();
        return asyncTaskManager.scheduleAsyncTask(t -> {
            setCurrentSession(session);
            try {
                return asyncTask.apply(t);
            } finally {
                setCurrentSession(null);
            }
        });
    }

    @Operation(operationId = "get{Entity}Table", description = "Get the table view according to provided request")
    @POST
    @Path("/table")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public TableResponse<T> request(TableRequest request) throws TableServiceException {
        return tableService.request(entityName, request, getSession());
    }

    @Operation(operationId = "get{Entity}Versions", description = "Retrieves the versions of the entity with the given id")
    @GET
    @Path("/{id}/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<History> getVersions(@PathParam("id") String id) {
        return accessor.getHistory(new ObjectId(id), 0, 1000)
                .map(v->new History(v.getId().toHexString(), v.getUpdateTime()))
                .collect(Collectors.toList());
    }

    public static class History {
        public String id;
        public long updateTime;

        public History(String id, long updateTime) {
            this.id = id;
            this.updateTime = updateTime;
        }
    }

    @Operation(operationId = "restore{Entity}Version", description = "Restore a version of this entity")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/restore/{versionId}")
    public T restoreVersion(@PathParam("id") String id, @PathParam("versionId") String versionId) {
        return accessor.restoreVersion(new ObjectId(id), new ObjectId(versionId));
    }
}
