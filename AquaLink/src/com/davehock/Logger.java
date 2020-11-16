package com.davehock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;



public class Logger
{
	private static boolean logging;
	public static void setLogging(boolean logging)
	{
		Logger.logging=logging;
	}

   public void logInfo(String string)
   {
      if (logging)
      {
    	  DateFormat df = new SimpleDateFormat("yyyy-MM-dd- HH:mm:ss");
    	  System.out.println(df.format(new Date()) + " " + string);
      }
   }

}