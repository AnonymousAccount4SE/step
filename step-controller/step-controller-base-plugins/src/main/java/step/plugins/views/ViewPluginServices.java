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
package step.plugins.views;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Hidden;
import step.core.deployment.AbstractServices;

@Singleton
@Path("/views")
@Hidden
public class ViewPluginServices extends AbstractServices {

	private ViewManager viewManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		viewManager = getContext().get(ViewManager.class);
	}

	@GET
	@Path("/{id}/{executionId}")
	@Produces(MediaType.APPLICATION_JSON)
	public ViewModel getView(@PathParam("id") String viewId, @PathParam("executionId") String executionId) {
		return viewManager.queryView(viewId, executionId);
	}
}
