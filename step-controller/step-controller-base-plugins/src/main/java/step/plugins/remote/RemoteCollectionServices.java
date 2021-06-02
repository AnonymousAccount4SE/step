package step.plugins.remote;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.type.CollectionType;

import step.client.collections.remote.CountRequest;
import step.client.collections.remote.CountResponse;
import step.client.collections.remote.FindRequest;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filter;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Unfiltered;
import step.core.entities.EntityManager;

@Singleton
@Path("remote")
public class RemoteCollectionServices<T> extends AbstractServices {

    protected CollectionFactory collectionFactory;
    protected EntityManager entityManager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        entityManager = getContext().getEntityManager();
        collectionFactory = getContext().getCollectionFactory();
    }

    @PreDestroy
    public void destroy() {
    }
    
	@POST
	@Path("/{id}/count")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right = "collection-read")
	public Response count(@PathParam("id") String collectionId, CountRequest countRequest) {
		@SuppressWarnings("unchecked")
		Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId,
				entityManager.resolveClass(collectionId));
		long count = collectionDriver.count(countRequest.getFilter(), countRequest.getLimit());
		return Response.status(200).entity(new CountResponse(count)).build();
	}
	
	@GET
	@Path("/{id}/count/estimated")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "collection-read")
	public Response count(@PathParam("id") String collectionId) {
		@SuppressWarnings("unchecked")
		Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId,
				entityManager.resolveClass(collectionId));
		long count = collectionDriver.estimatedCount();
		return Response.status(200).entity(new CountResponse(count)).build();
	}

    @POST
    @Path("/{id}/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="collection-read")
    @Unfiltered
    public  Response find(@PathParam("id") String collectionId, FindRequest findRequest) {
        Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId, entityManager.resolveClass(collectionId));
        List<T> collect = collectionDriver.find(findRequest.getFilter(), findRequest.getOrder(), findRequest.getSkip(),
                findRequest.getLimit(), findRequest.getMaxTime()).collect(Collectors.toList());
        Class<?> entityClass = getContext().getEntityManager().resolveClass(collectionId);
        ParameterizedType parameterizedGenericType = new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[] { entityClass };
            }

            public Type getRawType() {
                return List.class;
            }

            public Type getOwnerType() {
                return List.class;
            }
        };

        GenericType<List<T>> genericType = new GenericType<List<T>>(
                parameterizedGenericType) {
        };

        GenericEntity<List<T>> genericEntity = new GenericEntity<>(collect,genericType.getType());
        return Response.status(200).entity(genericEntity).build();
    }

    @POST
    @Path("/{id}/distinct/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="collection-read")
    public  List<String> distinctPost(@PathParam("id") String collectionId, @PathParam("id") String columnName, Filter filter) {
        Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId, entityManager.resolveClass(collectionId));
        return collectionDriver.distinct(columnName,filter);
    }

    @POST
    @Path("/{id}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="collection-delete")
    public void delete(@PathParam("id") String collectionId, Filter filter) {
        Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId, entityManager.resolveClass(collectionId));
        collectionDriver.remove(filter);
    }

    @POST
    @Path("/{id}/save")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="collection-write")
    public Response save(@PathParam("id") String collectionId, T entity) {
        Class<?> entityClass = entityManager.resolveClass(collectionId);
        Object value = DefaultJacksonMapperProvider.getObjectMapper().convertValue(entity, entityClass);
        Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId, entityClass);
        //method cannot return a generic (T) due to serialization
        return  Response.status(200).entity(collectionDriver.save((T) value)).build();
    }

    @POST
    @Path("/{id}/saveMany")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="collection-write")
    public  void saveBulk(@PathParam("id") String collectionId, List<T> entities) {
        Collection<T> collectionDriver = (Collection<T>) collectionFactory.getCollection(collectionId, entityManager.resolveClass(collectionId));
        Class<?> entityClass = entityManager.resolveClass(collectionId);
        CollectionType collectionType = DefaultJacksonMapperProvider.getObjectMapper().getTypeFactory().constructCollectionType(List.class, entityClass);
        Object o = DefaultJacksonMapperProvider.getObjectMapper().convertValue(entities,collectionType);
        collectionDriver.save((List) o);
    }





}
