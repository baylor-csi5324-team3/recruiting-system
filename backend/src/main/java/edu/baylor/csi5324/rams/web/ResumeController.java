package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Resume;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ResumeRepository;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Applicant resume selection and upload for Submit Job Application. */
@RestController
@RequestMapping("/api/applicants")
public class ResumeController {
    private final ApplicantRepository applicants;
    private final ResumeRepository resumes;

    public ResumeController(ApplicantRepository applicants, ResumeRepository resumes) {
        this.applicants = applicants;
        this.resumes = resumes;
    }

    public record ResumeInfo(Long id, String filename) {}
    public record ApplicantInfo(Long id, String name) {}

    /** Demo-only applicant selector; replace the supplied ID with authenticated identity. */
    @GetMapping
    public List<ApplicantInfo> listApplicants() {
        return applicants.findAll().stream()
                .map(a -> new ApplicantInfo(a.getId(), a.getName())).toList();
    }

    @GetMapping("/{id}/resumes")
    public List<ResumeInfo> listResumes(@PathVariable Long id) {
        if (!applicants.existsById(id)) throw new NotFoundException("Applicant not found.");
        return resumes.findByApplicantIdOrderByIdDesc(id).stream()
                .map(r -> new ResumeInfo(r.getId(), r.getFilename())).toList();
    }

    /** Step 3: upload required application materials before confirmation. */
    @PostMapping(value = "/{id}/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeInfo> upload(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        Applicant applicant = applicants.findById(id)
                .orElseThrow(() -> new NotFoundException("Applicant not found."));
        String filename = file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !(lower.endsWith(".pdf") || lower.endsWith(".docx"))) {
            throw new ValidationException("Upload a nonempty PDF or DOCX resume (maximum 5 MB).");
        }
        Resume saved = resumes.save(new Resume(applicant, filename, file.getBytes()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResumeInfo(saved.getId(), saved.getFilename()));
    }
}
