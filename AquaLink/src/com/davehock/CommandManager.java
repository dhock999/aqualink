package com.davehock;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import com.davehock.AquaLinkManager.EQUIPMENT;
import com.davehock.AquaLinkManager.EQUIPMENTSTATUS;
import com.davehock.KeyManager.COMMAND_KEYS;
import com.davehock.ScreenManager.SCREENS;

public class CommandManager {
	KeyManager keyManager;
	ScreenManager screenManager;
	AquaLinkCommand pendingCommand;
	Calendar setDateTime = Calendar.getInstance();

	private Logger logger = new Logger();

	public CommandManager(ScreenManager screenManager, KeyManager keyManager)
	{
		this.keyManager=keyManager;
		this.screenManager=screenManager;
	}
	
	Queue<AquaLinkCommand> commandQueue = new LinkedList<AquaLinkCommand>();
	
	//TODO AddCommand

	   private void executeSetSpaTemperature(Integer setPoint)
	   {
		   if (screenManager.getSpaHeaterSetPoint()==null || setPoint.intValue() != screenManager.getSpaHeaterSetPoint().intValue())
		   {
			   if (keyManager.goToSetTemperature())
			   {
				   if (keyManager.isEditingLine())
				   {
	logger.logInfo("EDITING TEMP " + setPoint + " " + screenManager.getPendingSpaHeaterSetPoint());
					   if (setPoint.intValue() == screenManager.getPendingSpaHeaterSetPoint().intValue())
					   {
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);	
						   screenManager.setSpaHeaterSetPoint(setPoint);
	logger.logInfo("EDITING TEMP DONE ");
			               clearPendingCommand();
					   }
					   else if (setPoint.intValue()<screenManager.getPendingSpaHeaterSetPoint().intValue())
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_DOWN);
					   else
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_UP);
				   }
				   else if (keyManager.goToLine(2,1,2)) //set spa is second item
	               {
	logger.logInfo("EDITING TEMP START ");
	                  keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
	               }
			   }
		   }
		   else
		   {
			  this.clearPendingCommand();
		   }
	   }
	   
	   private void executeSetPoolTemperature(Integer setPoint)
	   {
		   if (screenManager.getPoolHeaterSetPoint()==null || setPoint.intValue() != screenManager.getPoolHeaterSetPoint().intValue())
		   {
			   if (keyManager.goToSetTemperature())
			   {
				   if (keyManager.isEditingLine())
				   {
					   if (setPoint.intValue() == screenManager.getPendingPoolHeaterSetPoint().intValue())
					   {
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);						   
						   screenManager.setPoolHeaterSetPoint(setPoint);
			               clearPendingCommand();
					   }
					   else if (setPoint.intValue()<screenManager.getPendingPoolHeaterSetPoint().intValue())
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_DOWN);
					   else
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_UP);
				   }
				   else if (keyManager.goToLine(1,1,2)) //set pool is 1st item
	               {
						keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
	               }
			   }
		   }
		   else
		   {
			  this.clearPendingCommand();
		   }
	   }

	   private void executeSetPoolAquaPure(Integer setPoint)
	   {
		   if (screenManager.getPoolAquaPureSetPoint()==null || setPoint.intValue() != screenManager.getPoolAquaPureSetPoint().intValue())
		   {
			   if (keyManager.goToSetAquaPure())
			   {
		    	   if (!screenManager.isAquaPureAvailable())
		    	   {
		    		   //cancel all the aquapureMethods
		    		   clearPendingCommand();
		    	   }
		    	   else if (keyManager.isEditingLine())
				   {
		    		   logger.logInfo("EDITING AQUAPURE " + setPoint + " " + screenManager.getPendingPoolAquaPureSetPoint());
					   if (setPoint.intValue() == screenManager.getPendingPoolAquaPureSetPoint().intValue())
					   {
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);						   
						   logger.logInfo("EDITING AQUAPURE DONE ");
						   screenManager.setPoolAquaPureSetPoint(setPoint);
			               clearPendingCommand();
					   }
					   else if (setPoint.intValue()<this.screenManager.getPendingPoolAquaPureSetPoint().intValue())
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_DOWN);
					   else
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_UP);
				   }
				   else if (keyManager.goToLine(3,3,4)) //set pool is 1st item
	               {
					   logger.logInfo("EDITING AQUAPURE START ");
	                   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
	               }
			   }
		   }
		   else
		   {
			  this.clearPendingCommand();
		   }			   
	   }
	   
	   private void executeSetSpaAquaPure(Integer setPoint)
	   {
		   if (screenManager.getSpaAquaPureSetPoint()==null || setPoint.intValue() != screenManager.getSpaAquaPureSetPoint().intValue())
		   {
			   if (keyManager.goToSetAquaPure())
			   {
		    	   if (!screenManager.isAquaPureAvailable())
		    	   {
		    		   clearPendingCommand();
		    	   }
		    	   else if (keyManager.isEditingLine())
				   {
					   if (setPoint.intValue() == screenManager.getPendingSpaAquaPureSetPoint().intValue())
					   {
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);						   
						   logger.logInfo("EDITING AQUAPURE DONE ");
						   screenManager.setSpaAquaPureSetPoint(setPoint);
			               clearPendingCommand();
			           }
					   else if (setPoint.intValue()<screenManager.getPendingSpaAquaPureSetPoint().intValue())
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_DOWN);
					   else
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_UP);
				   }
				   else if (keyManager.goToLine(4,3,4)) //set spa is 2nd item
	               {
					   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
	               }
			   }
		   }
		   else
		   {
			  this.clearPendingCommand();
		   }			   
	   }
	   
	   private void executeSetDateTime()
	   {
			   if (keyManager.goToSetTime())
			   {
				   logger.logInfo("***Execute Date Time: " + keyManager.getSelectedField() + " " + setDateTime.getTime().toString());
				   logger.logInfo("***Pending Date Time: " + keyManager.getSelectedField() + " " + setDateTime.getTime().toString());

				   if (keyManager.getSelectedField() != -1)
				   {
					   int pendingValue = screenManager.getPendingDateTime().get(keyManager.getSelectedField());
//					   if (selectedField==Calendar.HOUR && pendingValue==0) pendingValue=12;
	logger.logInfo(String.format("***Pending %d Target: %d ",pendingValue,this.setDateTime.get(keyManager.getSelectedField())));
					   if (pendingValue==this.setDateTime.get(keyManager.getSelectedField()))
					   {
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);	
						   if (keyManager.getSelectedField()==Calendar.MINUTE)
						   {
							   clearPendingCommand();
						   }
					   }
					   else if (pendingValue<this.setDateTime.get(keyManager.getSelectedField()))
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_UP);	
					   else
						   keyManager.setKeyPress(COMMAND_KEYS.KEY_DOWN);		   
				   }
			   }
		   }

	   //TODO take advantage of home menu items and key one and key two for speed
	   private void executeEquipmentStatus(AquaLinkCommand command)
	   {
	         if(screenManager.getEquipmentStatus()[command.getEquipment().ordinal()]==EQUIPMENTSTATUS.PENDING)
	            return;//wait for equipment to be finished before clearing
	         if(command.getEquipment()==EQUIPMENT.LOWSPEED && command.getStatus()==EQUIPMENTSTATUS.ON && screenManager.getEquipmentStatus()[EQUIPMENT.FILTERPUMP.ordinal()]==EQUIPMENTSTATUS.OFF)
	         {
	            //TODO we just ignore if pump not on first
	        	 clearPendingCommand();
	             return;
	         }         

	         if (screenManager.getEquipmentStatus()[command.getEquipment().ordinal()]!=command.getStatus())
	         {
	            //TODO for speed if on Home screen, use POOL MODE, SPA MODE, SPA HEATER, POOL HEATER 
//	          THIS  CMD_MSG_LONG 4 "POOL MODE    OFF"
//	          THIS  CMD_MSG_LONG 5 "POOL HEATER  OFF"
//	          THIS  CMD_MSG_LONG 6 "SPA MODE     OFF"
//	          THIS  CMD_MSG_LONG 7 "SPA HEATER   OFF"
//	          THIS  CMD_MSG_LONG 8 "MENU            "
//	          THIS  CMD_MSG_LONG 9 "EQUIPMENT ON/OFF"

	              //TODO lights flashing on/off if key_two is used but not working because light flashes on and off!
	            if (screenManager.getCurrentScreen()==SCREENS.SCREEN_HOME || screenManager.getCurrentScreen()==SCREENS.SCREEN_EQUIPMENTONOFF)
	            {
	            	//TODO these toggle...smarter to check status first??
//	                if (command.getEquipment() == EQUIPMENT.SPILLOVER)
//	                {
//	                	keyManager.setKeyPress( COMMAND_KEYS.KEY_TWO);
//	                	clearPendingCommand();
//	                    return;
//	                }
//	                else 
	                if (command.getEquipment() == EQUIPMENT.JETPUMP)
	                {
	                	keyManager.setKeyPress(COMMAND_KEYS.KEY_ONE);
	                	clearPendingCommand();
	                    return;
	                }
	            }

	        	if (keyManager.goToEquipmentOnOff())//at SCREEN_EQUIPMENTONOFF
	            {
	               if (keyManager.goToLine(command.getEquipment().ordinal()+1,ScreenManager.MENUSTART_EQUIPMENTONOFF,ScreenManager.MENUEND_EQUIPMENTONOFF))
	               {
	                  keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
	               }
	            }
	         }
	         else
	         {
             	clearPendingCommand();
	         }
	      }

	   private boolean executeAllEquipmentOff()
	   {
		   if (keyManager.goToEquipmentOnOff())
		   {
	           if (keyManager.goToLine(ScreenManager.MENUITEM_EQUIPMENTONOFF_ALLOFF,ScreenManager.MENUSTART_MAINMENU,ScreenManager.MENUEND_MAINMENU))
	           {
	              keyManager.setAllowRetry(false);//we may get no response if no equipment is on.....e; 
	              keyManager.setKeyPress(COMMAND_KEYS.KEY_SELECT);
//	              screenManager.setCurrentScreen(SCREENS.SCREEN_UNKNOWN); //force back key???
//	              keyManager.setKeyPress(COMMAND_KEYS.KEY_BACK);
	              clearPendingCommand();//pull the command and discard
	           }
		   }	
	       return false;
	   }
	   //TODO implement a queue of commands!!
	   public void executeCommands()
	   {
	       if(screenManager.getCurrentScreen()==SCREENS.SCREEN_STARTUP)
	           return; //wait for startup

	       if(keyManager.getSelectedLine()==-1 && keyManager.getSelectedField()==-1)
	           return; //wait for first line num change for new screen

	       if (pendingCommand==null)
	       {
	    	   try {
		    	   pendingCommand=commandQueue.poll();
		    	   if (pendingCommand!=null)
		    	   {
		    		   while (pendingCommand.isRedundant(commandQueue.peek()))
		    			   pendingCommand=commandQueue.poll();
		    	   }	    		   
	    	   } catch (Exception e) {
	    		   logger.logInfo("error" + e.getMessage());
	    		   pendingCommand = null;
	    	   }
	       }
		   if (pendingCommand==null)return;		   
	       
		   keyManager.setAllowRetry(true);
		   
		   switch (pendingCommand.getType())
		   {
		   case COMMAND_ALLOFF:
			   executeAllEquipmentOff();
			   break;
		   case COMMAND_EQUIPMENT:
	    	   executeEquipmentStatus(pendingCommand);
			   break;
		   case COMMAND_SPASETPOINT:
	    	   executeSetSpaTemperature(pendingCommand.getParameter());
			   break;
		   case COMMAND_POOLSETPOINT:
	    	   executeSetPoolTemperature(pendingCommand.getParameter());
			   break;
		   case COMMAND_SPAAQUAPURE:
	    	   executeSetSpaAquaPure(pendingCommand.getParameter());
			   break;
		   case COMMAND_POOLAQUAPURE:
	    	   executeSetPoolAquaPure(pendingCommand.getParameter());
			   break;
		   case COMMAND_TIME:
	    	   executeSetDateTime();
			   break;
		   default:
			   clearPendingCommand(); //unknown command!
			   break;  
		   }
	   }
	   
	   private void clearPendingCommand()
	   {
		   pendingCommand=null;
	   }
	
	//***********************************************
	//*These are the commands that go into the queue. 
	//* FOR temperature don't let the commands stack up or else the pending operation will be corrupted by update from openhab
	//***********************************************
	public void setSpaTemperature(int setSpaTemperature) {
		if (screenManager.getSpaHeaterSetPoint()== null || setSpaTemperature != screenManager.getSpaHeaterSetPoint().intValue())
			if (this.getPendingCommand() == null || getPendingCommand().getType() != AquaLinkCommand.COMMAND_TYPE.COMMAND_SPASETPOINT)
				commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_SPASETPOINT, new Integer(setSpaTemperature)));
	}
	public void setPoolTemperature(int setPoolTemperature) {
		if (screenManager.getPoolHeaterSetPoint()==null || setPoolTemperature != screenManager.getPoolHeaterSetPoint().intValue())
			if (getPendingCommand() == null || getPendingCommand().getType() != AquaLinkCommand.COMMAND_TYPE.COMMAND_POOLSETPOINT)
				commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_POOLSETPOINT, new Integer(setPoolTemperature)));
	}
	public void setPoolAquaPure(int setPoolAquaPure) {
		if (screenManager.getPoolAquaPureSetPoint() == null || setPoolAquaPure != screenManager.getPoolAquaPureSetPoint().intValue())
			if (getPendingCommand() == null || getPendingCommand().getType() != AquaLinkCommand.COMMAND_TYPE.COMMAND_POOLAQUAPURE)
				commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_POOLAQUAPURE, new Integer(setPoolAquaPure)));
	}
	public void setSpaAquaPure(int setSpaAquaPure) {
		if (screenManager.getSpaAquaPureSetPoint() == null || setSpaAquaPure != screenManager.getSpaAquaPureSetPoint().intValue())
			if (getPendingCommand() == null || getPendingCommand().getType() != AquaLinkCommand.COMMAND_TYPE.COMMAND_SPAAQUAPURE)
				commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_SPAAQUAPURE, new Integer(setSpaAquaPure)));
	}
	public void setDateTime(Date dateTime) {
	   keyManager.setSelectedField(-1);
	   this.setDateTime.setTime(dateTime);
	   commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_TIME));
	}
	public void setAllEquipmentOff() {
		commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_ALLOFF));
	}
	public void setEquipmentStatus(EQUIPMENT equipment, EQUIPMENTSTATUS status)
	{
        if (equipment!=null && status!=null)
        {
        	if (equipment==EQUIPMENT.LOWSPEED)
        	{
        		if (status==EQUIPMENTSTATUS.ON)
        		{
        			//TODO turn spillover off
        			//turn spa off
            		//turn filter pump on
        			
        		}
        		else
        		{
        			
        		}
        		
        	}
        	else
        	if (equipment==EQUIPMENT.SPILLOVER)
        	{
        		//turn pump on first??
        	}
        	commandQueue.add(new AquaLinkCommand(AquaLinkCommand.COMMAND_TYPE.COMMAND_EQUIPMENT, equipment, status));
        }
	}
	public int getCommandQueueLength()
	{
		if (commandQueue==null)
			return 0;
		return commandQueue.size();
	}
	
	public AquaLinkCommand getPendingCommand()
	{
		return pendingCommand;
	}
	   
	public void cancelCommands()
	{
		commandQueue.clear();
		pendingCommand=null;
	}
}