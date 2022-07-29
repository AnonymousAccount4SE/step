/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.interactive;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.artefacts.ArtefactQueue;
import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.artefacts.StreamingArtefact;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.core.encryption.EncryptionManager;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanNavigator;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.manager.FunctionManager;
import step.grid.client.AbstractGridClientImpl.AgentCommunicationException;
import step.parameter.ParameterManager;
import step.planbuilder.FunctionArtefacts;
import step.plugins.parametermanager.ParameterManagerPlugin;
import step.plugins.screentemplating.FunctionTableScreenInputs;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

@Singleton
@Path("interactive")
@Tag(name = "Interactive plan execution")
public class InteractiveServices extends AbstractStepServices {

	private final Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();
	private final Timer sessionExpirationTimer;
	private ExecutionEngine executionEngine;
	private final ExecutorService executorService;
	private PlanAccessor planAccessor;
	private ObjectPredicateFactory objectPredicateFactory;
	private FunctionTableScreenInputs functionTableScreenInputs;
	private FunctionManager functionManager;

	private static class InteractiveSession {
		
		long lasttouch;
		private final ArtefactQueue artefactQueue;
		private final Future<PlanRunnerResult> future;
		
		public InteractiveSession(ArtefactQueue artefactQueue, Future<PlanRunnerResult> future) {
			super();
			this.artefactQueue = artefactQueue;
			this.future = future;
			this.lasttouch = System.currentTimeMillis();
		}
		
		protected ArtefactQueue getArtefactQueue() {
			return artefactQueue;
		}
		
		protected Future<PlanRunnerResult> getFuture() {
			return future;
		}
	}
	
	public InteractiveServices() {
		super();
		
		executorService = Executors.newCachedThreadPool();
		
		sessionExpirationTimer = new Timer("Session expiration timer");
		sessionExpirationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				final int sessionTimeout = configuration.getPropertyAsInteger("ui.artefacteditor.interactive.sessiontimeout.minutes", 10)*60000;
				long time = System.currentTimeMillis();
				
				sessions.forEach((sessionId,session)->{
					if((session.lasttouch+sessionTimeout)<time) {
						try {
							closeSession(session);
						} catch (FunctionExecutionServiceException e) {
							
						}
						sessions.remove(sessionId);
					}
				});
			}
		}, 60000, 60000);
	}
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		planAccessor = context.getPlanAccessor();
		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		// the encryption manager might be null
		EncryptionManager encryptionManager = context.get(EncryptionManager.class);
		executionEngine = ExecutionEngine.builder().withOperationMode(OperationMode.CONTROLLER)
				.withParentContext(context).withPluginsFromClasspath().withPlugin(new AbstractExecutionEnginePlugin() {

					@Override
					public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext,
							ExecutionEngineContext executionEngineContext) {
						executionEngineContext.setExecutionAccessor(new InMemoryExecutionAccessor());
					}
				}).withPlugin(new ParameterManagerPlugin(context.get(ParameterManager.class), encryptionManager)).withObjectHookRegistry(objectHookRegistry).build();
		objectPredicateFactory = context.require(ObjectPredicateFactory.class);
		functionTableScreenInputs = getContext().require(FunctionTableScreenInputs.class);
		functionManager = getContext().get(FunctionManager.class);
	}
	
	@PreDestroy
	public void close() {
		if(sessionExpirationTimer != null) {
			sessionExpirationTimer.cancel();
		}
		executorService.shutdown();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/start")
	@Secured(right="interactive")
	public String start(ExecutionParameters executionParameters) throws AgentCommunicationException {
		StreamingArtefact streamingArtefact = new StreamingArtefact();
		Plan plan = PlanBuilder.create()
						.startBlock(FunctionArtefacts.session())
							.add(streamingArtefact)
						.endBlock()
					.build();
		
		executionParameters.setPlan(plan);
		String executionId = executionEngine.initializeExecution(executionParameters);
		Future<PlanRunnerResult> future = executorService.submit(()->executionEngine.execute(executionId));
		InteractiveSession session = new InteractiveSession(streamingArtefact.getQueue(), future);
		
		// the session id has to match the execution id as it used in the client to find the report nodes
		sessions.put(executionId, session);
		return executionId;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stop")
	@Secured(right="interactive")
	public void stop(@PathParam("id") String sessionId) throws FunctionExecutionServiceException, InterruptedException, ExecutionException {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			closeSession(session);		
			session.getFuture().get();
		}
	}

	private void closeSession(InteractiveSession session) throws FunctionExecutionServiceException {
		session.getArtefactQueue().stop();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}/execute/{planid}/{artefactid}")
	@Secured(right="interactive")
	public ReportNode executeArtefact(@PathParam("id") String sessionId, @PathParam("planid") String planId, @PathParam("artefactid") String artefactId, @Context ContainerRequestContext crc) throws InterruptedException, ExecutionException {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			AbstractArtefact artefact = findArtefactInPlan(planId, artefactId);
			Future<ReportNode> future = session.getArtefactQueue().add(artefact);
			return future.get();
		} else {
			 throw new RuntimeException("Session doesn't exist or expired.");
		}
	}

	protected AbstractArtefact findArtefactInPlan(String planId, String artefactId) {
		Plan plan = planAccessor.get(planId);
		return new PlanNavigator(plan).findArtefactById(artefactId);
	}
	
	public static class FunctionTestingSession {
		
		private String planId;
		private String callFunctionId;

		public FunctionTestingSession() {
			super();
		}

		public String getPlanId() {
			return planId;
		}

		public void setPlanId(String planId) {
			this.planId = planId;
		}

		public String getCallFunctionId() {
			return callFunctionId;
		}

		public void setCallFunctionId(String callFunctionId) {
			this.callFunctionId = callFunctionId;
		}

	}
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/functiontest/{keywordid}/start")
	@Secured(right="interactive")
	public FunctionTestingSession startFunctionTestingSession(@PathParam("keywordid") String keywordid) {
		Function function = functionManager.getFunctionById(keywordid);

		ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
		Map<String, String> functionAttributes = functionTableScreenInputs.getSelectionAttributes(function, null, objectPredicate);

		CallFunction callFunction = FunctionArtefacts.keyword(functionAttributes).withInput("{}").build();

		// TODO do this centrally. Currently the same logic is implemented in the UI
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", function.getAttributes().get(AbstractOrganizableObject.NAME));
		callFunction.setAttributes(attributes);
		FunctionGroup functionGroup = new FunctionGroup();
		attributes = new HashMap<>();
		attributes.put("name", "Session");
		functionGroup.setAttributes(attributes);
		
		Plan plan = PlanBuilder.create()
				.startBlock(functionGroup)
				.add(callFunction)
				.endBlock()
				.build();
		plan.setVisible(false);
		
		getObjectEnricher().accept(plan);
		getContext().getPlanAccessor().save(plan);
		FunctionTestingSession result = new FunctionTestingSession();
		result.setPlanId(plan.getId().toString());
		result.setCallFunctionId(callFunction.getId().toString());
		return result;
	}

	private InteractiveSession getAndTouchSession(String sessionId) {
		InteractiveSession session = sessions.get(sessionId);
		if(session!=null) {
			session.lasttouch = System.currentTimeMillis();			
		}
		return session;
	}
}
