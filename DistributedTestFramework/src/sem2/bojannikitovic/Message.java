package sem2.bojannikitovic;

public class Message
{
	private int hashCode;
	
	private int resp;
	
	private Object value;
	
	private int sender;

	public Message(int hashCode, Object value, int sender)
	{
		this.hashCode = hashCode;
		this.value = value;
		this.sender = sender;
	}
	
	public int getHashCode()
	{
		return hashCode;
	}

	public void setHashCode(int hashCode)
	{
		this.hashCode = hashCode;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public int getSender()
	{
		return sender;
	}

	public void setSender(int sender)
	{
		this.sender = sender;
	}

	public int getResp()
	{
		return resp;
	}

	public void setResp(int resp)
	{
		this.resp = resp;
	}
}
