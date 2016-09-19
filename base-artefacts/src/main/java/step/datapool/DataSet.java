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
package step.datapool;

import java.util.concurrent.atomic.AtomicInteger;



public abstract class DataSet {
	
	protected AtomicInteger rowNum;
	
	public final synchronized void reset() {
		rowNum = new AtomicInteger();
		reset_();
	}
	
	public abstract void reset_();
	
	public final synchronized DataPoolRow next() {
		int currentRowNum = rowNum.incrementAndGet();
		Object nextValue = next_();
		return nextValue!=null?new DataPoolRow(currentRowNum,nextValue):null;
	}
	
	public abstract Object next_();
	
	public void save() {};
	
	public abstract void close();
	
}
