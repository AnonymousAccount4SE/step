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
package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.ForBlock;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class ForHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testSuccess() {
		setupContext();
		
		ExecutionContext.getCurrentContext().getVariablesManager().putVariable(
				ExecutionContext.getCurrentContext().getReport(), "var", "val1");
			
		ForBlock f = add(new ForBlock());
		f.setStart("1");
		f.setEnd("3");
		f.setInc("2");
		f.setItem("item");
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = addAsChildOf(new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				assertEquals(i.get(),(int)ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsInteger("item"));
				i.addAndGet(2);
			}
		}), f);
		
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals(2, child.getCount());
		assertEquals(0, child.getErrorCount());

		
		assertEquals(getChildren(child).size(), 2);
		
		for(ReportNode node:getChildren(child)) {
			assertEquals(node.getStatus(), ReportNodeStatus.PASSED);		
			assertEquals(getChildren(node).size(), 1);
			assertEquals(check1.getId(),getChildren(node).get(0).getArtefactID());
			
		}
		
	}
	
	@Test
	public void testBreak() {
		setupContext();
			
		ForBlock f = add(new ForBlock());
		f.setEnd("10");
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = addAsChildOf(new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				if(i.get()==2) {
					ExecutionContext.getCurrentContext().getVariablesManager().updateVariable("break", "true");
				}
				i.addAndGet(1);
			}
		}), f);
		
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals(2, child.getCount());
		assertEquals(0, child.getErrorCount());		
	}
	
	@Test
	public void testMaxFailedCount() {
		setupContext();
			
		ForBlock f = add(new ForBlock());
		f.setEnd("10");
		f.setMaxFailedLoops("2");
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = addAsChildOf(new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED);
			}
		}), f);
		
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals(2, child.getCount());
		assertEquals(2, child.getErrorCount());		
	}
}

