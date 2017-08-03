package beans;

public class File {
    private String id;
    private long created;
    private String name;
    private String title;
    private String user;

    public File(String id, long created, String name, String title, String user) {
        this.id = id;
        this.created = created;
        this.name = name;
        this.title = title;
        this.user = user;
    }

    //  GETTERS
    public String getId() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getUser() {
        return user;
    }

    //  SETTERS
    public void setId(String id) {
        this.id = id;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUser(String user) {
        this.user = user;
    }

    //  METHODES

    @Override
    public String toString() {
        return "File{" +
                "id='" + id + '\'' +
                ", created=" + created +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
