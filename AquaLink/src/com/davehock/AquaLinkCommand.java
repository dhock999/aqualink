package com.davehock;

import com.davehock.AquaLinkManager.EQUIPMENT;
import com.davehock.AquaLinkManager.EQUIPMENTSTATUS;

//TODO use proper OO principals and create class for each command type. Encapsulate Execute in each command type. Maybe have abstract classes for temperatur, aquapure...so we can share more code
public class AquaLinkCommand {
	private COMMAND_TYPE type;
	private Integer parameter;
	private EQUIPMENTSTATUS status;
	private EQUIPMENT equipment;
	public enum COMMAND_TYPE{
		COMMAND_EQUIPMENT,
		COMMAND_SPAAQUAPURE,
		COMMAND_POOLAQUAPURE,
		COMMAND_SPASETPOINT,
		COMMAND_POOLSETPOINT,
		COMMAND_TIME,
		COMMAND_ALLOFF
	}	
	public AquaLinkCommand(COMMAND_TYPE type)
	{
		this.type=type;
	}
	public AquaLinkCommand(COMMAND_TYPE type, EQUIPMENT equipment, EQUIPMENTSTATUS status)
	{
		this.type=type;
		this.equipment=equipment;
		this.status=status;
	}
	public AquaLinkCommand(COMMAND_TYPE type, Integer parameter)
	{
		this.type=type;
		this.parameter = parameter;
	}
	
	public COMMAND_TYPE getType()
	{
		return type;
	}
	public Integer getParameter()
	{
		return parameter;
	}
	public EQUIPMENT getEquipment() {
		return equipment;
	}
	public EQUIPMENTSTATUS getStatus() {
		return status;
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer(type.name());

		if (equipment!=null) sb.append(" " + equipment.name());
		if (status!=null) sb.append(" " + status.name());
		if (parameter!=null) sb.append(" " + parameter.toString());
		
		return sb.toString();
	}
	public boolean isRedundant(AquaLinkCommand other)
	{
		//we only care if it is the same command but param can be different.
		if (other==null) return false;
		if (this.type!=other.type) return false;
		if (this.type==COMMAND_TYPE.COMMAND_EQUIPMENT && this.equipment!=other.equipment) return false;
		return true;
	}
}