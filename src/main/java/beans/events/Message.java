package beans.events;

import beans.Reaction;

import java.util.List;

public class Message extends Event {
	private String user;
	private String text;
	private String subtype;
	private String timestamp;
	private List<Reaction> reactions;
	
	
	public Message(String user, String channel, String text, String subtype, String timestamp, List<Reaction> reactions) {
		super(channel);
		this.user = user;
		this.text = text;
		this.subtype = subtype;
		this.timestamp = timestamp;
		this.reactions = reactions;
	}
	
	//  GETTERS
	public String getUser() {
		return user;
	}
	
	public String getText() {
		return text;
	}
	
	public String getSubtype() {
		return subtype;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public List<Reaction> getReactions() {
		return reactions;
	}
	
	//  SETTERS
	public void setUser(String user) {
		this.user = user;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setReactions(List<Reaction> reactions) {
		this.reactions = reactions;
	}
	
	//  METHODES
	public int countReactions() {
		int count = 0;
		for(Reaction reaction : reactions)
			count += reaction.getCount();
		return count;
	}
	
	@Override
	public String toString() {
		return "Message{" +
				"user='" + user + '\'' +
				", text='" + text + '\'' +
				", subtype='" + subtype + '\'' +
				", timestamp='" + timestamp + '\'' +
				", reactions=" + reactions +
				", channel='" + channel + '\'' +
				'}';
	}
}
