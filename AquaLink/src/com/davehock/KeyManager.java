package com.davehock;

import java.util.Calendar;

import com.davehock.ScreenManager.*;

public class KeyManager {
	// COMMAND KEYS
	Logger logger = new Logger();
	public enum COMMAND_KEYS  {
	   KEY_NONE, //0
	   KEY_ONE,
	   KEY_BACK, //02
	   KEY_TWO,
	   KEY_SELECT, //4
	   KEY_DOWN, //5
	   KEY_UP //6
	};
	
	int selectedLine=-2;
	
	int selectedColumn=-1;
	int selectedColumnLength = 0;
	int selectedColumnHasFocus = 0;
	
	private COMMAND_KEYS sendKeyPress=null;
	private boolean isEditingLine=false;
	private int selectedField = -1;
	private ScreenManager screenManager;
	private long whenKeySent = 0;
	private boolean keyPressPending = false;
	private boolean allowRetry=false;


	public boolean isAllowRetry() {
		return allowRetry;
	}

	public void setAllowRetry(boolean allowRetry) {
		this.allowRetry = allowRetry;
	}

	public boolean isEditingLine() {
		return isEditingLine;
	}

	public void setEditingLine(boolean isEditingLine) {
		this.isEditingLine = isEditingLine;
		logger.logInfo("Is editing line" + isEditingLine);
	}

	public KeyManager(ScreenManager screenManager)
	{
		this.screenManager=screenManager;
	}
	
	public int getSelectedLine() {
		return selectedLine;
	}

	public int getSelectedField() {
		return selectedField;
	}
	public void setSelectedField(int field)
	{
		this.selectedField=field;
	}
   public void setPendingKeyPress(COMMAND_KEYS key)
   {
       if (sendKeyPress == null) //don't stomp on pending command
    	   sendKeyPress=key;
   }
   public COMMAND_KEYS getPendingKeyPress()
   {
	   return this.sendKeyPress;
   }
	
	
	//*******************************************
   //*MENU NAVIGATIONS
   //*******************************************

   public boolean goToHome()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_HOME)
    	   return true;
       
       this.setKeyPress(COMMAND_KEYS.KEY_BACK);
	   return false;
   }
   public boolean goToEquipmentOnOff()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_EQUIPMENTONOFF)
    	   return true;
       
       if (goToHome())//at SCREEN_HOME
       {
            if (goToLine(ScreenManager.MENUITEM_EQUIPMENTONOFF,ScreenManager.MENUSTART_HOME,ScreenManager.MENUEND_HOME))
            {
               this.setKeyPress(COMMAND_KEYS.KEY_SELECT);
            }
       }            
       return false;
   }

   public boolean goToMainMenu()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_MAINMENU)
    	   return true;
       
       if (goToHome())//at SCREEN_HOME
       {
            if (goToLine(ScreenManager.MENUITEM_MAINMENU,ScreenManager.MENUSTART_HOME,ScreenManager.MENUEND_HOME))
            {
               this.setKeyPress(COMMAND_KEYS.KEY_SELECT);
            }
       }            
       return false;
   }
   
   public boolean goToSetTemperature()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_MAINMENU_SETTEMP)
    	   return true;
       
       if (goToMainMenu())
       {
           if (goToMainMenuLine(ScreenManager.MENUITEM_MAINMENU_SETTEMP,ScreenManager.MENUSTART_MAINMENU,ScreenManager.MENUEND_MAINMENU))
           {
              this.setKeyPress(COMMAND_KEYS.KEY_SELECT);
           }
       }
	   return false;
   }
   
   public boolean goToSetAquaPure()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_MAINMENU_SETAQUAPURE)
    	   return true;
       
       if (goToMainMenu())
       {
           if (goToMainMenuLine(ScreenManager.MENUITEM_MAINMENU_SETAQUAPURE,ScreenManager.MENUSTART_MAINMENU,ScreenManager.MENUEND_MAINMENU))
           {
	    	   if (screenManager.isAquaPureAvailable())
	    		   this.setKeyPress(COMMAND_KEYS.KEY_SELECT);
           }
       }
	   return false;
   }
   
   //main menu shifts around if pool is running and boost pool is showing
   private boolean goToMainMenuLine(long targetLine, long firstLine, long numLines)
   {
	   if (!screenManager.isAquaPureAvailable())
	   {
		   targetLine++;
		   firstLine++;
	   }
	   return goToLine(targetLine,firstLine,numLines);
   }
   public boolean goToSetTime()
   {
       if (screenManager.getCurrentScreen()==SCREENS.SCREEN_MAINMENU_SETTIME)
    	   return true;
       
       if (goToMainMenu())
       {
           if (goToMainMenuLine(ScreenManager.MENUITEM_MAINMENU_SETTIME,ScreenManager.MENUSTART_MAINMENU,ScreenManager.MENUEND_MAINMENU))
           {
              this.setKeyPress(COMMAND_KEYS.KEY_SELECT);
           }
       }
	   return false;
   }
   public boolean goToLine(long targetLine, long firstLine, long maxLine)
   {
	  if (selectedLine==targetLine)
		  return true;
//      if (false)
//      {
//         this.setKeyPress(COMMAND_KEYS.KEY_DOWN);
//      }
//      else
//      {
         //move to the target line using the fewest key presses
         if (selectedLine < targetLine)
         {
            if (targetLine - selectedLine < maxLine - targetLine + selectedLine - firstLine + 1)
               this.setKeyPress(COMMAND_KEYS.KEY_DOWN);
            else
               this.setKeyPress(COMMAND_KEYS.KEY_UP);
         }
         else
         {
            if (selectedLine - targetLine < maxLine - targetLine + selectedLine - firstLine + 1)
               this.setKeyPress(COMMAND_KEYS.KEY_UP);
            else
               this.setKeyPress(COMMAND_KEYS.KEY_DOWN);
         }         
//      }
      return false;
   }
	public void processSelectedLine(byte[] packet)
	{
	    this.clearKeyPress();
	    selectedColumn = -1;
        if (packet[AquaLinkManager.PKT_DATA]==-1)
        {
        	this.isEditingLine = true;
        }
        else
        {
            this.selectedLine = packet[AquaLinkManager.PKT_DATA];
            this.isEditingLine = false;
        }
	}
	
	public boolean startFieldHilite(int line, int column)
	{
		return selectedLine == line && selectedColumn == column && selectedColumnHasFocus == 1;
	}
	
	public boolean endFieldHilite(int line, int column)
	{
		return selectedLine == line && selectedColumnLength == column && selectedColumnHasFocus == 1;
	}
	
	public boolean hiliteLine(int line)
	{
		return selectedLine==line && selectedColumn==-1;
	}
	
	public void processFieldFocus(byte[] packet)
	{
		
		   this.selectedLine = packet[5];
		   this.selectedColumn = packet[6];
		   this.selectedColumnLength = packet[7];
		   this.selectedColumnHasFocus = packet[8];

		   clearKeyPress();
		   if (selectedColumnHasFocus==1)
		   {
			   switch (screenManager.getCurrentScreen())
			   {
			   case SCREEN_MAINMENU_SETTIME:
				   switch (selectedLine)
				   {
				   case 2:
					   switch (selectedColumn)
					   {
					   case 2:
						   selectedField = Calendar.MONTH;
						   logger.logInfo("CURRENT TIME FIELD: MONTH");
						   break;
					   case 5:
						   selectedField = Calendar.DAY_OF_MONTH;
						   logger.logInfo("CURRENT TIME FIELD: DAY_OF_MONTH");
						   break;
					   case 8:
						   selectedField = Calendar.YEAR;
						   logger.logInfo("CURRENT TIME FIELD: YEAR");
						   break;
					   }
					   break;
				   case 3:
					   switch (selectedColumn)
					   {
					   case 4:
						   selectedField = Calendar.HOUR_OF_DAY;
						   logger.logInfo("CURRENT TIME FIELD: HOUR");
						   break;
					   case 7:
						   selectedField = Calendar.MINUTE;
						   logger.logInfo("CURRENT TIME FIELD: MINUTE");
						   break;
					   }
					   break;	   
				   }
				   break;   
			   case SCREEN_MAINMENU_SETAQUAPURE:
				   break;
			   case SCREEN_MAINMENU_SETTEMP:
				   break;
			   }
			   
		   }
	}

   public void clearKeyPress()
   {
       keyPressPending=false;
       this.whenKeySent = 0;
       sendKeyPress = null;
   }
   public void setKeyPress(COMMAND_KEYS key)
   {
	   this.sendKeyPress=key;
   }
   public boolean isKeyPressPending()
   {
	   return keyPressPending;
   }
   public void setKeyPressPending(boolean pending)
   {
	   this.keyPressPending=pending;

   }
   public void setWhenKeySent(long tm)
   {
	   this.whenKeySent=tm;
   }
   public long getWhenKeySent()
   {
	   return this.whenKeySent;
   }
}