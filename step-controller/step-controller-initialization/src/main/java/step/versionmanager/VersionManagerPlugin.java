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
package step.versionmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin()
public class VersionManagerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(VersionManagerPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		VersionManager versionManager = new VersionManager(context);
		context.put(VersionManager.class, versionManager);
		
		versionManager.readLatestControllerLog();
		versionManager.insertControllerLog();
				
		super.executionControllerStart(context);
	}
}
