package edu.baylor.csi5324.rams.system;

/**
 * Response payload describing the running application.
 *
 * <p>A Java record: the compiler generates the constructor, accessors,
 * {@code equals}, {@code hashCode} and {@code toString}. Jackson serialises it
 * to JSON as {@code {"application": ..., "status": ...}}.
 */
public record SystemInfo(String application, String status) {
}
