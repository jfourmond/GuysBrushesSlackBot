package beans.channels;

import java.util.List;

public class Channel {
    private String id;
    private String name;
    private int numMembers;
    private List<String> members;

    private ChannelType type;

    public Channel(String id, String name, int numMembers, List<String> members) {
        this.id = id;
        this.name = name;
        this.numMembers = numMembers;
        this.members = members;

        // Le type est déterminé par la première lettre de l'ID
        switch (id.charAt(0)) {
            case 'C':
                type = ChannelType.PUBLIC;
                break;
            case 'D':
                type = ChannelType.DIRECT_MESSAGE;
                break;
            default:
                type = ChannelType.PRIVATE;
                break;
        }
    }

    //  GETTERS
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNumMembers() {
        return numMembers;
    }

    public List<String> getMembers() {
        return members;
    }

    public ChannelType getType() {
        return type;
    }

    //  SETTERS
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumMembers(int numMembers) {
        this.numMembers = numMembers;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    //  METHODES
    @Override
    public String toString() {
        return "Channel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", numMembers=" + numMembers +
                ", members=" + members +
                ", type=" + type +
                '}';
    }
}
