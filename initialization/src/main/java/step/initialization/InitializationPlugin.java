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
package step.initialization;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jongo.MongoCollection;
import org.json.JSONObject;

import step.artefacts.CallFunction;
import step.artefacts.Check;
import step.artefacts.ForEachBlock;
import step.artefacts.TestCase;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.ArtefactAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.plugins.adaptergrid.FunctionRepositoryImpl;

@Plugin
public class InitializationPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MongoCollection controllerLogs = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "controllerlogs");
		
		long runCounts = controllerLogs.count();
		
		if(runCounts==0) {
			// First start
			setupUsers(context);
			setupDemo(context);
			setupExecuteProcessFunction(context);
		}
		
		insertLogEntry(controllerLogs);
		
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("default");
		user.setPassword(UserAccessor.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}

	private void insertLogEntry(MongoCollection controllerLogs) {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		controllerLogs.insert(logEntry);
	}
	
	private void setupExecuteProcessFunction(GlobalContext context) {		
		Function executeProcessFunction = createFunction("ExecuteProcess", "class:step.handlers.processhandler.ProcessHandler");
		
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		functionRepository.addFunction(executeProcessFunction);
	}

	private void setupDemo(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		addFunction(functionRepository, "Demo_Echo");
		addFunction(functionRepository, "Javascript_HttpGet");
		addFunction(functionRepository, "Grinder_HttpGet", "classuri:../ext/lib/jython|classuri:../ext/lib/grinder|class:step.handlers.scripthandler.ScriptHandler");
		addFunction(functionRepository, "Demo_Java_Clock", "classuri:../data/scripts/java/src|class:step.script.AnnotatedMethodHandler");
		
		addFunction(functionRepository, "Selenium_StartChrome");
		addFunction(functionRepository, "Selenium_StartFirefox");
		addFunction(functionRepository, "Selenium_StartHTMLUnit");
		addFunction(functionRepository, "Selenium_Navigate");
		
		ArtefactAccessor artefacts = context.getArtefactAccessor();
		
		createDemoPlan(artefacts,"Demo_TestCase_Echo","Demo_Echo","{\"arg1\":\"val1\"}","output.getString(\"output1\")==\"val1\"");
		createDemoPlan(artefacts,"Demo_TestCase_Javascript_HttpGet","Javascript_HttpGet","{\"url\":\"http://www.denkbar.io\"}","output.getInt(\"statusCode\")==200");
		createDemoPlan(artefacts,"Demo_TestCase_Grinder_HttpGet","Grinder_HttpGet","{\"url\":\"http://www.denkbar.io\"}",null);
		createDemoPlan(artefacts,"Demo_TestCase_Java_Clock","Demo_Java_Clock","{ \"prettyString\" : \"Current time is : \" }",null);
		
		createDemoPlan(artefacts,"Demo_Testcase_ProcessExecution_Windows","ExecuteProcess","{\"cmd\":\"cmd.exe /r echo TEST\"}",null);
		createDemoPlan(artefacts,"Demo_Testcase_ProcessExecution_Linux","ExecuteProcess","{\"cmd\":\"echo TEST\"}",null);

		createDemoForEachPlan(artefacts, "Demo_Testcase_ForEach_CSV");
		
		createSeleniumDemoPlan(artefacts, "Firefox");
		createSeleniumDemoPlan(artefacts, "HTMLUnit");
	}

	private void createDemoForEachPlan(ArtefactAccessor artefacts, String planName)  {
		CallFunction call1 = createCallFunctionWithCheck(artefacts,"Javascript_HttpGet","{\"url\":\"[[dataPool.url]]\"}","output.getString(\"data\").contains(\"[[dataPool.check]]\")");
		
		ForEachBlock forEach = new ForEachBlock();
		forEach.setDataSource(new JSONObject().put("file", "../data/testdata/demo.csv"));
		forEach.addChild(call1.getId());
		artefacts.save(forEach);

		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", planName);
		testCase.setAttributes(tcAttributes);
		testCase.addChild(forEach.getId());
		artefacts.save(testCase);
	}
	
	private void createDemoPlan(ArtefactAccessor artefacts, String planName, String functionName, String args, String check) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", planName);
		testCase.setAttributes(tcAttributes);
		
		CallFunction call1 = createCallFunctionWithCheck(artefacts, functionName, args, check);
		
		testCase.addChild(call1.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}

	private CallFunction createCallFunctionWithCheck(ArtefactAccessor artefacts, String functionName, String args,
			String check) {
		CallFunction call1 = createCallFunction(functionName, args);

		if(check!=null) {
			Check check1 = new Check();
			check1.setExpression(check);
			artefacts.save(check1);
			call1.addChild(check1.getId());
		}
		
		artefacts.save(call1);
		return call1;
	}

	private CallFunction createCallFunction(String functionName, String args) {
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\""+functionName+"\"}");
		call1.setArgument(new DynamicValue<String>(args));
		call1.setToken("{\"route\":\"remote\"}");
		return call1;
	}
	
	private void createSeleniumDemoPlan(ArtefactAccessor artefacts, String browser) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", "Demo_Selenium_" + browser);
		testCase.setAttributes(tcAttributes);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\"Selenium_Start"+ browser +"\"}");
		call1.setArgument(new DynamicValue<String>("{}"));
		call1.setToken("{\"route\":\"remote\"}");
		artefacts.save(call1);
		
		CallFunction call2 = new CallFunction();
		call2.setFunction("{\"name\":\"Selenium_Navigate\"}");
		call2.setArgument(new DynamicValue<String>("{\"url\":\"http://denkbar.io\"}"));
		call2.setToken("{\"route\":\"remote\"}");
		artefacts.save(call2);
		
		testCase.addChild(call1.getId());
		testCase.addChild(call2.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}
	
	private void addFunction(FunctionRepositoryImpl functionRepository, String name) {
		addFunction(functionRepository, name, "class:step.handlers.scripthandler.ScriptHandler");
	}

	
	private void addFunction(FunctionRepositoryImpl functionRepository, String name, String handlerChain) {
		Function demoFunction = createFunction(name, handlerChain);
		functionRepository.addFunction(demoFunction);
	}

	private Function createFunction(String name, String handlerChain) {
		Function demoFunction = new Function();
		
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", name);
		
		demoFunction.setAttributes(kwAttributes);
		// TODO replace by specific types
		demoFunction.setType("custom");
		JSONObject conf = new JSONObject();
		conf.put("handlerChain", handlerChain);
		demoFunction.setConfiguration(conf);

		return demoFunction;
	}

	
}
