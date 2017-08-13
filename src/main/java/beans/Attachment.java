package beans;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Attachment {
    private String text;
    private String fallback;
    private String callbackId;
    private String color;
    private String attachmentType = "default";
    private List<ActionButton> actionsButton;

    public Attachment(String text, String fallback, String callbackId, String color, ActionButton ... actions) {
        actionsButton = new ArrayList<>();
        this.text = text;
        this.fallback = fallback;
        this.callbackId = callbackId;
        this.color = color;
        actionsButton.addAll(Arrays.asList(actions));
    }

    //  GETTERS
    public String getText() {
        return text;
    }

    public String getFallback() {
        return fallback;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public String getColor() {
        return color;
    }

    public List<ActionButton> getActionsButton() {
        return actionsButton;
    }

    //  SETTERS
    public void setText(String text) {
        this.text = text;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setActionsButton(List<ActionButton> actionsButton) {
        this.actionsButton = actionsButton;
    }

    //  METHODES
    @Override
    public String toString() {
        return "Attachment{" +
                "text='" + text + '\'' +
                ", fallback='" + fallback + '\'' +
                ", callbackId='" + callbackId + '\'' +
                ", color=" + color +
                ", attachmentType='" + attachmentType + '\'' +
                ", actionsButton=" + actionsButton +
                '}';
    }

    public String json() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\"text\" : \"").append(text).append("\",\n");
        sb.append("\"fallback\" : \"").append(fallback).append("\",\n");
        sb.append("\"callback_id\" : \"").append(callbackId).append("\",\n");
        sb.append("\"color\" : \"").append(color).append("\",\n");
        sb.append("\"attachment_type\" : \"").append(attachmentType).append("\",\n");
        sb.append("\"actions\" : [");
        Iterator<ActionButton> iterator = actionsButton.iterator();
        while(iterator.hasNext()) {
            sb.append(iterator.next().json());
            if(iterator.hasNext())
                sb.append(",");
        }
        sb.append("]\n}");
        return sb.toString();
    }
}
