package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.*;

/** Applicant-owned resume; the submitted application stores its own immutable copy. */
@Entity
@Table(name = "applicant_resumes")
public class Resume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;
    @Column(nullable = false)
    private String filename;
    @Lob @Column(nullable = false)
    private byte[] content;

    protected Resume() {}
    public Resume(Applicant applicant, String filename, byte[] content) {
        this.applicant = applicant;
        this.filename = filename;
        this.content = content.clone();
    }
    public Long getId() { return id; }
    public Applicant getApplicant() { return applicant; }
    public String getFilename() { return filename; }
    public byte[] getContent() { return content.clone(); }
}
