package beans;

import java.util.List;

public class Channel {
    private String id;
    private String name;
    private int numMembers;
    private List<String> members;

    public Channel(String id, String name, int numMembers, List<String> members) {
        this.id = id;
        this.name = name;
        this.numMembers = numMembers;
        this.members = members;
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

    //  METHODES
    @Override
    public String toString() {
        return "Channel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", numMembers=" + numMembers +
                ", members=" + members +
                '}';
    }
}
