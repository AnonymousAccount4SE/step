/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.functions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentManager;
import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.TokenWrapper;
import step.grid.client.GridClient;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionClient implements FunctionExecutionService, FunctionTypeRegistry {

	private final GridClient gridClient;
	
	private final AttachmentManager attachmentManager;
	
	private final FunctionRepository functionRepository;
	
	private final Map<String, AbstractFunctionType<Function>> functionTypes = new HashMap<>();
	
	private final DynamicBeanResolver dynamicBeanResolver;
	private final Configuration configuration;
	
	private static final Logger logger = LoggerFactory.getLogger(FunctionClient.class);
	
	public FunctionClient(AttachmentManager attachmentManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver, GridClient gridClient, FunctionRepository functionRepository) {
		super();
		this.attachmentManager = attachmentManager;
		this.configuration = configuration;
		this.dynamicBeanResolver = dynamicBeanResolver;
		this.gridClient = gridClient;
		this.functionRepository = functionRepository;
	}
	
	@Override
	public TokenWrapper getLocalTokenHandle() {
		return gridClient.getLocalTokenHandle();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws AgentCommunicationException {
		return gridClient.getTokenHandle(attributes, interests, createSession);
	}

	@Override
	public void returnTokenHandle(TokenWrapper adapterToken) throws AgentCommunicationException {
		adapterToken.setCurrentOwner(null);
		gridClient.returnTokenHandle(adapterToken);
	}
	
	@Override
	public Output callFunction(TokenWrapper tokenHandle, Map<String,String> functionAttributes, Input input) {	
		Function function = functionRepository.getFunctionByAttributes(functionAttributes);
		return callFunction(tokenHandle, function.getId().toString(), input);
	}
	
	@Override
	public Output callFunction(TokenWrapper tokenHandle, String functionId, Input input) {	
		Function function = functionRepository.getFunctionById(functionId);
		
		Output output = new Output();
		output.setFunction(function);
		try {
			AbstractFunctionType<Function> functionType = getFunctionTypeByFunction(function);
			dynamicBeanResolver.evaluate(function, Collections.<String, Object>unmodifiableMap(input.getProperties()));
			
			String handlerChain = functionType.getHandlerChain(function);
			FileVersionId handlerPackage = functionType.getHandlerPackage(function);
			
			Map<String, String> properties = new HashMap<>();
			properties.putAll(input.getProperties());
			Map<String, String> handlerProperties = functionType.getHandlerProperties(function);
			if(handlerProperties!=null) {
				properties.putAll(handlerProperties);				
			}
			
			functionType.beforeFunctionCall(function, input, properties);
			
			int callTimeout = function.getCallTimeout().get();
			OutputMessage outputMessage = gridClient.call(tokenHandle, function.getAttributes().get(Function.NAME), input.getArgument(), handlerChain, handlerPackage, properties, callTimeout);
			
			output.setResult(outputMessage.getPayload());
			output.setError(outputMessage.getError());
			output.setAttachments(outputMessage.getAttachments());
			output.setMeasures(outputMessage.getMeasures());
			return output;
		} catch (Exception e) {
			if(logger.isDebugEnabled()) {
				logger.error("Error while calling function with id "+functionId, e);
			}
			attachExceptionToOutput(output, e);
		}
		return output;
	}

	public static void attachExceptionToOutput(Output output, Exception e) {
		output.setError("Unexpected error while calling function: " + e.getClass().getName() + " " + e.getMessage());
		Attachment attachment = AttachmentHelper.generateAttachmentForException(e);
		List<Attachment> attachments = output.getAttachments();
		if(attachments==null) {
			attachments = new ArrayList<>();
			output.setAttachments(attachments);
		}
		attachments.add(attachment);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
		functionType.setConfiguration(configuration);
		functionType.setAttachmentManager(attachmentManager);
		functionType.setFunctionClient(this);
		functionType.init();
		functionTypes.put(functionType.newFunction().getClass().getName(), (AbstractFunctionType<Function>) functionType);
	}
	
	private AbstractFunctionType<Function> getFunctionTypeByType(String functionType) {
		AbstractFunctionType<Function> type = (AbstractFunctionType<Function>) functionTypes.get(functionType);
		if(type==null) {
			throw new RuntimeException("Unknown function type '"+functionType+"'");
		} else {
			return type;
		}
	}
	
	@Override
	public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
		return getFunctionTypeByType(function.getClass().getName());
	}
	
	public void setupFunction(Function function) throws SetupFunctionException {
		AbstractFunctionType<Function> type = getFunctionTypeByFunction(function);
		type.setupFunction(function);
	}
	
	public Function copyFunction(Function function) throws FunctionTypeException {
		AbstractFunctionType<Function> type = getFunctionTypeByFunction(function);
		return type.copyFunction(function);
	}
	
	public Function updateFunction(Function function) throws FunctionTypeException {
		AbstractFunctionType<Function> type = getFunctionTypeByFunction(function);
		return type.updateFunction(function);
	}
	
	public void deleteFunction(Function function) throws FunctionTypeException {
		AbstractFunctionType<Function> type = getFunctionTypeByFunction(function);
		type.deleteFunction(function);
	}
	
	public String registerAgentFile(File file) {
		return gridClient.registerFile(file);
	}

	public FunctionRepository getFunctionRepository() {
		return functionRepository;
	}
	
	public Function newFunction(String type) {
		return getFunctionTypeByType(type).newFunction();
	}
	
}
