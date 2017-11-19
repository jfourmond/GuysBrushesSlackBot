package beans;

public class Member {
	private String id;
	private String name;
	private String realName;
	
	public Member(String id, String name, String realName) {
		this.id = id;
		this.name = name;
		this.realName = realName;
	}
	
	//  GETTERS
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getRealName() {
		return realName;
	}
	
	//  SETTERS
	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setRealName(String realName) {
		this.realName = realName;
	}
	
	//  METHODES
	
	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", realName='" + realName + '\'' +
				'}';
	}
}
