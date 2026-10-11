package edu.baylor.csi5324.rams.web;

import java.util.List;

/** What the system asks the actor for at the start of a use case ("request search criteria", ...). */
public record Prompt(String message, List<String> fields) {
}
