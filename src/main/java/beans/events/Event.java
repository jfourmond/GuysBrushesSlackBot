package beans.events;

public abstract class Event {
    protected String channel;

    public Event(String channel) {
        this.channel = channel;
    }

    //  GETTERS
    public String getChannel() {
        return channel;
    }

    //  SETTERS
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
