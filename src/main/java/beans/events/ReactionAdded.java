package beans.events;

public class ReactionAdded extends Event {
	private String userId;
	private String itemType;
	private String itemTs;
	private String reaction;
	private String ts;
	private String itemUser;
	
	public ReactionAdded(String userId, String itemType, String itemChannel, String itemTs, String reaction, String ts, String itemUser) {
		super(itemChannel);
		this.userId = userId;
		this.itemType = itemType;
		this.itemTs = itemTs;
		this.reaction = reaction;
		this.ts = ts;
		this.itemUser = itemUser;
	}
	
	// GETTERS
	public String getUserId() {
		return userId;
	}
	
	public String getReaction() {
		return reaction;
	}
	
	public String getTs() {
		return ts;
	}
	
	public String getItemUser() {
		return itemUser;
	}
	
	public String getItemTs() {
		return itemTs;
	}
	
	public String getItemType() {
		return itemType;
	}
	
	// SETTERS
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public void setReaction(String reaction) {
		this.reaction = reaction;
	}
	
	public void setTs(String ts) {
		this.ts = ts;
	}
	
	public void setItemUser(String itemUser) {
		this.itemUser = itemUser;
	}
	
	public void setItemTs(String itemTs) {
		this.itemTs = itemTs;
	}
	
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}
	
	//  METHODES
	@Override
	public String toString() {
		return "ReactionAdded{" +
				"userId='" + userId + '\'' +
				", itemType='" + itemType + '\'' +
				", itemTs='" + itemTs + '\'' +
				", reaction='" + reaction + '\'' +
				", ts='" + ts + '\'' +
				", itemUser='" + itemUser + '\'' +
				", channel='" + channel + '\'' +
				'}';
	}
}
