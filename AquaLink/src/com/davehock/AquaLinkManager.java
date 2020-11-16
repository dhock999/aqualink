package com.davehock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import com.davehock.KeyManager.COMMAND_KEYS;
import com.davehock.ScreenManager.SCREENS;

//import javax.comm.*;
//import gnu.io.*;
public class AquaLinkManager implements Runnable {
   
//ch341-uart converter now attached to ttyUSB2
	private String port = "/dev/ttyUSB0";  //"COM8" 
//	private String port = "COM4";  //"COM8" 
//private String port = "/dev/ttyUSB1";  //"COM8" 
//private String port = "/dev/usbPool";  //"COM8" 
// File: /etc/udev/rules.d/99_usbdevices.rules
//SUBSYSTEM=="tty", ATTRS{idVendor}=="1a86", ATTRS{idProduct}=="7523", SYMLINK+="usbPool", GROUP="dialout", MODE="0666"
//SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="8a28", SYMLINK+="usbMeter", GROUP="dialout", MODE="0666"
//dave@lubuntu:/usr/share/tomcat7/bin$ cat setenv.sh
//export CATALINA_OPTS="-Dgnu.io.rxtx.SerialPorts=/dev/usbMeter:/dev/usbPool"



//timeout times in seconds
private int KEYRESENDTIME = 5; 
private int WHENTOSLEEP = 15; //if inactive this long, go to sleep and wait for a probe
private int SYSTEMSLEEPTIME = 8; //how long to sleep for
private int PDAWAITTIME = 30; //how long to wait after PDA last seen awake
private int keyPressRetryCount = 0; //once we exceed retries we are going to sleep
private EQUIPMENTSTATUS unitStatus=EQUIPMENTSTATUS.ON;

private Logger logger=new Logger();
private static boolean debugging=false;


public enum EQUIPMENT {
   FILTERPUMP,
   SPA,
   POOLHEAT,
   SPAHEAT,
   JETPUMP,
   LOWSPEED,
//   POOLLIGHT,
   SPILLOVER,
   EXTRAAUX
};


public enum EQUIPMENTSTATUS {
   ON,
   OFF,
   PENDING
};

private String aquapureErrorLast="";


private String miscStatusLast=""; //stores the last miscStatus so it is preserved after equipment goes off

private byte lastDevice = 0;
private long lastPDAAck = 0;
private long thisSleepUntil;
private long lastActivity;
private String lastAquaPureResponse = "";

private boolean done=false;

private String status = "";
private String statusMessage = "" ;

//private BufferedReader inputStream;
//private InputStream inputStream;
//private OutputStream outputStream;
private SerialPort serialPort1;
private Thread readThread;

/* COMMANDS */
public enum COMMANDS {
   CMD_PROBE,
   CMD_ACK,
   CMD_STATUS,
   CMD_MSG,
   CMD_MSG_LONG,
   CMD_5,
   CMD_6,
   CMD_7,
   CMD_SELECTEDLINE,
   CMD_CURRENTSCREEN,
   CMD_A,
   CMD_B,
   CMD_C,
   CMD_D,
   CMD_E,
   CMD_SCROLLSCREEN,
   CMD_FIELDFOCUS,
   CMD_SETAQUAPURELEVEL, //Next byte is percentage number 0-100 for output level 101 for "boost mode"
   CMD_AQUALINK_STATUS, //response from aquapure
   CMD_13,
   CMD_IDENTIFY,
   CMD_AQUALINK_GETSTATUS,
   CMD_16,
   CMD_17,
   CMD_18,
   CMD_19,
   CMD_1A,
   CMD_PDASTATUS  
}

//+++++++++++++++++++++++++++++++++++++++
//When Zodiac TRi set to AquaPure Rev L/M:
//Chlorinator RS485 Aqualink protocol
//+++++++++++++++++++++++++++++++++++++++
//
//
//<head><data><chksum><tail>
//data=<destination><command><parameter>
//
//
//(all in HEX)
//head=10:02
//tail=10:03
//destination=50
//command=00/11/14
//
//
//Master/Slave:
//
//
//probe:
//50:00
//00:01:00:00
//
//
//identify:
//50:14:01
//00:03:01:41:71:75:61:50:75:72:65:00:00:00:00:00:00:00:00
//="AquaPure"

//set output/check status:
//Master:<50><11><output%/boost>
//where: 
//<output%/boost> = 0-100 for output level 
//<output%/boost> = 101 for "boost mode"
//
//Slave:<00><version><salt><error><add salt:LowByte><add salt:HighByte>
//where:
//<version>=0x16="salt/100+add salt"
//<error>= bit0=no flow, b1=low salt, b2=high salt, b3=general fault
//<salt>=ppm salt level/100
//<add salt>=<16bit signed word>=lb salt to be added
//
//
//----------------------------------------------------
//Examples:
//
//Master:50:11:01
//=1% output
//Slave: 00:16:20:01:03:04:50
//=Salt 3200 PPM, NO Flow Error,Add 1027 lbs Salt
//
//
//set output 0%:
//50:11:00
//00:16:28:00:00:00
//set output 34%:
//50:11:22
//00:16:28:00:00:00
//set output 100%:
//50:11:64
//00:16:28:00:00:00
//
//set BOOST (100%) (lights BOOST LED)
//50:11:65
//00:16:28:00:00:00
//
//ERROR:
//inresponse to
//50:11:xx
//No flow : 00:16:28:01:00:00
//Low Salt: 00:16:28:02:00:00
//High Salt: 00:16:28:04:00:00
//Gen Fault: 00:16:28:08:00:00
//
//
//NO signal will turn off ZODIAC TRi in 20 seconds
//Start with again with probe:
//50:00
//00:01:00:00

//======================//
//=== AQUARITE COMMS ===//
//======================//

////Aquarite Commands
//CMD_AQUARITE_STAT       = 0x11; // Responds with Status (PPM, FLOW,...)
//CMD_AQUARITE_IDENT      = 0x14; // Responds with MSG "BOOST for all arguments except for "1", where it responds with its name (Aquapure, ...)
//CMD_AQUARITE_STAT2      = 0x15; // Responds with Status (PPM, FLOW,...)
//
////Aquarite responses to Master
//RESP_AQUARITE_IDENT     = 0x03;
//RESP_AQUARITE_STAT      = 0x16;
//
////Aquarite Status
//STAT_AQUARITE_OK        = 0x00;
//STAT_AQUARITE_NOFLOW    = 0x01;
//STAT_AQUARITE_LOSALT    = 0x02;
//STAT_AQUARITE_HISALT    = 0x04;
//STAT_AQUARITE_GENERAL   = 0x08;

//2019-03-17- 18:45:19 AQUAPURE  CMD:14  
//2019-03-17- 18:45:19 10 02 50 14 01 77 10 03 
//2019-03-17- 18:45:19 MASTER  CMD_MSG 0 "AquaPure *0  *0  *0  *0  *0  *0  *0  *0 "
//2019-03-17- 18:45:19 AQUAPURE  CMD_PROBE 
//2019-03-17- 18:45:19 10 02 50 00 62 10 03 
//2019-03-17- 18:45:19 MASTER  CMD_ACK 
//2019-03-17- 18:45:19 10 02 00 01 00 00 13 10 03 

// DEVICE CODES
// devices probed by master are 08-0b, 10-13, 18-1b, 20-23,
private static final byte  DEV_MASTER  =    0;
private static final byte  DEV_AQUAPURE = 0x50;
private static final byte  DEV_PDA = 0x60;

private static final byte  DEV_LXHEATER = 0x38;
private static final byte  DEV_PCDOCK = 0x58;
private static final byte  DEV_LXi_LZRE = 0x68;
private static final byte  DEV_AQUALINK = 0x33;
private static final  int  DEV_CHEMLINK = 0x81;


private byte deviceId = 0x60; //ID of the linux device 0x61 for old board

private static final byte NUL = 0x00;
private static final byte MSGDELIM_10 = 0x10;
private static final byte MSGSTART_02 = 0x02;
private static final byte MSGEND_03 = 0x03;

private static final int MAXPKTLEN  = 64;

/* how many seconds spa stays running after heat shutdown */
//private static final int SPA_COOLDOWN  = (20*60);

private byte lastPacket[];

byte AQUAPURE_NOFLOW_ERRORMASK = 0x1;
byte AQUAPURE_LOWSALT_ERRORMASK = 0x2;
byte AQUAPURE_HIGHSALT_ERRORMASK = 0x4;
byte AQUAPURE_GENERAL_ERRORMASK = 0x8;

//packet format 
//MSGDELIM_10 = 0
//MSGSTART_02 = 1
private static final int PKT_DEST      =  2; //device id
private static final int PKT_CMD       =  3;
public static final int PKT_DATA      =  4;
private static final int PKT_CHKSUM_FROMEND = 3;
private static final int PKT_KEY = 6;

private static final int PKT_AQUAPURE_SALT = 4;
private static final int PKT_AQUAPURE_ERROR = 5;
private static final int PKT_AQUAPURE_ADDSALT_LOWBYTE = 6;
private static final int PKT_AQUAPURE_ADDSALT_HIGHBYTE = 7;


//PKT_CHKSUM_FROMEND = LAST - 2
//MSGDELIM_10 = LAST - 1
//MSGEND_03 - LAST



private byte ackPacket[] = {MSGDELIM_10, MSGSTART_02, DEV_MASTER, (byte)COMMANDS.CMD_ACK.ordinal(), NUL, NUL, 0x13, MSGDELIM_10, MSGEND_03};
private byte ackPacketLong[] = {MSGDELIM_10, MSGSTART_02, DEV_MASTER, (byte)COMMANDS.CMD_ACK.ordinal(), 0x10, NUL, NUL, 0x23, MSGDELIM_10, MSGEND_03};
private byte setAquaPureLevelPacket[] = {MSGDELIM_10, MSGSTART_02, DEV_AQUAPURE, (byte)COMMANDS.CMD_SETAQUAPURELEVEL.ordinal(), NUL, NUL, MSGDELIM_10, MSGEND_03};
private byte getAquaPureStatusPacket[] = {MSGDELIM_10, MSGSTART_02, DEV_AQUAPURE, (byte)COMMANDS.CMD_AQUALINK_GETSTATUS.ordinal(), NUL, NUL, MSGDELIM_10, MSGEND_03};


private KeyManager keyManager;
private ScreenManager screenManager;
private CommandManager commandManager;
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
//	  byte test[] = {0x10,0x02,0x60,0x08,0x06,0x00,0x00,(byte)0xAC,0x10,03};
//	  .logger.logInfo(String.format("%02x ",generateChecksum(test)));
		AquaLinkManager smm = new AquaLinkManager();
      smm.init();
      while (true)
      {
         Thread.sleep(200);
      }
	}

	public AquaLinkManager() {
		System.out.println("AquaLinkManager");
	}

	public void init() {
		System.out.println("init");
		screenManager = new ScreenManager();
	   keyManager =	 screenManager.getKeyManager();
	   commandManager = new CommandManager(screenManager, keyManager);
	   done=false;
	   keyManager.clearKeyPress();
	   screenManager.setPdaReady(false);
	   this.lastActivity = now();
	   this.thisSleepUntil=0;

		try {		
			serialPort1 = SerialPort.getCommPort(port);
			serialPort1.setComPortParameters(9600, 8,
					SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
			serialPort1.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
			if (!serialPort1.openPort())
				throw new Exception("Port did not open");
			
		
			System.out.println("isOpen" + serialPort1.isOpen());
//			serialPort1.setInputBufferSize(0);
//			serialPort1.setOutputBufferSize(0);

//			serialPort1.notifyOnDataAvailable(true);
//	        serialPort1.addEventListener(this);
			
//	        outputStream = serialPort1.getOutputStream();
//	        inputStream = serialPort1.getInputStream();

			readThread = new Thread(this);
			logger.logInfo("Started on port: " + port);
			readThread.start();			
		} catch (Exception e) {
			this.handleException(e);
		}

	}
	
	// Reads the bytes of the next incoming packet, and
	// returns when a good packet is available in packet
	// file_descriptor: the file descriptor to read the bytes from
	// packet: the unsigned char buffer to store the bytes in
	// returns the length of the packet
	private byte[] getPacket() throws IOException
	{
	   byte dataByte;
	   int index = 0;
	   boolean endOfPacket = false;
	   int numBytes=0;
	   byte single[] = new byte[1];
	   byte packet[] = new byte[MAXPKTLEN];
	   while(!endOfPacket)
	   {
		   numBytes = serialPort1.readBytes(single, 1);
		   dataByte = single[0];
		   packet[index] = dataByte;
		   if (dataByte==MSGEND_03 && index>1 && packet[index-1]==MSGDELIM_10)
			   endOfPacket=true;

	       if (index >= MAXPKTLEN-1) {
	           logger.logInfo("**ERROR getPacket-Max Length Exceeded");
//	           startSleep();
	           break;
	       }
	       else
	    	   index++;
	   }

	   byte[] receivedPacket = Arrays.copyOf(packet, index);
	   if (debugging)
	       logPacket(receivedPacket);

	   return receivedPacket;
	}

   private void handleException (Exception e)
   {
	   this.handleException(e, "");
   }

	
   private void handleException (Exception e, String info)
   {
      this.status = "Exception";
      
       StackTraceElement[] elements = (e.getStackTrace());

       StringBuffer buf = new StringBuffer();

       for (int i = 0; i < elements.length; i++) {
         buf.append("    " + elements[i].getClassName() + "."
             + elements[i].getMethodName() + "(" + elements[i].getFileName() + ":"
             + elements[i].getLineNumber() + ")");
       }


      this.statusMessage = (new Date()).toString() + " " + e.toString() + " StackTrace=["+buf.toString()+ " " + this.port + " " + info;
      System.err.println(statusMessage);
   }
	
	public void readData()
	{
      try {
         byte[] packet=getPacket();
         if (packet.length<7)  //2 start, 1 destination, 1 cmd, 1 checksum, 2 end == 7
         {
        	 logPacket(packet); 
        	 logger.logInfo("****ERROR SHORT PACKET**** " + packet.length);
             return;
         }
         logPacket(packet); 
         checkPacket(packet);
//         if (debugging)// && packet[PKT_CMD] != COMMANDS.CMD_PROBE.ordinal())
//             logPacket(packet);

         // Ignore packets not for this Aqualink terminal device.

           if (packet[PKT_DEST] == this.deviceId) 
           {
        	   this.lastActivity = now();//if no messages for a while we sleep and wait for restart

              // Process the packet. This includes deriving general status, and identifying
              // warnings and errors.

              if (!isPDAAwake() && isThisAwake() && this.unitStatus==EQUIPMENTSTATUS.ON) //only ack if PDA offline for > 2 minutes
              { 
                sendAcknowledge(packet);
                processPacket(packet);
                if (!keyManager.isKeyPressPending() && packet[PKT_CMD]==COMMANDS.CMD_STATUS.ordinal())
                {
                	commandManager.executeCommands(); 
                }
              }
              lastPacket=packet;
           }
            else if (packet[PKT_DEST] == DEV_MASTER )
            {
               if (lastDevice==DEV_PDA || lastDevice==this.deviceId)
               {
                   {
                       if (!isPDAAwake()) 
                    	   logger.logInfo("  PDA AWAKE");
//****FOR NOW WE WILL USE same ID for linux emulator and PDA                       
                       this.lastPDAAck = now();
                   }
               }
//TODO this parses the aquapure response sent to master. We don't need it but if we use it need to check packet because once got a -112 command value which threw an exception               
               else if (lastDevice==DEV_AQUAPURE && packet[PKT_CMD]!=COMMANDS.CMD_ACK.ordinal())
               {
            	   setAquaPureResponse(packet);
               }
            }
            lastDevice = packet[PKT_DEST];   
      } catch (Exception e) {
         this.handleException(e);
      }
	}

	private void setAquaPureResponse(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append(COMMANDS.values()[packet[PKT_CMD]].name());
		
		if (packet[PKT_CMD]==COMMANDS.CMD_AQUALINK_STATUS.ordinal())
		{
			sb.append(" SALTLEVEL:" + packet[PKT_AQUAPURE_SALT]*100/2);
			
			if (packet[PKT_AQUAPURE_ERROR]!=0)
			{
				sb.append(" ERROR:");
				if ((packet[PKT_AQUAPURE_ERROR] & AQUAPURE_NOFLOW_ERRORMASK) != 0)
					sb.append(" NOFLOW");
				if ((packet[PKT_AQUAPURE_ERROR] & AQUAPURE_LOWSALT_ERRORMASK) != 0)
					sb.append(" LOWSALT");
				if ((packet[PKT_AQUAPURE_ERROR] & AQUAPURE_HIGHSALT_ERRORMASK) != 0)
					sb.append(" HIGHSALT");
				if ((packet[PKT_AQUAPURE_ERROR] & AQUAPURE_GENERAL_ERRORMASK) != 0)
					sb.append(" GENERALFAULT");
			}
		}
		sb.append(" ");
		for (int i=0; i < packet.length; i++)
			sb.append(String.format("%02x ", packet[i]));
		
		sb.append(" " + (new Date()).toString());

		this.lastAquaPureResponse = sb.toString();
	}
	
	public String getAquaPureResponse()
	{
		return this.lastAquaPureResponse;
	}

//	public void serialEvent(SerialPortEvent event) {
//
////logger.logInfo(event.getEventType());
//	   
//	   
//		switch (event.getEventType()) {
//		case SerialPortEvent.BI:
//		case SerialPortEvent.OE:
//		case SerialPortEvent.FE:
//		case SerialPortEvent.PE:
//		case SerialPortEvent.CD:
//		case SerialPortEvent.CTS:
//		case SerialPortEvent.DSR:
//		case SerialPortEvent.RI:
//		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
//		   logger.logInfo("***EVENT***: "+event.getEventType());
//			break;
//		case SerialPortEvent.DATA_AVAILABLE:
//		  // readData();
//
//			break;
//		}
//	}
	

private void startSleep()
{
   logger.logInfo(String.format("Sleeping for %d seconds ",SYSTEMSLEEPTIME));
   screenManager.setPdaReady(false);
   this.thisSleepUntil = now() + SYSTEMSLEEPTIME*1000;
}
private void dumpPacket(byte[] packet)
{
	StringBuffer sb = new StringBuffer();
   for (int i=0; i < packet.length; i++)
      sb.append(String.format("%02x ", packet[i]));   
   logger.logInfo(sb.toString());
}
private void dumpPacket(byte[] packet, int length)
{
   StringBuffer sb = new StringBuffer();
   for (int i=0; i < length; i++)
      sb.append(String.format("%02x ", packet[i]));   
   logger.logInfo(sb.toString());
}

private void logPacket(byte[] packet)
{
	StringBuffer sb=new StringBuffer();
//    print(getNow().getTimeInMillis() + " ");
	
	if (packet.length > PKT_DEST)
	{
	    byte device = packet[PKT_DEST];
	    String devName;

	    switch (device)
	    {
	    case DEV_MASTER:
	       devName="MASTER";
	       break;
	    case DEV_PDA:
	        devName="PDA";
	        break;
	    case DEV_AQUAPURE:
	        devName="AQUAPURE";
	        break;
	    case DEV_LXHEATER:
	        devName="LXHEATER";
	        break;
	    case DEV_PCDOCK:
	        devName="PCDOCK";
	        break;
	    case DEV_LXi_LZRE:
	        devName="LXi_LZRE";
	        break;
	    case DEV_AQUALINK:
	        devName="AQUALINK";
	        break;
	    case (byte) DEV_CHEMLINK:
	        devName="CHEMLINK";
	        break;
	    default:
	       if (device==deviceId)
	          devName = "THIS";
	       else
	          devName="UNKNOWN DEVICE:" + String.format("%02x ", packet[PKT_DEST]);     
	    }
	    sb.append(devName + " ");	
	}

    
	if (packet.length > PKT_CMD)
	{
	    byte cmd = packet[PKT_CMD];
	    sb.append(" ");
	    if (cmd<COMMANDS.values().length && cmd>=0)
	    	sb.append(COMMANDS.values()[cmd].name());
	    else
	    	sb.append(String.format("CMD:%02x ", cmd));
	    sb.append(" ");
	    if (packet.length > PKT_DATA)
	    {
		    if (cmd==COMMANDS.CMD_MSG.ordinal() || cmd==COMMANDS.CMD_MSG_LONG.ordinal())
		    {
		    	sb.append((int)packet[PKT_DATA]+ " \"" +getMessage(packet)+"\"");
		    }
		}

        logger.logInfo(sb.toString());
        dumpPacket(packet);
//	    print(" CHECKSUM:" );
//	    for (int i=packet.length-3; i < packet.length; i++)
//	          print(String.format("%02x ", packet[i]));
        if (packet.length > (PKT_DATA+2))
        {
    	    if (cmd==COMMANDS.CMD_ACK.ordinal() && packet[PKT_DATA+2]<KeyManager.COMMAND_KEYS.values().length && packet[PKT_DATA+2]>0)
    	    {
    	       logger.logInfo("--"+ KeyManager.COMMAND_KEYS.values()[packet[PKT_DATA+2]].name());
    	    }		
        }
	}
}

private static String getMessage(byte[] packet)
{
   StringBuffer sb = new StringBuffer();
   for (int i=PKT_DATA+1; i < packet.length-3 && packet[i]!=0; i++)
   {
      char c = (char) packet[i];
      if (c>=32 && c<126 ) sb.append(c);   
//      else sb.append("\\"+(byte)c);
   }
   return sb.toString();
}

void processPacket(byte[] packet) throws IOException
{
   byte cmd = 0;
   byte lineNum=0;
   
   if (packet.length>PKT_CMD)
      cmd=packet[PKT_CMD];
   if (packet.length>PKT_DATA)
      lineNum=packet[PKT_DATA];
   
   
   if(!checkPacket(packet)) //bad packet
   {
//	  startSleep();
	  return;
//      if (cmd!=(byte)COMMANDS.CMD_CURRENTSCREEN.ordinal() && cmd!=(byte)COMMANDS.CMD_PDASTATUS.ordinal() && !(lineNum==ScreenManager.MSGTYPE_TEMPERATURE && cmd==COMMANDS.CMD_MSG_LONG.ordinal()))
//      {
//         logger.logInfo("***NO ACKNOWLEDGE BAD PACKET");
//         logger.logInfo("");
//         return;
//     }
//     else
//     {
//         logger.logInfo("***BAD PACKET ACCEPTED");
//     }
   }
   
   if(Arrays.equals(packet,lastPacket)) {
      // Don't process redundant packets. They can occur for two reasons.
      // First, status doesn't change much so the vast majority of packets
      // are identical under normal circumstances. It is more efficient to
      // process only changes. Second, the master will send redundant packets
      // if it misses an ACK response up to 3 times before it sends a
      // command probe. Redundant message packets can corrupt long message
      // processing.
      // Log the redundant packets other than STATUS packets at DEBUG level.
//      if(packet[PKT_CMD] != CMD_STATUS && logLevel == LOGLEVEL.DEBUG) {
//         logger.logInfo("Trapped redundant packet...");
//         logPacket(packet);
//      }
      // Return without processing the packet.
//COMMENTED OUT FOR NOW      return;
   }


//   logPacket(packet);
   // Process by packet type.
   if (packet[PKT_CMD] == COMMANDS.CMD_PDASTATUS.ordinal())
   {
//      logger.logInfo("PDA STATUS:"+packet[PKT_DATA]);
//      if (packet[PKT_DATA] == 1)
//      {
//         this.pdaStatus=0x10;         
//         logger.logInfo("PDA STATUS READY");
//      }
   }   
   else if (cmd==COMMANDS.CMD_CURRENTSCREEN.ordinal())
   {
      screenManager.setCurrentScreen(SCREENS.SCREEN_PENDING);
      if (screenManager.getMiscStatus().length()>0) this.miscStatusLast=screenManager.getMiscStatus();
      if (screenManager.getLastScreen() == SCREENS.SCREEN_EQUIPMENTSTATUS) this.aquapureErrorLast=screenManager.getAquaPureError();
      screenManager.dumpStatus();
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_FIELDFOCUS.ordinal()) 
   {
	   keyManager.processFieldFocus(packet);
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_SCROLLSCREEN.ordinal()) 
   {
	   //10 02 60 0f 01 08 01 8b 10 03 PKT_DATA top line, +1 bottom line, +2 direction
	   screenManager.scrollScreen(packet[PKT_DATA], packet[PKT_DATA+1], packet[PKT_DATA+2]);
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_STATUS.ordinal()) 
   {
     if (packet[PKT_DEST]==this.deviceId)
    	 screenManager.setPdaReady(true);
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_MSG.ordinal()) {
    // Packet is a single line message.
//      logger.logInfo("MESSAGE: " + getMessage(packet));
   }   
   else if(packet[PKT_CMD] == COMMANDS.CMD_SELECTEDLINE.ordinal()) {
	   keyManager.processSelectedLine(packet);
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_MSG_LONG.ordinal()) {
//      logger.logInfo("LONG MESSAGE: " + getMessage(packet));
      lineNum=packet[PKT_DATA];
      String msg=getMessage(packet);
      keyManager.clearKeyPress(); //TODO we need to do this for equip on/off menu or else we will get retries!
      screenManager.processMsgLine(lineNum,msg);
   }
   else if(packet[PKT_CMD] == COMMANDS.CMD_PROBE.ordinal()) {
      if (packet[PKT_DEST]==this.deviceId)
          screenManager.setPdaReady(false);
      // Packet is a command probe. The master is trying to find
      // this device.
   }
}


public static long now()
{
   return (new Date()).getTime();
}


//Send an ack packet to the Aqualink RS8 master device.
//file_descriptor: the file descriptor of the serial port connected to the device
//command: the command byte to send to the master device, NUL if no command
void sendAcknowledge(byte[] packetIn) throws IOException
{
 byte packet[];
 
 //2018/03/30 we need to always send PdaReady?
 if (screenManager.isPdaReady())
 {
    packet=this.ackPacketLong;
    packet[PKT_KEY]=0x0;
//TODO need a way to retry a single key press vs. just time it out
    // Update the packet and checksum if command argument is not NUL.
//    if(sendKeyPress != null && packetIn[PKT_CMD]==COMMANDS.CMD_STATUS.ordinal() && (!keyManager.isKeyPressPending() || now()-whenKeySent > KEYRESENDTIME*1000)) {
    if(keyManager.getPendingKeyPress() != null && packetIn[PKT_CMD]==COMMANDS.CMD_STATUS.ordinal())
    {
        logger.logInfo(String.format("Pending Key Press: %s Retry Counter %d Key Pending %s %d Sec" , keyManager.getPendingKeyPress().name(), keyPressRetryCount, keyManager.isKeyPressPending(), (now()-keyManager.getWhenKeySent())/1000));
    	if (!keyManager.isKeyPressPending() || (now()-keyManager.getWhenKeySent() > KEYRESENDTIME*1000)) 
    	{
    	   logger.logInfo(String.format("ACKing with Key Press: %s" , keyManager.getPendingKeyPress().name()));
	       packet[PKT_KEY] = (byte)keyManager.getPendingKeyPress().ordinal();
	       if (!keyManager.isAllowRetry())
	       {
	           //all off can't allow retry if we get nothing to clear the pending
        	   keyManager.clearKeyPress();
	           logger.logInfo("No Retry Resetting Key Press");
	       } else {
	           if (!keyManager.isKeyPressPending()) //we didnt get here because of a retry wait timeout so init retry count
	           {
	        	   //init retry count for first key send
	        	   this.keyPressRetryCount=5;
	               logger.logInfo("Initializing Retry Counter");
	           }
	
	           this.keyPressRetryCount--; 
	           
	           if (keyPressRetryCount<=0) //we are done retrying, go to sleep
	           {
	        	   //start all over if we can't get key accepted
//	        	   startSleep();
	        	   keyManager.clearKeyPress();
	           }
	       }
	       keyManager.setKeyPressPending(true);
	       keyManager.setWhenKeySent(now());
	    }  
    }
 }
 else
    packet=this.ackPacket;

 packet[packet.length-PKT_CHKSUM_FROMEND] = generateChecksum(packet);
 logPacket(packet);
 serialPort1.writeBytes(packet, packet.length);
// outputStream.write(packet);
// outputStream.flush();  
}

//Kludgy as we are going to impersonate the master to send set level to get a response back
//TODO we can use this if we want to disconnect the aquapure from the aqualink and we can be the direct controller
//If I thought of this prior, I wouldn't have needed the new board!!
public void sendSetAquapureLevel(byte level) throws IOException
{
	setAquaPureLevelPacket[PKT_DATA]=level; 
	setAquaPureLevelPacket[setAquaPureLevelPacket.length-PKT_CHKSUM_FROMEND] = generateChecksum(setAquaPureLevelPacket);
	 serialPort1.writeBytes(setAquaPureLevelPacket, setAquaPureLevelPacket.length);
//	outputStream.write(setAquaPureLevelPacket);
//	outputStream.flush();
	logger.logInfo("****INJECTED PACKET****");
	logPacket(setAquaPureLevelPacket);
	lastDevice=DEV_AQUAPURE; //watch for the response
}

//Kludgy as we are going to impersonate the master to send set level to get a response back
//TODO we can use this if we want to disconnect the aquapure from the aqualink and we can be the direct controller
//If I thought of this prior, I wouldn't have needed the new board!!
public void sendGetAquapureStatus() throws IOException
{
	getAquaPureStatusPacket[PKT_DATA]=50; //50%
	getAquaPureStatusPacket[getAquaPureStatusPacket.length-PKT_CHKSUM_FROMEND] = generateChecksum(getAquaPureStatusPacket);
	 serialPort1.writeBytes(getAquaPureStatusPacket, getAquaPureStatusPacket.length);
//	outputStream.write(getAquaPureStatusPacket);
//	outputStream.flush();
	logger.logInfo("****INJECTED PACKET****");
	logPacket(getAquaPureStatusPacket);
	lastDevice=DEV_AQUAPURE; //watch for the response
}

//Generate and return checksum of packet.
static byte generateChecksum(byte[] packet)
{
   int sum=0;
   //172 vs 128
   for (int i = 0; i < packet.length - 3; i++)
   {
      int value = packet[i];
      if (value < 0) 
         value += 256; 
      sum += value;
   }    
   return(byte)(sum & 0xff);
}


   @Override
   public void run() {

      while (!done) {
         readData();
      }
   }	
     
   private boolean checkPacket(byte[] packet)
   {
      boolean ok=true;
      if (packet.length<PKT_DATA)
      {
         logger.logInfo("***ERROR Illegal Packet Length:" + packet.length);   
         ok=false;         
      }
      else
      {
         byte csCalculated = generateChecksum(packet);
         byte csReceived = packet[packet.length-PKT_CHKSUM_FROMEND];
         
         //coomands with whack checksums
         if(packet.length==6 && packet[PKT_CMD]==COMMANDS.CMD_FIELDFOCUS.ordinal() && csCalculated==0x72)
        	 return true;
//2019-04-14- 09:40:24 PDA  CMD_MSG_LONG 5 "DEGREES C/F    >"
//2019-04-14- 09:40:24 10 02 60 04 05 44 45 47 52 45 45 53 20 43 2f 46 20 20 20 20 3e 10 00 10 03 
         if(packet.length==25 && packet[PKT_CMD]==COMMANDS.CMD_MSG_LONG.ordinal() && csCalculated==0x20)
        	 return true;
         
         
         if (csReceived != csCalculated)
         {
            logger.logInfo(String.format("**ERROR Check Sum Error - Received %02x, Calculated %02x", csReceived, csCalculated )); 

            dumpPacket(packet);
            ok=false;
         }     
      }  
     return ok;
   }

	public void close() {
      try{
         
//         if (outputStream != null)
//            outputStream.close();
//         if (inputStream != null)
//            inputStream.close();
         if (serialPort1 != null)
            serialPort1.closePort();
      }
      catch (Exception e)
      {
         this.handleException(e);
      }
	}

	//If no activity for WHENTOSLEEP seconds, we sleep for, SYSTEMSLEEPTIME seconds
	public String getStatus() {
//	   if (this.isThisAwake() && now() - this.lastActivity > WHENTOSLEEP*1000) //15 seconds
//	      this.startSleep();
		if (status==null) return "";
		return status;
	}

	public String getStatusMessage() {
		if (statusMessage==null) return "";
		return statusMessage;
	}

	public String getPort() {
		return port;
	}
      
   public void setPort(String port) {
      this.port = port;
   }

   public void interrupt() {
     done=true;      
   }

   public boolean isAlive() {
      return true;
   }
   
   public static String getName()
   {
      return "AquaLinkManager";
   }
   
   //used to detect specific screens
   public class ScreenID
   {
      public int lineNum;
      public String textMarker;
      
   }

   public String getDateTime() {
      return screenManager.getDayTime();
   }

   public String getCurrentAirTemperature() {
      return screenManager.getCurrentAirTemperature();
   }

   public String getCurrentWaterTemperature() {
      return screenManager.getCurrentWaterTemperature();
   }

   public String getAquaPureError() {
      return aquapureErrorLast;
   }

   public String getAquaPureStatus() {
      return miscStatusLast;
   }
   public long getCurrentLine() {
	      return keyManager.selectedLine;
	   }

   public SCREENS getCurrentScreen() {
      return screenManager.getCurrentScreen();
   }

   public void setKeyPress(String key) {
      try {
            keyManager.setKeyPress(COMMAND_KEYS.valueOf(key));
      }
      catch (Exception e)
      {
         this.handleException(e);
      }
   }
   
   
   //*******************************************
   //*PUBLIC METHODS
   //*******************************************


   public boolean isKeyPressPending() {
      return keyManager.isKeyPressPending();
   }

   public boolean isPdaReady() {
      return screenManager.isPdaReady();
   }

   public void setLogging(boolean logging) {
	      Logger.setLogging(logging);
   }

   public void setDebugging(boolean logging) {
		debugging=logging;
		Logger.setLogging(logging);
   }

   public String getAquaPureCurrentStatus() {
      return screenManager.getMiscStatus();
   }

   public String getAquaPureCurrentError() {
      return screenManager.getAquaPureError();
   }
   
   public void allEquipmentOff()
   {
	   commandManager.setAllEquipmentOff();
   }
   
   public String getStatusSummary() {
      String statusSummary="";
      
      for (int i=0;i<screenManager.getEquipmentStatus().length;i++)
      {
         if(screenManager.getEquipmentStatus()[i]==EQUIPMENTSTATUS.ON)
            statusSummary+=EQUIPMENT.values()[i].toString() + " ";
       }
      return statusSummary;
   }

   public EQUIPMENTSTATUS getUnitStatus() {
      return unitStatus;
   }

   public void setUnitStatus(String statusString) {
      try {
         unitStatus = EQUIPMENTSTATUS.valueOf(statusString); 
         if (unitStatus==EQUIPMENTSTATUS.OFF)
        	 this.cancelCommands();
      }
      catch (Exception e)
      {
         this.handleException(e);
      }

   }


   public Integer getSpaHeaterSetPoint() {
      return screenManager.getSpaHeaterSetPoint();
   }

   public Integer getPoolHeaterSetPoint() {
	  return screenManager.getPoolHeaterSetPoint();
   }
   
   public Integer getSpaAquaPureSetPoint() {
	  return screenManager.getSpaAquaPureSetPoint();
   }

   public Integer getPoolAquaPureSetPoint() {
      return screenManager.getPoolAquaPureSetPoint();
   }
   
   public Integer getSpaHeaterPendingSetPoint() {
     return screenManager.getPendingSpaHeaterSetPoint();
   }

   public Integer getPoolHeaterPendingSetPoint() {
	  return screenManager.getPendingPoolHeaterSetPoint();
   }
   
   public Integer getSpaAquaPurePendingSetPoint() {
	  return screenManager.getPendingSpaAquaPureSetPoint();
   }

   public Integer getPoolAquaPurePendingSetPoint() {
      return screenManager.getPendingPoolAquaPureSetPoint();
   }

   public void setSpaHeaterSetPoint(int temperature) 
   {
	   commandManager.setSpaTemperature(temperature);
   }
   
   public void setPoolHeaterSetPoint(int temperature) 
   {
	   commandManager.setPoolTemperature(temperature);
   }

   public void setPoolAquaPureSetPoint(int temperature) 
   {
	   commandManager.setPoolAquaPure(temperature);
   }
   
   public void setSpaAquaPureSetPoint(int temperature) 
   {
	   commandManager.setSpaAquaPure(temperature);
   }
   public EQUIPMENTSTATUS[] getEquipmentStatus()
   {
      return screenManager.getEquipmentStatus();
   }

   public void cancelCommands()
   {
	   commandManager.cancelCommands();
       keyManager.clearKeyPress();
   }
   
   public void setEquipmentStatus(String equipmentString, String statusString) {
      try {
         EQUIPMENT equip = EQUIPMENT.valueOf(equipmentString);
         EQUIPMENTSTATUS status = EQUIPMENTSTATUS.valueOf(statusString);
         commandManager.setEquipmentStatus(equip,status);
         
 
      }
      catch (Exception e)
      {
         this.handleException(e);
      }
   }
   
  public void setDateTime(Date dateTime) 
   {	   
	  commandManager.setDateTime(dateTime);
   }
  public boolean isPDAAwake()
  {
//	  return false; //We now use same ID for THIS and PDA
     return (now() - this.lastPDAAck)/1000 < PDAWAITTIME;
  }
  public boolean isThisAwake()
  {
     return now() > thisSleepUntil;
  }
  public long getLastActivity() {
		return lastActivity;
	}

	public long getWhenKeySent() {
		return keyManager.getWhenKeySent();
	}
	public int getCommandQueueLength()
	{
		return commandManager.getCommandQueueLength();
	}
	public String getPendingCommand()
	{
		if (commandManager.getPendingCommand()==null)
			return "";
		return commandManager.getPendingCommand().toString();
	}
	public String getScreenDisplay()
	{
		return screenManager.getScreenDisplay();
	}
	public String getScreenDisplayHtml()
	{
		return screenManager.getScreenDisplayHtml();
	}
	public boolean isAquaPureAvailable()
	{
		return screenManager.isAquaPureAvailable();
	}
}
