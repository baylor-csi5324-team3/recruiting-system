import { createContext, useContext, useState, type ReactNode } from 'react';
import { toast } from 'sonner';
import { initialJobs, initialApplications, initialInterviews, initialAccounts, type Role, type Stage, type Job, type Application, type Interview } from './data';

function useRecruitingState() {
  const [role, setRole] = useState<Role>('Applicant');
  const [jobs, setJobs] = useState(initialJobs);
  const [saved, setSaved] = useState<number[]>([3, 4]);
  const [applications, setApplications] = useState(initialApplications);
  const [interviews, setInterviews] = useState(initialInterviews);
  const [accounts, setAccounts] = useState(initialAccounts);
  const [profile, setProfile] = useState({ name: 'Alex Morgan', email: 'alex.morgan@example.com', phone: '+1 (254) 555-0142', location: 'Waco, TX', headline: 'Product designer & creative problem solver', bio: 'Creating thoughtful digital experiences. Currently exploring opportunities in design and technology.', resume: 'Alex_Morgan_Resume.pdf' });
  const [audit, setAudit] = useState<string[]>([]);
  const record = (message: string) => setAudit(prev => [`${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} · ${role === 'Applicant' ? profile.name : role === 'Recruiter' ? 'Taylor Brooks' : 'Robin Parker'} · ${message}`, ...prev]);
  const toggleSave = (id: number) => { if (role !== 'Applicant') { toast.error('Sign in as an applicant to save jobs.'); return; } setSaved(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]); };
  const submit = (job: Job, resume: string) => {
    if (role !== 'Applicant' || job.status !== 'Open' || new Date(job.closes + 'T23:59:59') < new Date()) { toast.error('This job cannot accept an application.'); return false; }
    if (applications.some(a => a.name === profile.name && a.jobId === job.id)) { toast.error('You have already applied for this job.'); return false; }
    if (!profile.name || !profile.email || !resume) { toast.error('Complete your profile and select a resume.'); return false; }
    const date = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    setApplications(prev => [...prev, { id: Date.now(), jobId: job.id, name: profile.name, email: profile.email, stage: 'Submitted', date, resume, history: [{ stage: 'Submitted', date, actor: profile.name }] }]);
    record(`Submitted an application for ${job.title}`); toast.success('Application submitted successfully.'); return true;
  };
  const updateStage = (app: Application, stage: Stage) => {
    const job = jobs.find(j => j.id === app.jobId);
    const owner = role === 'Applicant' && app.name === profile.name;
    const recruiter = role === 'Recruiter' && job?.company === 'Linear';
    const allowed = owner ? (stage === 'Withdrawn' && ['Submitted', 'Under review', 'Interview'].includes(app.stage)) || (app.stage === 'Offer' && ['Accepted', 'Declined'].includes(stage)) : recruiter && ((app.stage === 'Submitted' && stage === 'Under review') || (['Under review', 'Interview'].includes(app.stage) && ['Offer', 'Rejected'].includes(stage)) || (app.stage === 'Under review' && stage === 'Interview'));
    if (!allowed) { toast.error('That stage change is not available.'); return; }
    setApplications(prev => prev.map(a => a.id === app.id ? { ...a, stage, history: [...a.history, { stage, date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }), actor: owner ? profile.name : 'Taylor Brooks' }] } : a));
    if (['Withdrawn', 'Rejected'].includes(stage)) setInterviews(prev => prev.map(i => i.applicationId === app.id ? { ...i, status: 'Cancelled' } : i));
    record(`${app.name}: changed application stage to ${stage}`); toast.success(`Application updated to ${stage.toLowerCase()}.`);
  };
  const saveJob = (job: Job) => { setJobs(prev => prev.some(j => j.id === job.id) ? prev.map(j => j.id === job.id ? job : j) : [job, ...prev]); record(`${job.status === 'Draft' ? 'Saved draft' : 'Updated posting'}: ${job.title}`); toast.success(job.status === 'Draft' ? 'Draft saved.' : 'Job posting published.'); };
  const schedule = (interview: Interview) => { setInterviews(prev => prev.some(i => i.id === interview.id) ? prev.map(i => i.id === interview.id ? interview : i) : [...prev, interview]); const app = applications.find(a => a.id === interview.applicationId); if (app?.stage === 'Under review') updateStage(app, 'Interview'); record('Saved interview schedule'); toast.success('Interview scheduled.'); };
  return { role, setRole, jobs, setJobs, saved, toggleSave, applications, submit, updateStage, interviews, setInterviews, schedule, accounts, setAccounts, profile, setProfile, saveJob, audit, record };
}
type RecruitingState = ReturnType<typeof useRecruitingState>;
const RecruitingContext = createContext<RecruitingState | null>(null);
export function RecruitingProvider({ children }: { children: ReactNode }) { const state = useRecruitingState(); return <RecruitingContext.Provider value={state}>{children}</RecruitingContext.Provider>; }
export function useRecruiting() { const state = useContext(RecruitingContext); if (!state) throw new Error('RecruitingProvider is required'); return state; }