package beans.events;

public class ReactionAdded extends Event {
    private String userId;
    private String itemTs;
    private String reaction;
    private String ts;

    public ReactionAdded(String userId, String itemTs, String reaction, String ts) {
        super();
        this.userId = userId;
        this.itemTs = itemTs;
        this.reaction = reaction;
        this.ts = ts;
    }

    // GETTERS
    public String getUserId() {
        return userId;
    }

    public String getItemTs() {
        return itemTs;
    }

    public String getReaction() {
        return reaction;
    }

    public String getTs() {
        return ts;
    }

    // SETTERS
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setItemTs(String itemTs) {
        this.itemTs = itemTs;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    //  METHODES
    @Override
    public String toString() {
        return "ReactionAdded{" +
                "userId='" + userId + '\'' +
                ", itemTs='" + itemTs + '\'' +
                ", reaction='" + reaction + '\'' +
                ", ts='" + ts + '\'' +
                '}';
    }
}
