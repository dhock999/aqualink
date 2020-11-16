package com.davehock;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import com.davehock.AquaLinkManager.EQUIPMENT;
import com.davehock.AquaLinkManager.EQUIPMENTSTATUS;

public class ScreenManager
{
	private static final byte MSGTYPE_DAYTIME = 64;
	public static final byte MSGTYPE_TEMPERATURE = 26;
	private static final byte MSGTYPE_TEMPERATURE2 = -126;
	private Logger logger=new Logger();

	public enum SCREENS {
		   SCREEN_UNKNOWN,
		   SCREEN_STARTUP,
		   SCREEN_PENDING,
		   SCREEN_EQUIPMENTSTATUS,
		   SCREEN_HOME,
		   SCREEN_EQUIPMENTONOFF,
		   SCREEN_EQUIPMENTONOFF_SPAHEAT,
		   SCREEN_EQUIPMENTONOFF_POOLHEAT,
		    
		   SCREEN_MAINMENU,
		   SCREEN_MAINMENU_HELP,
		   SCREEN_MAINMENU_HELP_SERVICE,
		   SCREEN_MAINMENU_HELP_DIAGNOSTICS,
		   SCREEN_MAINMENU_HELP_DIAGNOSTICS_SENSORS,
		   SCREEN_MAINMENU_HELP_DIAGNOSTICS_REMOTES,
		   SCREEN_MAINMENU_PROGRAM,
		   //all program menus
		   SCREEN_MAINMENU_SETTEMP,
		   SCREEN_MAINMENU_SETTIME,
		   SCREEN_MAINMENU_SETAQUAPURE,
		   SCREEN_MAINMENU_PDAOPTIONS,
		   SCREEN_MAINMENU_SYSTEMSETUP,
		   SCREEN_MAINMENU_BOOSTPOOL,
		}

	private SCREENS currentScreen = SCREENS.SCREEN_UNKNOWN;
	private SCREENS lastScreen = SCREENS.SCREEN_UNKNOWN;
	//SCREEN_HOME menu items 
	public static final long MENUSTART_HOME = 4;
	public static final long MENUEND_HOME = 9;
	public static final long MENUITEM_MAINMENU = 8;
	public static final long MENUITEM_EQUIPMENTONOFF = 9;

	//SCREEN_MAINMENU menu items (when boost pool available
	public static final long MENUSTART_MAINMENU = 1;
	public static final long MENUEND_MAINMENU = 9; //only 9 if pool running with boost pool, ot
	public static final long MENUITEM_MAINMENU_PROGRAM = 2;
	public static final long MENUITEM_MAINMENU_SETTEMP = 3;
	public static final long MENUITEM_MAINMENU_SETTIME = 4;
	public static final long MENUITEM_MAINMENU_SETAQUAPURE = 5;
	public static final long MENUITEM_MAINMENU_BOOSTPOOL = 9;
	//if boost not available, start at 2 and shift all down 1
	public boolean aquaPureItemsAvailable=false; //if pool is running, this menu changes and item locations shift 
	private int EQUIPMENTTIMEOUTTIME = 3*60; //after this much time without a status screen, assume all off

	//SCREEN_EQUIPMENTONOFF
	public static final long MENUSTART_EQUIPMENTONOFF = 1;
	public static final long MENUEND_EQUIPMENTONOFF = 9;
	public static final long MENUITEM_EQUIPMENTONOFF_ALLOFF = 9;
	private KeyManager keyManager;
	private String currentDayTime = "";
	//attributes 
	private String currentAirTemperature = "";
	private String currentWaterTemperature = "";
	private long lastEquipmentStatus;

	private Integer pendingSpaHeaterSetPoint;
	private Integer pendingPoolHeaterSetPoint;
	private Integer pendingPoolAquaPureSetPoint;
	private Integer pendingSpaAquaPureSetPoint;
	private Calendar pendingDateTime = Calendar.getInstance();
	private boolean pdaReady = false;

	private String aquapureError="";
	private String miscStatus=""; //Place holder for aquapure and other messages

	
//	private int freezeProtectSetPoint;
	private Integer poolHeaterSetPoint;
	private Integer spaHeaterSetPoint;

	private Integer poolAquaPureSetPoint;
	private Integer spaAquaPureSetPoint;
	private static final String SETPOINTPREFIX = "  SET TO ";
	private static final String POOLHEATSETPOINTPREFIX = "POOL HEAT ";
	private static final String SPAHEATSETPOINTPREFIX = "SPA HEAT ";
	private static final String POOLAQUAPURESETPOINTPREFIX = "SET POOL TO:";
	private static final String SPAAQUAPURESETPOINTPREFIX = " SET SPA TO:";
	private EQUIPMENTSTATUS equipmentStatus[] = new EQUIPMENTSTATUS[EQUIPMENT_LABELS.length];
	private Queue<AquaLinkCommand> commandQueue;
	private String screenDisplay[]= new String[11];
	private final int SCREENWIDTH = 16;

	private static String[] EQUIPMENT_LABELS = {
	   "FILTER PUMP",
	   "SPA",
	   "POOL HEAT",
	   "SPA HEAT",
	   "JET PUMP",
	   "LOW SPEED",
	   "SPILLOVER",
	   "EXTRA AUX",
	};


	public ScreenManager()
	{
		commandQueue=new LinkedList<AquaLinkCommand>();
		this.keyManager=new KeyManager(this);
	      for(int i=0; i< equipmentStatus.length;i++)
		         equipmentStatus[i]=EQUIPMENTSTATUS.OFF;

	}
	public void processMsgLine(int lineNum, String msg)   
	{
	      if (lineNum==MSGTYPE_DAYTIME)
	      {
	         currentScreen=SCREENS.SCREEN_HOME;//time always on home screen
	         currentDayTime=msg.trim();
	         this.setScreenLine(0,msg);
	         if (AquaLinkManager.now()-this.lastEquipmentStatus > 1000*EQUIPMENTTIMEOUTTIME) this.clearEquipmentStatus(); //if no status screen for 5 minutes, assume Program stopped running and clear all equipment
	         logger.logInfo("DAYTIME:"+ currentDayTime);
	         dumpStatus();
	      }
	      else if (lineNum==MSGTYPE_TEMPERATURE || lineNum==MSGTYPE_TEMPERATURE2)
	      {
		     // 72`     79`
			 //0123456789012345
	         currentScreen=SCREENS.SCREEN_HOME;//temp always on home screen
	         logger.logInfo("TEMPERATURE:" + msg);
	         String msg2 = msg.replace("`", " ").trim();
	         this.setScreenLine(3,msg);
	         
	         String tmp = msg2;
	         if (tmp.length()>3) tmp = tmp.substring(0, 3).trim();
	         currentAirTemperature=tmp;
	         if (msg2.length()>3)
	         {
	            tmp=msg2.substring(3).trim(); 
	            if (tmp.length()>0) //Remember water temp from last time pump running
	               currentWaterTemperature=tmp;
	         }
	         dumpStatus();
	      }
	      else
	      {         
	         this.setScreenLine(lineNum,msg);

	         String equipmentName = msg.substring(0, msg.length()-3).trim(); //trim off last 3
	         String eStatus = msg.substring(msg.length()-3).trim(); //trim all but last 3
	         EQUIPMENTSTATUS currentStatus = null;
	         if (eStatus.equals("***"))
	            currentStatus=EQUIPMENTSTATUS.PENDING;
	         else if (eStatus.equals("ENA"))
	            currentStatus=EQUIPMENTSTATUS.ON;
	         else if (eStatus.equals(EQUIPMENTSTATUS.OFF.name()) || eStatus.equals(EQUIPMENTSTATUS.ON.name()))
	            currentStatus=EQUIPMENTSTATUS.valueOf(eStatus);
	         
	         if (lineNum==5) //could be a 1 or 2 button press
	            parseEquipmentOnOff(equipmentName, currentStatus);

	         
	         switch (currentScreen)
	         {
	         case SCREEN_HOME:
	          EQUIPMENT equipment=null;
	          
	          if(equipmentName.equals("POOL MODE"))
	             equipment = EQUIPMENT.FILTERPUMP;        
	          else if(equipmentName.equals("POOL HEATER"))
	             equipment = EQUIPMENT.POOLHEAT;
	          else if(equipmentName.equals("SPA MODE"))
	             equipment = EQUIPMENT.SPA;
	          else if(equipmentName.equals("SPA HEATER"))
	             equipment = EQUIPMENT.SPAHEAT;
	          
	          if (equipment!=null && equipmentStatus[equipment.ordinal()]!=currentStatus)
	          {
	             equipmentStatus[equipment.ordinal()]=currentStatus;
//	             this.miscStatus="";
	             dumpStatus();
	          }
	                     
	            break;
	         case SCREEN_EQUIPMENTONOFF:
	            //Check for recent status change due to use action
	            //Parse Equipment Status
	            
	            parseEquipmentOnOff(equipmentName, currentStatus);           
	            break;
	         case SCREEN_EQUIPMENTONOFF_SPAHEAT:
	             //THIS  CMD_MSG_LONG 3 "  SET TO 100`F *0  *1 "
	             int degreeLocation = msg.indexOf("`");
	             if (lineNum==3 && msg.startsWith(SETPOINTPREFIX) && degreeLocation!=-1)
	             {
	                this.spaHeaterSetPoint = Integer.parseInt(msg.substring(SETPOINTPREFIX.length(),degreeLocation).trim());
	             }
	        	 break;
	         case SCREEN_EQUIPMENTONOFF_POOLHEAT:
	             //THIS  CMD_MSG_LONG 3 "  SET TO 100`F *0  *1 "
	             degreeLocation = msg.indexOf("`");
	             if (lineNum==3 && msg.startsWith(SETPOINTPREFIX) && degreeLocation!=-1)
	             {
	                this.poolHeaterSetPoint = Integer.parseInt(msg.substring(SETPOINTPREFIX.length(),degreeLocation).trim());
	             }
	        	 break;
	         case SCREEN_MAINMENU:
	        	 if (msg.equals("HELP           >"))
	        	 {
	        		 //If pump is on help is on line 1 otherwise on line 2
	        		 if (lineNum==1) 
	        			 aquaPureItemsAvailable=true;
	        		 else
	        			 aquaPureItemsAvailable=false;
	        	 }
	        	 break;
	         case SCREEN_MAINMENU_SETTEMP:
	             //THIS  CMD_MSG_LONG 3 "  SET TO 100`F *0  *1 "
	             degreeLocation = msg.indexOf("`");
	             if (degreeLocation!=-1)
	             {
	                 if (lineNum==1 && msg.startsWith(POOLHEATSETPOINTPREFIX))
	                 {
	                	Integer temp = Integer.parseInt(msg.substring(POOLHEATSETPOINTPREFIX.length(),degreeLocation).trim());
	            		this.pendingPoolHeaterSetPoint = temp;
	                 }
	                 else if (lineNum==2 && msg.startsWith(SPAHEATSETPOINTPREFIX))
	                 {
	                 	Integer temp = Integer.parseInt(msg.substring(SPAHEATSETPOINTPREFIX.length(),degreeLocation).trim());
	        logger.logInfo("Parsed Spa Temp:"+ temp.toString());
	             		this.pendingSpaHeaterSetPoint = temp;
	                 }
	             }
	        	 break;
	         case SCREEN_MAINMENU_SETAQUAPURE:
	             //JDA  CMD_MSG_LONG 3 "SET POOL TO:100%"
	        	 //JDA  CMD_MSG_LONG 4 " SET SPA TO:100%"

	             int percentLocation = msg.indexOf("%");
	             if (percentLocation!=-1)
	             {
	                 if (lineNum==3 && msg.startsWith(POOLAQUAPURESETPOINTPREFIX))
	                 {
	                	Integer temp = Integer.parseInt(msg.substring(POOLAQUAPURESETPOINTPREFIX.length(),percentLocation).trim());
	        	        logger.logInfo("Parsed Pool Aquapure:"+ temp.toString());
	            		this.pendingPoolAquaPureSetPoint = temp;
	                 }
	                 else if (lineNum==4 && msg.startsWith(SPAAQUAPURESETPOINTPREFIX))
	                 {
	                 	Integer temp = Integer.parseInt(msg.substring(SPAAQUAPURESETPOINTPREFIX.length(),percentLocation).trim());
	             		this.pendingSpaAquaPureSetPoint = temp;
	                 }
	             }
	        	 break;
	        case SCREEN_MAINMENU_SETTIME:
	        	 //                     0123456789  
	        	 //JDA  CMD_MSG_LONG 2 "  07/20/14 SUN  "
	        	 //JDA  CMD_MSG_LONG 3 "    10:00 AM    "
	        	 try
	        	 {
	            	 if (lineNum==2)
	            	 {
	            		 pendingDateTime.set(Calendar.MONTH, Integer.parseInt(msg.substring(2,4).trim())-1);
	            		 pendingDateTime.set(Calendar.DAY_OF_MONTH, Integer.parseInt(msg.substring(5,7).trim()));
	            		 pendingDateTime.set(Calendar.YEAR, Integer.parseInt(msg.substring(8,10).trim())+2000);
	            	 }
	            	 else if (lineNum==3)
	            	 {
	            		 int hour=Integer.parseInt(msg.substring(4,6).trim());
	            		 if (hour==12) hour=0;
	               		 pendingDateTime.set(Calendar.HOUR, hour);
	               		 pendingDateTime.set(Calendar.MINUTE, Integer.parseInt(msg.substring(7,9).trim()));
	               		 if (msg.charAt(10)=='A')
	               			 pendingDateTime.set(Calendar.AM_PM, Calendar.AM);
	               		 else
	               			 pendingDateTime.set(Calendar.AM_PM, Calendar.PM);
	            	 }
	             }
	        	 catch (Exception e)
	        	 {
	        		 System.out.println(e.toString()+ " lineNum:" + lineNum + " msg:" + msg);
	        	 }
	logger.logInfo("PENDING TIME: " + pendingDateTime.getTime().toString());
	        	 break;
	         case SCREEN_PENDING:
	           if (lineNum==0 && msg.equals("EQUIPMENT STATUS"))
	           {
	              currentScreen = SCREENS.SCREEN_EQUIPMENTSTATUS;
	              this.clearEquipmentStatus(); //TODO we will have a blip where all status are artificially cleared
	           }
	           else if (lineNum==1 && msg.equals(" PDA-PS4 Combo  ") || lineNum==3 && msg.equals("Firmware Version"))
	           {
	              currentScreen=SCREENS.SCREEN_STARTUP;
	              this.pdaReady=false;
	              keyManager.clearKeyPress();
	           }
	           else if (lineNum==4 && msg.startsWith("POOL MODE  "))
	           {
	              currentScreen=SCREENS.SCREEN_HOME;
	           }
	           else if (lineNum==0 && msg.equals("   EQUIPMENT    "))
	           {
	              currentScreen = SCREENS.SCREEN_EQUIPMENTONOFF;
	           }
	           else if (lineNum==0 && msg.equals("   MAIN MENU    "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU;
	           }
	           else if (lineNum==0 && msg.equals("    SPA HEAT    "))
	           {
	              currentScreen = SCREENS.SCREEN_EQUIPMENTONOFF_SPAHEAT;
	           }
	           else if (lineNum==0 && msg.equals("   POOL HEAT    "))
	           {
	              currentScreen = SCREENS.SCREEN_EQUIPMENTONOFF_POOLHEAT;
	           }
	           else if (lineNum==0 && msg.equals("    PROGRAM     "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU_PROGRAM;
	           }
	           else if (lineNum==0 && msg.equals("    SET TEMP    "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU_SETTEMP;
	              keyManager.setSelectedField(-1);
	           }
	           else if (lineNum==0 && msg.equals("    SET TIME    "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU_SETTIME;
	           }
	           else if (lineNum==0 && msg.equals("  SET AquaPure  "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU_SETAQUAPURE;
	           }
	           else if (lineNum==0 && msg.equals("   BOOST POOL   "))
	           {
	              currentScreen = SCREENS.SCREEN_MAINMENU_BOOSTPOOL;
	           }
	                      
	           if (lineNum == 5)
	           {
	              //buttons 1 or 2
	           }
	           this.lastScreen = currentScreen;
	           logger.logInfo("NEW SCREEN:" + currentScreen.name());
	           parseEquipmentOnOff(equipmentName, currentStatus); //TODO this is redundant because we check for KEY_ONE, KEY_TWO above
	            break;
	          case SCREEN_EQUIPMENTSTATUS:
	            boolean equipFound=false;
	            this.lastEquipmentStatus=AquaLinkManager.now();
	            for (int i=0;i<equipmentStatus.length;i++)
	            {
	               if (msg.trim().equals(EQUIPMENT_LABELS[i]) 
	                     || (msg.trim().equals("SPA HEAT ENA") && EQUIPMENT.SPAHEAT.ordinal()==i))
	               {
	                  equipmentStatus[i]=EQUIPMENTSTATUS.ON;
	                  equipFound=true;
	               }
	             }
	            
//	             if (msg.contains("AquaPure"))
//	             {
//	            	 this.aquaPureItemsAvailable = true;
//	             }
	             
	             if (!equipFound && lineNum!=0)
	             {
	                if (msg.trim().equals("CHECK AquaPure"))
	                {
	                   this.aquapureError = "   "; //non zero length
	                }
	                else if (this.aquapureError.length()>0) //once we get check aquapure, all messages are errors
	                {
	                   if (aquapureError.length()>0) aquapureError += " ";
	                   aquapureError+=msg.trim();
	                   aquapureError=aquapureError.trim();
	                }
	                else //none error
	                {
	                   if (miscStatus.length()>0) miscStatus += ", ";
	                   miscStatus+=msg.trim();
	                }
	             }

	            break;
	         }
	      }

	}
	   private void parseEquipmentOnOff(String equipmentName, EQUIPMENTSTATUS currentStatus)
	   {
	     if (currentStatus==null)
	        return;
	     for (int i=0; i < EQUIPMENT_LABELS.length; i++)
	     {
	         if (equipmentName.equals(EQUIPMENT_LABELS[i]))
	         {

	            if (equipmentStatus[i]!=currentStatus)
	            {
	               equipmentStatus[i]=currentStatus;
	               dumpStatus();
	            }
	            break;
	         }
	       }      
	   }

	   private void clearEquipmentStatus()
	   {
	      for (int i=0;i<equipmentStatus.length;i++)
	         equipmentStatus[i]=EQUIPMENTSTATUS.OFF;
	      
	      miscStatus="";
	      this.aquapureError="";
	   }
	   public void dumpStatus()
	   {
	      logger.logInfo("");
	      logger.logInfo("---STATUS DUMP---");
	      logger.logInfo(this.getDayTime());
	      logger.logInfo(String.format("Air %s F", this.getCurrentAirTemperature()));
	      logger.logInfo(String.format("Water %s F", this.getCurrentWaterTemperature()));
	      for (int i=0;i<equipmentStatus.length;i++)
	      {
	         if (equipmentStatus[i]==EQUIPMENTSTATUS.ON)
	        	 logger.logInfo(EQUIPMENT_LABELS[i] + " ON");
	      }
	      if (miscStatus.length()>0)
	    	  logger.logInfo(miscStatus);
	      logger.logInfo("");
	   }

	      
	public KeyManager getKeyManager()
	{
		return this.keyManager;
	}
	public SCREENS getCurrentScreen()
	{
		return currentScreen;
	}
	public boolean isAquaPureAvailable()
	{
		return this.aquaPureItemsAvailable;
	}
	public SCREENS getLastScreen()
	{
		return this.lastScreen;
	}
	public void setCurrentScreen(SCREENS screen)
	{
		this.currentScreen=screen;
		//TODO  is this best place to clear display?
		this.clearScreenDisplay();
	}
	public String getDayTime()
	{
		return this.currentDayTime;
	}
	public String getCurrentAirTemperature() {
		return currentAirTemperature;
	}
	public String getCurrentWaterTemperature() {
		return currentWaterTemperature;
	}
	public Integer getPendingSpaHeaterSetPoint() {
		return pendingSpaHeaterSetPoint;
	}
	public Integer getPendingPoolHeaterSetPoint() {
		return pendingPoolHeaterSetPoint;
	}
	public Integer getPendingPoolAquaPureSetPoint() {
		return pendingPoolAquaPureSetPoint;
	}
	public Integer getPendingSpaAquaPureSetPoint() {
		return pendingSpaAquaPureSetPoint;
	}
	public EQUIPMENTSTATUS[] getEquipmentStatus() {
		return equipmentStatus;
	}
	public Integer getPoolHeaterSetPoint() {
		return poolHeaterSetPoint;
	}
	public void setPoolHeaterSetPoint(Integer poolHeaterSetPoint) {
		this.poolHeaterSetPoint = poolHeaterSetPoint;
	}
	public Integer getSpaHeaterSetPoint() {
		return spaHeaterSetPoint;
	}
	public void setSpaHeaterSetPoint(Integer spaHeaterSetPoint) {
		this.spaHeaterSetPoint = spaHeaterSetPoint;
	}
	public Integer getPoolAquaPureSetPoint() {
		return poolAquaPureSetPoint;
	}
	public void setPoolAquaPureSetPoint(Integer poolAquaPureSetPoint) {
		this.poolAquaPureSetPoint = poolAquaPureSetPoint;
	}
	public Integer getSpaAquaPureSetPoint() {
		return spaAquaPureSetPoint;
	}
	public void setSpaAquaPureSetPoint(Integer spaAquaPureSetPoint) {
		this.spaAquaPureSetPoint = spaAquaPureSetPoint;
	}
	public Calendar getPendingDateTime() {
		return pendingDateTime;
	}
	public String getAquaPureError()
	{
		return this.aquapureError;
	}
	public String getMiscStatus()
	{
		return this.miscStatus;
	}
	public boolean isPdaReady() {
		return pdaReady;
	}
	public void setPdaReady(boolean pdaReady) {
		this.pdaReady = pdaReady;
	}
	private void clearScreenDisplay() {
		for (int i=0; i< screenDisplay.length; i++)
			screenDisplay[i]="&nbsp;";
	}
	private void setScreenLine(int lineNum, String line)
	{
		if (lineNum<0 || lineNum > screenDisplay.length-1) return;
		screenDisplay[lineNum]=line;
	}
	public String getScreenDisplay() {
		String content="";
		for (int i=0; i < screenDisplay.length; i++)
			content+=screenDisplay[i]+"\r\n";
		return content;
	}
	public String getScreenDisplayHtml() {
		StringBuffer content = new StringBuffer();
		for (int line=0; line < screenDisplay.length; line++)
		{
			if (keyManager.hiliteLine(line))
				content.append("<div style='color: #FFFFFF; background-color: #000000; font-family: monospace'>");
			else
				content.append("<div style='color: #000000; background-color: #ffffff; font-family: monospace'>");
			for (int column=0; column < screenDisplay[line].length(); column++)
			{
				char c = screenDisplay[line].charAt(column);
				
				if (keyManager.startFieldHilite(line, column))
					content.append("<span style=\"color: #FFFFFF; background-color: #000000;\">");
				if (c==0) 
					break;
				else if (c==' ')						
					content.append("&nbsp;");
				else content.append(c);
				
				if (keyManager.endFieldHilite(line, column))
					content.append("</span>");
			}
			content.append("</div>\r\n");
		}
		return content.toString();				
	}
	public void scrollScreen(byte first, byte last, byte direction) {
		if (direction==1) //KEY UP
		{
			for (int i=last; i > first; i--)
				screenDisplay[i]=screenDisplay[i-1];
		}
		else
		{
			for (int i=first; i < last; i++)
				screenDisplay[i]=screenDisplay[i+1];
		}
	}	
}