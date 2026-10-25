import type { Application, Job, Stage, Role } from '@/features/recruiting/data';

export type Actor = { id: number; name: string; email: string; role: Role; company: string | null };
export type UserProfile = { name: string; email: string; phone: string; location: string; headline: string; bio: string; resume: string };
export type AccountRow = { id: number; name: string; email: string; role: Role; active: boolean };
export type BackendJob = { id: number; title: string; description: string; companyName: string; location: string; employmentType: string; closingDate: string; postedDate: string | null; status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' };
export type BackendApplication = { id: number; jobId: number; name: string; email: string; stage: string; submittedAt: string; resumeFilename: string | null; history: { stage: string; changedAt: string; changedByUserId: number | null }[] };

export async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, options);
  if (!res.ok) {
    let message = `HTTP ${res.status}: ${url}`;
    try {
      const value: unknown = await res.json();
      if (value && typeof value === 'object') {
        const e = value as Record<string, unknown>;
        message = String(e.message ?? e.error ?? message);
      }
    } catch { /* not a JSON API error */ }
    throw new Error(message);
  }
  return res.json() as Promise<T>;
}

export function bodyJson(data: unknown, headers?: Record<string, string>): RequestInit {
  return { headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(data) };
}

export const toBackendStage: Record<Stage, string> = {
  'Submitted': 'SUBMITTED', 'Under review': 'UNDER_REVIEW', 'Interview': 'INTERVIEW', 'Offer': 'OFFER',
  'Accepted': 'ACCEPTED', 'Declined': 'DECLINED', 'Rejected': 'REJECTED', 'Withdrawn': 'WITHDRAWN'
};
export const fromBackendStage = (s: string): Stage => ({
  SUBMITTED: 'Submitted', UNDER_REVIEW: 'Under review', INTERVIEW: 'Interview', OFFER: 'Offer',
  ACCEPTED: 'Accepted', DECLINED: 'Declined', REJECTED: 'Rejected', WITHDRAWN: 'Withdrawn'
} as Record<string, Stage>)[s] ?? 'Submitted';
export const toBackendType = (s: string) => ({ 'Full-time': 'FULL_TIME', 'Part-time': 'PART_TIME',
  'Internship': 'INTERNSHIP', 'Contract': 'CONTRACT' } as Record<string, string>)[s] ?? s;
export const fromBackendType = (s: string) => ({ FULL_TIME: 'Full-time', PART_TIME: 'Part-time',
  INTERNSHIP: 'Internship', CONTRACT: 'Contract' } as Record<string, string>)[s] ?? s;
export function convertJob(job: BackendJob): Job {
  const closed = job.status === 'CLOSED' || (job.status === 'PUBLISHED' && job.closingDate < new Date().toISOString().slice(0, 10));
  return {
    id: job.id, title: job.title, company: job.companyName,
    logo: job.companyName.trim().split(/\s+/).map(x => x[0]).slice(0,2).join('').toUpperCase(),
    color: 'green', location: job.location, type: fromBackendType(job.employmentType),
    mode: 'Not specified', salary: 'Not disclosed', category: 'Not specified', level: 'Not specified',
    posted: job.postedDate ? new Date(job.postedDate).toLocaleDateString('en-US') : 'Not published',
    closes: job.closingDate, status: job.status === 'DRAFT' ? 'Draft' : closed ? 'Closed' : 'Open',
    description: job.description
  };
}
export function convertApplication(row: BackendApplication): Application {
  return {
    id: row.id, jobId: row.jobId, name: row.name, email: row.email,
    stage: fromBackendStage(row.stage), date: new Date(row.submittedAt).toLocaleDateString('en-US', { month:'short', day:'numeric', year:'numeric' }),
    resume: row.resumeFilename ?? '',
    history: row.history.map(h => ({ stage: fromBackendStage(h.stage),
      date: new Date(h.changedAt).toLocaleString('en-US'), actor: h.changedByUserId ? `User #${h.changedByUserId}` : 'System' }))
  };
}
