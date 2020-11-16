<%@ page language="java" contentType="text/xml; charset=ISO-8859-1"
    pageEncoding="UTF-8" import="com.davehock.*,javax.comm.*" %><?xml version="1.0" encoding="UTF-8"?>
<%
AquaLinkManager al = (AquaLinkManager) this.getServletContext().getAttribute(AquaLinkManager.getName());
AquaLinkManager.EQUIPMENTSTATUS equipmentStatus[]  = al.getEquipmentStatus();
String key = request.getParameter("key");
String setEquipment = request.getParameter("equipment");
String setStatus = request.getParameter("status");
String setTime = request.getParameter("time");

if (key!=null)
{
   al.setKeyPress(key);
   
}
if (setStatus!=null)
{
   if (setEquipment!=null)
   		al.setEquipmentStatus(setEquipment, setStatus);
   else 
      al.setUnitStatus(setStatus);
}

if (request.getParameter("close")!=null)
{
   al.interrupt();
   al.close();
}
if (setTime!=null)
	al.setDateTime(new java.util.Date());
if (request.getParameter("init")!=null)
{
   al.init();
}
if (request.getParameter("cancel")!=null)
{
   al.cancelCommands();
}   
if (request.getParameter("logging")!=null)
{
   al.setLogging(request.getParameter("logging").equals("true"));
}  
if (request.getParameter("debugging")!=null)
{
   al.setDebugging(request.getParameter("debugging").equals("true"));
}  
if (request.getParameter("allOff")!=null)
{
   al.allEquipmentOff();
} 
if (request.getParameter("setSpaTemp")!=null)
{
   al.setSpaHeaterSetPoint(Integer.parseInt(request.getParameter("setSpaTemp")));
}  
if (request.getParameter("setPoolTemp")!=null)
{
   al.setPoolHeaterSetPoint(Integer.parseInt(request.getParameter("setPoolTemp")));
} 
if (request.getParameter("setSpaAquaPure")!=null)
{
   al.setSpaAquaPureSetPoint(Integer.parseInt(request.getParameter("setSpaAquaPure")));
}  
if (request.getParameter("setPoolAquaPure")!=null)
{
   al.setPoolAquaPureSetPoint(Integer.parseInt(request.getParameter("setPoolAquaPure")));
} 
if (request.getParameter("sendGetAquaPureStatus")!=null)
{
   al.sendGetAquapureStatus();
} 
if (request.getParameter("sendSetAquaPureLevel")!=null)
{
   al.sendSetAquapureLevel(Byte.parseByte(request.getParameter("sendSetAquaPureLevel")));
} 

%>
<AquaLink>
     
    <AirTemp><%=al.getCurrentAirTemperature()%></AirTemp>
    <WaterTemp><%=al.getCurrentWaterTemperature()%></WaterTemp>
    <DayTime><%=al.getDateTime()%></DayTime>
    <AquaPure><%=al.getAquaPureStatus()%></AquaPure>
    <AquaPure2><%=al.getAquaPureCurrentStatus()%></AquaPure2> 
    <AquaPureError><%=al.getAquaPureError()%></AquaPureError>
    <AquaPureError2><%=al.getAquaPureCurrentError()%></AquaPureError2> 
<%
for(AquaLinkManager.EQUIPMENT equipment:AquaLinkManager.EQUIPMENT.values())
{
%>
<<%=equipment.name()%>><%=equipmentStatus[equipment.ordinal()]%></<%=equipment.name()%>>
<%
}
%>
    <SpaHeaterSetPoint><%=al.getSpaHeaterSetPoint()%></SpaHeaterSetPoint>
    <PoolHeaterSetPoint><%=al.getPoolHeaterSetPoint()%></PoolHeaterSetPoint>
    <SpaAquaPureSetPoint><%=al.getSpaAquaPureSetPoint()%></SpaAquaPureSetPoint>
    <PoolAquaPureSetPoint><%=al.getPoolAquaPureSetPoint()%></PoolAquaPureSetPoint>

    <AquaPureRunning><%=al.isAquaPureAvailable()%></AquaPureRunning>
    <Summary><%=al.getStatusSummary()%></Summary>
 
    <CurrentLine><%=al.getCurrentLine() %></CurrentLine>
    <CurrentScreen><%= al.getCurrentScreen().name()%></CurrentScreen>
    <PDAAwake><%=al.isPDAAwake()%></PDAAwake>
    <Awake><%=al.isThisAwake()%></Awake>
    <UnitStatus><%=al.getUnitStatus()%></UnitStatus>
    <Ready><%=al.isPdaReady()%></Ready>
    <StatusMessage><%=al.getStatusMessage() %></StatusMessage>    
    <Status><%=al.getStatus() %></Status>
    <KeyPressPending><%=al.isKeyPressPending()%></KeyPressPending>
    <QueueLength><%=al.getCommandQueueLength()%></QueueLength>
    <PendingCmd><%=al.getPendingCommand()%></PendingCmd>
    <LastKeySend><%=al.getWhenKeySent()/1000%></LastKeySend>
    <LastActivity><%=al.getLastActivity()/1000%></LastActivity>
    <AquaPureResponse><%=al.getAquaPureResponse()%></AquaPureResponse>
    
</AquaLink>
