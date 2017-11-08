package beans;

import com.sun.istack.internal.Nullable;

public class Attachment {
	private String pretext;
	private String text;
	private String fallback;
	private String color;
	private String footer;

	public Attachment(@Nullable String pretext, String text, String fallback, String color, @Nullable String footer) {
		this.pretext = pretext;
		this.text = text;
		this.fallback = fallback;
		this.color = color;
		this.footer = footer;
	}

	//  GETTERS
	public String getPretext() {
		return pretext;
	}

	public String getText() {
		return text;
	}

	public String getFallback() {
		return fallback;
	}

	public String getColor() {
		return color;
	}

	public String getFooter() {
		return footer;
	}

	//  SETTERS
	public void setPretext(String pretext) {
		this.pretext = pretext;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setFallback(String fallback) {
		this.fallback = fallback;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	//  METHODES
	@Override
	public String toString() {
		return "Attachment{" +
				"pretext='" + pretext + '\'' +
				", text='" + text + '\'' +
				", fallback='" + fallback + '\'' +
				", color='" + color + '\'' +
				", footer='" + footer + '\'' +
				'}';
	}

	public String json() {
		return "{" +
				"\"pretext\" : \"" + pretext + "\", " +
				"\"text\" : \"" + text + "\", " +
				"\"fallback\" : \"" + fallback + "\", " +
				"\"color\" : \"" + color + "\", " +
				"\"footer\" : \"" + footer + "\" " +
				"}";
	}
}
