package edu.baylor.csi5324.rams.web;

/** Error payload: says what went wrong and what has to be corrected (NFR 3.2.1). */
public record ApiError(String message) {
}
