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
package step.core.objectenricher;

import java.util.ArrayList;
import java.util.stream.Collectors;

import step.core.AbstractContext;

@SuppressWarnings("serial")
public class ObjectHookRegistry extends ArrayList<ObjectHook> {

	/**
	 * @param context
	 * @return the composed {@link ObjectFilter} based on all the registered hooks
	 */
	public ObjectFilter getObjectFilter(AbstractContext context) {
		return ObjectFilterComposer
				.compose(stream().map(hook -> hook.getObjectFilter(context)).collect(Collectors.toList()));
	}

	/**
	 * @param context
	 * @return the composed {@link ObjectEnricher} based on all the registered hooks
	 */
	public ObjectEnricher getObjectEnricher(AbstractContext context) {
		return ObjectEnricherComposer
				.compose(stream().map(hook -> hook.getObjectEnricher(context)).collect(Collectors.toList()));
	}
	
	/**
	 * @param context
	 * @return the composed {@link ObjectEnricher} based on all the registered hooks
	 */
	public ObjectEnricher getObjectDrainer(AbstractContext context) {
		return ObjectEnricherComposer
				.compose(stream().map(hook -> hook.getObjectDrainer(context)).collect(Collectors.toList()));
	}

}
