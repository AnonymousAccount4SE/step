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
package step.functions.io;

import java.io.IOException;

import javax.json.JsonObject;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.functions.handler.FunctionInputOutputObjectMapperFactory;

public class OutputSerializationTest {

	@Test
	public void test() throws IOException {
		OutputBuilder builder = new OutputBuilder();
		builder.add("test", "test");
		Output<JsonObject> output = builder.build();
		ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
		String value = mapper.writeValueAsString(output);
		mapper.readValue(value, new TypeReference<Output<JsonObject>>() {});
	}
}
