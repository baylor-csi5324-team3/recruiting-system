package edu.baylor.csi5324.rams.config;

/** Invalid development fixtures abort initialization rather than leaving a partial dataset. */
public class JobCsvException extends RuntimeException {
    public JobCsvException(String message) {
        super(message);
    }
}
