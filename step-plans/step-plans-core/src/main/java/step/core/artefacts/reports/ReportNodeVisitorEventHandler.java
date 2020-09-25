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
package step.core.artefacts.reports;

import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

/**
 *  A support class for {@link ReportTreeVisitor} which listens for report node events.
 *
 */
public interface ReportNodeVisitorEventHandler {
	
	/**
	 * This method is call by the {@link ReportTreeVisitor} when entering a report node
	 * 
	 * @param reportNodeEvent the {@link ReportNodeEvent} corresponding to the entered node
	 */
	public void startReportNode(ReportNodeEvent reportNodeEvent);
	
	/**
	 * This method is call by the {@link ReportTreeVisitor} when exiting a report node
	 * 
	 * @param reportNodeEvent the {@link ReportNodeEvent} corresponding to the exited node
	 */
	public void endReportNode(ReportNodeEvent reportNodeEvent);

}
