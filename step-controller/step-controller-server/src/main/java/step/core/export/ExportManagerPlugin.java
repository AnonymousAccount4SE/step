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
package step.core.export;

import step.core.GlobalContext;
import step.core.imports.ImportServices;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ResourceManagerControllerPlugin.class})
public class ExportManagerPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		ResourceManager resourceManager = context.getResourceManager();
		ExportTaskManager exportTaskManager = new ExportTaskManager(resourceManager);
		context.put(ExportTaskManager.class, exportTaskManager);
		
		context.getServiceRegistrationCallback().registerService(ExportServices.class);
		context.getServiceRegistrationCallback().registerService(ImportServices.class);
		
		super.serverStart(context);
	}

}
