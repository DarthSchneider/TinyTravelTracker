/** 
    Copyright 2015 Tim Engler, Rareventure LLC

    This file is part of Tiny Travel Tracker.

    Tiny Travel Tracker is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Tiny Travel Tracker is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Tiny Travel Tracker.  If not, see <http://www.gnu.org/licenses/>.

*/
/**
 * 
 */
package com.rareventure.android;

import java.util.ArrayList;

import org.acra.ErrorReporter;

import android.content.Context;

import com.rareventure.gps2.GTG;
import com.rareventure.gps2.GTG.GTGEvent;

public class ProcessThread extends WorkerThread
{

	private DataReader[] dataReaders;
	private String tag;
	private Context context;
	
	public ProcessThread(Context context, String tag, 
			DataReader ... dataReaders)
    {
		super();
		this.context = context;
		this.tag = tag;
		this.dataReaders = dataReaders;
		
		for(DataReader dr : dataReaders)
		{
			dr.setProcessThread(this);
		}
    }
    
    public void run()
    {
		try {
	    	ArrayList<DataReader> itemsToProcess = new ArrayList<DataReader>();
	    	
	    	while(true) {
	
	        	
	        	itemsToProcess.clear();
	        	
	        	synchronized(lock)
	        	{
	        		//wait for data
	        		while(!isShutdownRequested)
	        		{
	        			
	        			for(DataReader rt : dataReaders)
	        			{
	                		if(rt.canProcess())
	                		{
	                			itemsToProcess.add(rt);
	                		}
	        			}
	            		
	        			if(itemsToProcess.isEmpty())
	        			{
		        			try {
								lock.wait();
							} catch (InterruptedException e) {
								throw new IllegalStateException(e); //punt
							}
	        			}
	        			else
	        				break;
	        		} //while waiting for data
	        		
	        	} //sync lock
	        	
	        	if(isShutdownRequested)
	        		break;
	        	
	        	for(DataReader dr : itemsToProcess)
	        	{
	        		dr.process();
	        	}
	        	
	        } //end while running
	    	
	    	for(DataReader dr : dataReaders)
	    	{
	    		dr.notifyShutdown();
	    	}
	
	        synchronized(lock)
	        {
	            isShutdown = true;
	        	lock.notify();
	        }
	        

		}
		catch(Exception e)
		{
			e.printStackTrace();
			GTG.alert(GTGEvent.ERROR_SERVICE_INTERNAL_ERROR);
			ErrorReporter.getInstance().handleException(e);

			synchronized(lock)
	        {
	            isShutdown = true;
	        	lock.notify();
	        }
	        
			return;
		}
    } // void run()

}
