import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { toast } from 'sonner';
import type { Application, Job, Role, Stage } from './data';
import { api, bodyJson, convertApplication, convertJob, toBackendStage, toBackendType,
  type Actor, type AccountRow, type BackendApplication, type BackendJob, type UserProfile } from '@/lib/recruiting-api';

const blankProfile: UserProfile = { name: '', email: '', phone: '', location: '', headline: '', bio: '', resume: '' };

/** The account returned by User Login (POST /api/accounts/login and GET /api/accounts/me). */
type SignedInUser = { id: number; email: string; role: 'APPLICANT' | 'RECRUITER' | 'ADMINISTRATOR'; name: string };
/** The profile returned by Update Applicant Profile (GET and PUT /api/profile). */
type BackendProfile = { applicantId: number; name: string; email: string; phone: string | null; location: string | null; headline: string | null; bio: string | null };
const toActor = (u: SignedInUser): Actor => ({ id: u.id, name: u.name, email: u.email, company: null,
  role: u.role === 'RECRUITER' ? 'Recruiter' : u.role === 'ADMINISTRATOR' ? 'Administrator' : 'Applicant' });

function useRecruitingState() {
  const [role, setRole] = useState<Role>('Guest');
  const [actors, setActors] = useState<Actor[]>([]);
  const [signedIn, setSignedIn] = useState<Actor | null>(null);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [accounts, setAccounts] = useState<AccountRow[]>([]);
  const [saved, setSaved] = useState<number[]>([]);
  const [profile, setProfile] = useState<UserProfile>(blankProfile);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // Every page acts for the signed-in user; the read model adds details such as the recruiter's company.
  const actor = useMemo(() => signedIn ? actors.find(x => x.role === signedIn.role && x.id === signedIn.id) ?? signedIn : undefined, [actors, signedIn]);
  const applicantId = role === 'Applicant' ? actor?.id ?? null : null;
  const recruiterId = role === 'Recruiter' ? actor?.id ?? null : null;
  const administratorId = role === 'Administrator' ? actor?.id ?? null : null;
  // Audit history is not available from the current backend. Do not fabricate entries.
  const audit: string[] = [];
  const record = (_message: string) => {};

  // The list of users is only available to a signed-in user (it adds details such as the recruiter's company).
  useEffect(() => {
    if (!signedIn) { setActors([]); return; }
    let live = true;
    api<Actor[]>('/api/demo/actors').then(rows => { if (live) setActors(rows); }).catch(() => {});
    return () => { live = false; };
  }, [signedIn?.id]);

  // Restore a sign-in that is still open on the server (for example after a page reload).
  useEffect(() => {
    fetch('/api/accounts/me').then(r => r.ok ? r.json() : null).then((user: SignedInUser | null) => {
      if (user) { const next = toActor(user); setSignedIn(next); setRole(next.role); }
    }).catch(() => {});
  }, []);

  /** User Login: the server checks the credentials and keeps the user in its session; errors carry its message. */
  const signIn = async (email: string, password: string) => {
    const next = toActor(await api<SignedInUser>('/api/accounts/login', { method: 'POST', ...bodyJson({ email: email.trim(), password }) }));
    setSignedIn(next); setRole(next.role);
    return next;
  };
  const signOut = async () => {
    await fetch('/api/accounts/logout', { method: 'POST' }).catch(() => {});
    setSignedIn(null); setRole('Guest');
  };
  const loadProfile = async (id: number): Promise<UserProfile> => {
    const [p, resumes] = await Promise.all([
      api<BackendProfile>('/api/profile', { headers: { 'X-Applicant-Id': String(id) } }),
      api<{ id: number; filename: string }[]>(`/api/applicants/${id}/resumes`)]);
    return { name: p.name ?? '', email: p.email ?? '', phone: p.phone ?? '', location: p.location ?? '',
      headline: p.headline ?? '', bio: p.bio ?? '', resume: resumes[0]?.filename ?? '' };
  };
  const refresh = async () => {
    setLoading(true); setError('');
    try {
      const qs = role === 'Recruiter' && actor ? `?recruiterId=${actor.id}` : '';
      const jobsRequest = api<BackendJob[]>('/api/demo/jobs' + qs);
      const appsRequest = role === 'Applicant' && actor ? api<BackendApplication[]>(`/api/demo/applications?applicantId=${actor.id}`)
        : role === 'Recruiter' && actor ? api<BackendApplication[]>(`/api/demo/applications?recruiterId=${actor.id}`) : Promise.resolve([]);
      const profileRequest = applicantId !== null ? loadProfile(applicantId) : Promise.resolve(blankProfile);
      const savedRequest = applicantId !== null ? api<number[]>(`/api/demo/saved/${applicantId}`) : Promise.resolve([]);
      const accountsRequest = role === 'Administrator' && actor ? api<AccountRow[]>(`/api/demo/accounts?administratorId=${actor.id}`) : Promise.resolve([]);
      const [bj,ba,p,sv,ac] = await Promise.all([jobsRequest,appsRequest,profileRequest,savedRequest,accountsRequest]);
      setJobs(bj.map(convertJob)); setApplications(ba.map(convertApplication));
      setProfile(p); setSaved(sv); setAccounts(ac);
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Failed to load backend data';
      setError(msg); toast.error(msg);
      setJobs([]); setApplications([]); setAccounts([]); setSaved([]); setProfile(blankProfile);
    } finally { setLoading(false); }
  };
  useEffect(() => { void refresh(); }, [actors, role, signedIn]);
  async function after<T>(action: () => Promise<T>, message: string): Promise<T> {
    const result = await action();
    await refresh();
    toast.success(message);
    return result;
  }
  const toggleSave = async (id: number) => {
    if (role !== 'Applicant' || applicantId === null) { toast.error('Choose an applicant workspace'); return; }
    try { await after(() => api<number[]>(`/api/demo/saved/${applicantId}`, {
      method: 'PUT', ...bodyJson(saved.includes(id) ? saved.filter(x=>x!==id) : [...saved,id])
    }), 'Saved jobs updated'); } catch (e) { toast.error(String(e)); }
  };
  const submit = async (job: Job, file: File | null, cover: string): Promise<boolean> => {
    if (!actor || role !== 'Applicant' || !file) { toast.error('Select a resume and applicant'); return false; }
    if (file.size === 0 || file.size > 5 * 1024 * 1024 || !/\.(pdf|docx)$/i.test(file.name)) {
      toast.error('Choose a nonempty PDF or DOCX under 5 MB'); return false;
    }
    try {
      const formData = new FormData(); formData.append('file', file);
      const resume = await api<{id:number}>(`/api/applicants/${actor.id}/resumes`, { method:'POST', body:formData });
      await after(() => api(`/api/applications`, { method:'POST', ...bodyJson({
        applicantId: actor.id, jobPostingId: job.id, resumeId: resume.id, coverLetter: cover
      }) }), 'Application submitted');
      return true;
    } catch (e) { toast.error(`Submission failed: ${String(e)}`); return false; }
  };
  const updateStage = async (app: Application, stage: Stage): Promise<boolean> => {
    try {
      if (role === "Applicant" && actor) {
        // Withdraw is a separate use case.
        if (stage !== "Withdrawn") {
          throw new Error("Applicants cannot update application stages.");
        }

        await after(
          () =>
            api(`/api/applications/${app.id}/withdraw`, {
              method: "POST",
              headers: {
                "X-Applicant-Id": String(actor.id),
              },
            }),
          "Application withdrawn",
        );
      } else if (role === "Recruiter" && actor) {
        await after(
          () =>
            api(`/api/applications/${app.id}/stage`, {
              method: "PATCH",
              ...bodyJson({ stage: toBackendStage[stage] }, { "X-Recruiter-Id": String(actor.id) }),
            }),
          "Application stage updated",
        );
      } else {
        throw new Error("Select an appropriate workspace");
      }

      return true;
    } catch (error) {
      toast.error(String(error));
      return false;
    }
  };
  const saveJob = async (job: Job) => {
    if (role !== 'Recruiter' || !actor) { toast.error('Choose a recruiter workspace'); return false; }
    try {
      const details = { title:job.title, description:job.description, location:job.location,
        employmentType:toBackendType(job.type), closingDate:job.closes };
      const header = {'X-Recruiter-Id':String(actor.id)};
      const existing = jobs.find(x => x.id === job.id);
      let postingId = job.id;
      if (existing) {
        await api(`/api/postings/${job.id}`, { method:'PUT', ...bodyJson(details,header) });
      } else {
        const created = await api<{id:number}>('/api/postings', { method:'POST', ...bodyJson(details,header) });
        postingId = created.id;
      }
      if (job.status === 'Open' && (!existing || existing.status === 'Draft')) {
        await api(`/api/postings/${postingId}/publish`, { method:'POST', headers:header });
      }
      await refresh(); toast.success('Job posting saved on server'); return true;
    } catch (e) { toast.error(String(e)); return false; }
  };
  const closeJob = async (job: Job) => {
    if (!actor || role !== 'Recruiter') return false;
    try { await after(() => api(`/api/postings/${job.id}/close`, { method:'POST', headers:{'X-Recruiter-Id':String(actor.id)} }), 'Posting closed'); return true; }
    catch (e) { toast.error(String(e)); return false; }
  };
  const updateAccount = async (accountId: number, change: { active?:boolean; role?:Role; companyName?:string }) => {
    if (role !== 'Administrator' || !actor) return false;
    try {
      await after(() => api(`/api/accounts/${accountId}/role-status`, { method:'PUT',
        ...bodyJson({newRole:change.role?.toUpperCase(), newStatus:change.active===undefined?undefined:(change.active?'ACTIVE':'DEACTIVATED'),
          companyName:change.role==='Recruiter'?change.companyName?.trim():undefined}, {'X-Administrator-Id':String(actor.id)})
      }), 'Account updated'); return true;
    } catch (e) { toast.error(String(e)); return false; }
  };
  const uploadProfileResume = async (file: File) => {
    if (role !== 'Applicant' || !actor) return false;
    if (file.size===0 || file.size>5*1024*1024 || !/\.(pdf|docx)$/i.test(file.name)) { toast.error('PDF or DOCX, maximum 5 MB'); return false; }
    try {
      const data=new FormData();data.append('file',file);
      await after(()=>api(`/api/applicants/${actor.id}/resumes`,{method:'POST',body:data}),'Resume uploaded');
      return true;
    } catch(e){toast.error(String(e));return false;}
  };
  const saveProfile = async (draft: UserProfile) => {
    if (applicantId === null || role !== 'Applicant') return false;
    try {
      const changes = { name: draft.name, phone: draft.phone, location: draft.location, headline: draft.headline, bio: draft.bio };
      const updated = await after(() => api<BackendProfile>('/api/profile', { method:'PUT',
        ...bodyJson(changes, { 'X-Applicant-Id': String(applicantId) }) }), 'Profile saved');
      setSignedIn(prev => prev && { ...prev, name: updated.name });
      setActors(prev=>prev.map(a=>a.role==='Applicant' && a.id===applicantId ? {...a,name:updated.name}:a));
      return true;
    } catch (e) { toast.error(String(e)); return false; }
  };
  return { role, setRole, actor, actors, applicantId, recruiterId, administratorId,
    signedIn, signIn, signOut, jobs, saved, toggleSave, applications, submit, updateStage,
    accounts, profile, setProfile, saveProfile, uploadProfileResume, saveJob, closeJob, updateAccount,
    audit, record, loading, error, refresh };
}
type RecruitingState = ReturnType<typeof useRecruitingState>;
const RecruitingContext = createContext<RecruitingState | null>(null);
export function RecruitingProvider({ children }: {children: ReactNode}) {
  const state = useRecruitingState();
  return <RecruitingContext.Provider value={state}>{children}</RecruitingContext.Provider>;
}
export function useRecruiting() { const c = useContext(RecruitingContext); if (!c) throw new Error('RecruitingProvider missing'); return c; }
