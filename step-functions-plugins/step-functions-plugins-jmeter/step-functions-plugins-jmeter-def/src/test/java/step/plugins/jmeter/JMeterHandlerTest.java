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
package step.plugins.jmeter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.io.Output;
import step.functions.runner.FunctionRunner;
import step.functions.runner.FunctionRunner.Context;
import step.grid.bootstrap.ResourceExtractor;

public class JMeterHandlerTest {
	
	private static final Logger logger = LoggerFactory.getLogger(JMeterHandlerTest.class);
	
	@Test 
	public void test1() {
		File jmeterHomer = new File("../../distribution/template-controller/ext/jmeter");
		if(jmeterHomer.exists()) {
			JMeterFunction f = buildTestFunction();
			Output<JsonObject> output = run(f, "{}");
			Assert.assertNull(output.getError());
			Assert.assertNotNull(output.getPayload().get("samples"));
		} else {
			logger.warn("Skipping JMeterHandler test as no JMeter installation could been found");
		}
	}
	private Output<JsonObject> run(JMeterFunction f, String inputJson) {
		Configuration configuration = new Configuration();
		configuration.putProperty("plugins.jmeter.home", "../../distribution/template-controller/ext/jmeter");
		
		try (Context context = FunctionRunner.getContext(configuration,new JMeterFunctionType(configuration), new HashMap<>())) {
			return context.run(f, inputJson);			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private JMeterFunction buildTestFunction() {
		File file = ResourceExtractor.extractResource(this.getClass().getClassLoader(), "scripts/Demo_JMeter.jmx");
		JMeterFunction f = new JMeterFunction();
		
		f.setJmeterTestplan(new DynamicValue<String>(file.getAbsolutePath()));

		//f.setLibrariesFile(new DynamicValue<>());
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractOrganizableObject.NAME, "medor");
		f.setAttributes(attributes);

		//f.setScriptFile(new DynamicValue<String>(getScriptDir() + "/" + scriptFile));
		return f;
	}
}
