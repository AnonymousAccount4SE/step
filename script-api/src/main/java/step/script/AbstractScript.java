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
package step.script;

import java.util.Map;

import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.Attachment;
import step.grid.io.InputMessage;

public class AbstractScript  {
	
	protected AgentTokenWrapper token;
	
	protected InputMessage message;

	protected OutputMessageBuilder outputBuilder = new OutputMessageBuilder();
	
	public void addAttachment(Attachment attachment) {
		outputBuilder.addAttachment(attachment);
	}

	public OutputMessageBuilder add(String arg0, boolean arg1) {
		return outputBuilder.add(arg0, arg1);
	}

	public OutputMessageBuilder add(String arg0, long arg1) {
		return outputBuilder.add(arg0, arg1);
	}

	public OutputMessageBuilder add(String arg0, String arg1) {
		return outputBuilder.add(arg0, arg1);
	}

	public OutputMessageBuilder setError(String technicalError) {
		return outputBuilder.setError(technicalError);
	}

	public OutputMessageBuilder setError(String errorMessage, Throwable e) {
		return outputBuilder.setError(errorMessage, e);
	}

	public void startMeasure(String id) {
		outputBuilder.startMeasure(id);
	}

	public void startMeasure(String id, long begin) {
		outputBuilder.startMeasure(id, begin);
	}

	public void stopMeasure(long end, Map<String, String> data) {
		outputBuilder.stopMeasure(end, data);
	}

	public void stopMeasure() {
		outputBuilder.stopMeasure();
	}

	public void stopMeasure(Map<String, String> data) {
		outputBuilder.stopMeasure(data);
	}
	
	public void beforeCall(AgentTokenWrapper token, InputMessage message) {
		this.token = token;
		this.message = message;
	};
	
	public boolean onError(AgentTokenWrapper token, InputMessage message, Exception e) {
		return false;
	};
	
	public void afterCall(AgentTokenWrapper token, InputMessage message) {};
	
}
