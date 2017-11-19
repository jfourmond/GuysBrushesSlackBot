package beans;

public class ActionButton {
	private static final String type = "button";
	
	private String name;
	private String text;
	private String value;
	
	public ActionButton(String name, String text, String value) {
		this.name = name;
		this.text = text;
		this.value = value;
	}
	
	//  GETTERS
	public String getName() {
		return name;
	}
	
	public String getText() {
		return text;
	}
	
	public String getValue() {
		return value;
	}
	
	//  SETTERS
	public void setName(String name) {
		this.name = name;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	//  METHODES
	@Override
	public String toString() {
		return "ActionButton{" +
				"name='" + name + '\'' +
				", text='" + text + '\'' +
				", value='" + value + '\'' +
				'}';
	}
	
	public String json() {
		return "{\n" +
				"\t\"name\" : \"" + name + "\",\n" +
				"\t\"text\" : \"" + text + "\",\n" +
				"\t\"type\" : \"" + type + "\",\n" +
				"\t\"value\" : \"" + value + "\"\n" +
				"}\n";
	}
}
