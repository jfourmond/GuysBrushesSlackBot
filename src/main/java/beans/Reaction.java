package beans;

import java.util.List;

public class Reaction {
	private String name;
	private Integer count;
	private List<String> users;

	public Reaction(String name, Integer count, List<String> users) {
		this.name = name;
		this.count = count;
		this.users = users;
	}

	//  GETTERS
	public String getName() {
		return name;
	}

	public Integer getCount() {
		return count;
	}

	public List<String> getUsers() {
		return users;
	}

	//  SETTERS
	public void setName(String name) {
		this.name = name;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}

	//  METHODES
	public boolean addUser(String user) {
		return users.add(user);
	}

	public void incrementCount() {
		count++;
	}

	@Override
	public String toString() {
		return "Reaction{" +
				"name='" + name + '\'' +
				", count=" + count +
				", users=" + users +
				'}';
	}
}
